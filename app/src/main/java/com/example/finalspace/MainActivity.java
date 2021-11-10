package com.example.finalspace;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DESCOVER_BT = 1;
    private Set<BluetoothDevice> pairedDevices;
    public ArrayList<BluetoothDevice> mDeviceList = new ArrayList<>();
    public static String EXTRA_ADDRESS = "device_address";
    ListView devicelist;
    EditText mEditText;
    Button mOnOffBtn, mPairedBtn, mCleanTextBtn, mListenBtn, mSendBtn;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice[] btArray;
    ListView listView;
    TextView mTextOutput, mTextStatus;
    int status = 0;

    private static final String APP_NAME = "FinalSpace";
    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    SendReceive sendReceive;

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSAGE_RECEIVED=5;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        findViewByIdes();
        implemetListeners();
    }


    private void CheckBluetoothState() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth not supported. Aborting...", Toast.LENGTH_LONG).show();
            return;
        }
        if (mBluetoothAdapter.isEnabled()) {
            //listen paired devices
            mBluetoothAdapter.disable();
            Toast.makeText(getApplicationContext(), "Turning Bluetooth Off...", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Making Your Device Discoverable", Toast.LENGTH_LONG).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void findViewByIdes(){
        mOnOffBtn = findViewById(R.id.onoffBtn);
        mPairedBtn = findViewById(R.id.pairedBtn);
        listView = (ListView) findViewById(R.id.listView1);
        mSendBtn = findViewById(R.id.sendBtn);
        mEditText = findViewById(R.id.editText);
        mTextOutput = findViewById(R.id.textOutput);
        mTextStatus = findViewById(R.id.textStatus);
        //mListenBtn = findViewById(R.id.listenBtn);
        mCleanTextBtn = findViewById(R.id.cleanTextBtn);
    }

    private void implemetListeners() {
        mOnOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBluetoothState();
            }
        });

        mPairedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<BluetoothDevice> bt=mBluetoothAdapter.getBondedDevices();
                String[] strings=new String[bt.size()];
                btArray=new BluetoothDevice[bt.size()];
                int index=0;

                if( bt.size()>0)
                {
                    for(BluetoothDevice device : bt)
                    {
                        btArray[index]= device;
                        strings[index]=device.getName();
                        index++;
                    }
                    ArrayAdapter<String> arrayAdapter=new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,strings);
                    listView.setAdapter(arrayAdapter);
                }
            }
        });

        /*mListenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ServerClass serverClass=new ServerClass();
                serverClass.start();
            }
        });*/

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ClientClass clientClass=new ClientClass(btArray[i]);
                clientClass.start();

                //status.setText("Connecting");
                Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_LONG).show();
            }
        });

        mSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String string = String.valueOf(mEditText.getText());
                sendReceive.write(string.getBytes());
                mEditText.setText("");
            }
        });

        mCleanTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTextOutput.setText("");
                mTextOutput.computeScroll();
            }
        });
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case STATE_LISTENING:
                    mTextOutput.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    mTextOutput.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    //mTextOutput.setText("Connected");
                    Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_LONG).show();
                    break;
                case STATE_CONNECTION_FAILED:
                    mTextOutput.setText("Connection Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    //byte[] readBuff= (byte[]) msg.obj;
                    //String tempMsg=new String(readBuff,0,msg.arg1);
                    String tempMsg = (msg.obj.toString());
                    mTextOutput.setMovementMethod(new ScrollingMovementMethod());
                    mTextOutput.setText(mTextOutput.getText().toString() + tempMsg);
                    break;
            }
            return false;
        }
    });

    private class ServerClass extends Thread{
        private BluetoothServerSocket serverSocket;

        public ServerClass(){
            try {
                serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            BluetoothSocket socket=null;
            while (socket==null){
                try {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket = serverSocket.accept();
                }catch(IOException e){
                    e.printStackTrace();
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if(socket!=null){
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendReceive = new SendReceive(socket);
                    sendReceive.start();

                    break;
                }
            }
        }
    }

    private class ClientClass extends Thread{

        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass(BluetoothDevice device1){
            device = device1;

            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            try {
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive = new SendReceive(socket);
                sendReceive.start();
            }catch (IOException e){
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    private class SendReceive extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive(BluetoothSocket socket){
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn=bluetoothSocket.getInputStream();
                tempOut=bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream=tempIn;
            outputStream=tempOut;
        }

        public void run(){


            while (true)
            {
                try {
                    byte[] buffer=new byte[1024];
                    String readMessage;
                    int bytes;
                    //bytes=inputStream.read(buffer);
                    if(inputStream.available() > 2){
                        try {
                            bytes=inputStream.read(buffer);
                            readMessage = new String(buffer, 0, bytes);
                        }catch (IOException e){
                            break;
                        }
                        handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,readMessage).sendToTarget();
                    }else{
                        SystemClock.sleep(100);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}

    /*private void pairedDeviceList() {
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bt : pairedDevices) {
                list.add(bt.getName().toString() + "\n" + bt.getAddress().toString());
            }
        } else {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(myListClickListener);
    }*/



    /*private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String info = ((TextView) view).getText().toString();
            //String address = info.substring(info.length()-17);


            Intent i = new Intent(MainActivity.this, ConnectedDevice.class);
            i.putExtra(EXTRA_ADDRESS, info);
            startActivity(i);
        }
    };/*

            mDiscBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBluetoothAdapter.isDiscovering()) {
                    Toast.makeText(getApplicationContext(), "Making Your Device Discoverable", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivityForResult(intent, REQUEST_DESCOVER_BT);
                }
            }
        });
     */


