package com.bozzadevs.estufainteligentebluetooth;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

    SeekBar seekBarServo, seekBarRED1, seekBarGREEN1, seekBarBLUE1, seekBarRED2, seekBarRED3, seekBarBLUE2, seekBarBLUE3;
    TextView TextCondicoesAtuais, TextServo, TextRED1, TextGREEN1, TextBLUE1, TextRED2, TextBLUE2,TextRED3, TextBLUE3;
    Switch LED1, LED2, LED3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Initialization
        final Button buttonConnect = findViewById(R.id.buttonConnect);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        //final ProgressBar progressBar = findViewById(R.id.progressBar);
        //progressBar.setVisibility(View.GONE);
        TextCondicoesAtuais = findViewById(R.id.textParametrosAtuais);
        TextServo = findViewById(R.id.textServo);
        TextRED1 = findViewById(R.id.textRED1);
        TextGREEN1 = findViewById(R.id.textGREEN1);
        TextBLUE1 = findViewById(R.id.textBLUE1);
        TextRED2 = findViewById(R.id.textRED2);
        //TextGREEN2 = findViewById(R.id.textGREEN2);
        TextBLUE2 = findViewById(R.id.textBLUE2);
        TextRED3 = findViewById(R.id.textRED3);
        //TextGREEN3 = findViewById(R.id.textGREEN3);
        TextBLUE3 = findViewById(R.id.textBLUE3);

        seekBarServo = findViewById(R.id.seekBarServo);
        seekBarRED1 = findViewById(R.id.seekBarLED1Red);
        seekBarGREEN1 = findViewById(R.id.seekBarLED1Green);
        seekBarBLUE1 = findViewById(R.id.seekBarLED1Blue);
        seekBarRED2 = findViewById(R.id.seekBarLED2Red);
        //seekBarGREEN2 = findViewById(R.id.seekBarLED2Green);
        seekBarBLUE2 = findViewById(R.id.seekBarLED2Blue);
        seekBarRED3 = findViewById(R.id.seekBarLED3Red);
        //seekBarGREEN3 = findViewById(R.id.seekBarLED3Green);
        seekBarBLUE3 = findViewById(R.id.seekBarLED3Blue);

        LED1 = findViewById(R.id.switch1);
        LED2 = findViewById(R.id.switch2);
        LED3 = findViewById(R.id.switch3);

        //final Button buttonToggle = findViewById(R.id.buttonToggle);
        //buttonToggle.setEnabled(false);
        //final ImageView imageView = findViewById(R.id.imageView);
        //imageView.setBackgroundColor(getResources().getColor(R.color.colorOff));

        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null){
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progree and connection status
            toolbar.setSubtitle("Conectando com " + deviceName + "...");
            //progressBar.setVisibility(View.VISIBLE);
            buttonConnect.setEnabled(false);

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter,deviceAddress);
            createConnectThread.start();
        }

        /*
        Second most important piece of Code. GUI Handler
         */
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg){
                switch (msg.what){
                    case CONNECTING_STATUS:
                        switch(msg.arg1){
                            case 1:
                                toolbar.setSubtitle("Conectado com " + deviceName);
                                //progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                //buttonToggle.setEnabled(true);
                                break;
                            case -1:
                                toolbar.setSubtitle("Problema ao conectar com o dispositivo!");
                                //progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        String arduinoMsg = msg.obj.toString(); // Ler mensagem recebida do Arduino
                                                                // --> Envia a cada seg o estado do sistema
                                                                // ****--> ou fazer um botao pro app pedir o estado atual
                        String temperatura = arduinoMsg.split("#")[0];
                        String umidade = arduinoMsg.split("#")[1];
                        String luminosidade1 = arduinoMsg.split("#")[2];
                        String luminosidade2 = arduinoMsg.split("#")[3];
                        String VermelhoLED1 = arduinoMsg.split("#")[4];
                        String VerdeLED1 = arduinoMsg.split("#")[5];
                        String AzulLED1 = arduinoMsg.split("#")[6];
                        String VermelhoLED2 = arduinoMsg.split("#")[7];
                        String AzulLED2 = arduinoMsg.split("#")[8];
                        String VermelhoLED3 = arduinoMsg.split("#")[9];
                        String AzulLED3 = arduinoMsg.split("#")[10];
                        String ModoOperacao = arduinoMsg.split("#")[11];
                        String posicaoServo = arduinoMsg.split("#")[12];

                        try {
                            String EstadoAtual = "Estado atual da estufa:\n";
                            EstadoAtual+=("Modo de Operação: "+ModoOperacao+"\n");
                            EstadoAtual+=("Temperatura: "+temperatura+"ºC      "+"Umidade Rel.: "+umidade+"%"+"\n");
                            EstadoAtual+=("Luminosidade Frente: "+luminosidade1+"      "+"Luminosidade Fundo: "+luminosidade2+"\n");
                            EstadoAtual+=("LED 1 (Frente): R: "+VermelhoLED1+"   G: "+VerdeLED1+"   B: "+AzulLED1+"\n");
                            EstadoAtual+=("LED 2 (Meio): R: "+VermelhoLED2+"   G: 0"+"   B: "+AzulLED2+"\n");
                            EstadoAtual+=("LED 3 (Fundo): R: "+VermelhoLED3+"   G: 0"+"   B: "+AzulLED3+"\n");
                            TextCondicoesAtuais.setText(EstadoAtual);

                            // ***********************************SETAR SWITCHES E SLIDERS DE ACORDO COM OS DADOS RECEBIDOS********************************
                            //P nao bugar o arduino, ele deve conferir ao receber uma msgm do android se os valores atuais jah nao sao esses --> ai ignora

                            //SE FOR DIFERENTE DO ESTADO ATUAL --> SETAR OS VALORES --> SENAO IGNORA

                            seekBarServo.setProgress(Integer.parseInt(posicaoServo));

                            seekBarRED1.setProgress(Integer.parseInt(VermelhoLED1));
                            seekBarGREEN1.setProgress(Integer.parseInt(VerdeLED1));
                            seekBarBLUE1.setProgress(Integer.parseInt(AzulLED1));

                            seekBarRED2.setProgress(Integer.parseInt(VermelhoLED2));
                            seekBarBLUE2.setProgress(Integer.parseInt(AzulLED2));

                            seekBarRED3.setProgress(Integer.parseInt(VermelhoLED3));
                            seekBarBLUE3.setProgress(Integer.parseInt(AzulLED3));

                            LED1.setChecked(((Integer.parseInt(VermelhoLED1)+Integer.parseInt(VerdeLED1)+Integer.parseInt(AzulLED1))!=0));
                            LED2.setChecked(((Integer.parseInt(VermelhoLED2)+Integer.parseInt(AzulLED2))!=0));
                            LED3.setChecked(((Integer.parseInt(VermelhoLED3)+Integer.parseInt(AzulLED3))!=0));

                            // ****************************************************************************************************************************
                        }
                        catch (Exception e){
                        }

                        /*switch (arduinoMsg.toLowerCase()){
                            case "led is turned on":
                                //imageView.setBackgroundColor(getResources().getColor(R.color.colorOn));
                                //textViewInfo.setText("Arduino Message : " + arduinoMsg);
                                break;
                            case "led is turned off":
                                //imageView.setBackgroundColor(getResources().getColor(R.color.colorOff));
                                //textViewInfo.setText("Arduino Message : " + arduinoMsg);
                                break;
                        }
                        break;*/
                }
            }
        };

        // Select Bluetooth Device
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to adapter list
                Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });

        seekBarServo
                .setOnSeekBarChangeListener(
                        new SeekBar
                                .OnSeekBarChangeListener() {

                            String comando = null;

                            // When the progress value has changed
                            @Override
                            public void onProgressChanged(
                                    SeekBar seekBar,
                                    int progress,
                                    boolean fromUser)
                            {
                                TextServo.setText("Posição atual: "+progress);
                                comando = "S1#"+progress;
                                //comando = "Servo#"+progress;
                                try{connectedThread.write(comando);}
                                catch (Exception ignored){}
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar)
                            {

                                // This method will automatically
                                // called when the user touches the SeekBar
                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar)
                            {

                                // This method will automatically
                                // called when the user
                                // stops touching the SeekBar
                            }
                        });

        seekBarRED1
                .setOnSeekBarChangeListener(
                        new SeekBar
                                .OnSeekBarChangeListener() {

                            String comando = null;

                            // When the progress value has changed
                            @Override
                            public void onProgressChanged(
                                    SeekBar seekBar,
                                    int progress,
                                    boolean fromUser)
                            {
                                TextRED1.setText("Intensidade Vermelho - LED 1: "+progress);
                                comando = "R1#"+progress;
                                //comando = "RED1#"+progress;
                                try{connectedThread.write(comando);}
                                catch (Exception ignored){}
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar)
                            {

                                // This method will automatically
                                // called when the user touches the SeekBar
                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar)
                            {

                                // This method will automatically
                                // called when the user
                                // stops touching the SeekBar
                            }
                        });

        seekBarRED2
                .setOnSeekBarChangeListener(
                        new SeekBar
                                .OnSeekBarChangeListener() {

                            String comando = null;

                            // When the progress value has changed
                            @Override
                            public void onProgressChanged(
                                    SeekBar seekBar,
                                    int progress,
                                    boolean fromUser)
                            {
                                TextRED2.setText("Intensidade Vermelho - LED 2: "+progress);
                                comando = "R2#"+progress;
                                //comando = "RED2#"+progress;
                                try{connectedThread.write(comando);}
                                catch (Exception ignored){}
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar)
                            {

                                // This method will automatically
                                // called when the user touches the SeekBar
                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar)
                            {

                                // This method will automatically
                                // called when the user
                                // stops touching the SeekBar
                            }
                        });

        seekBarRED3
                .setOnSeekBarChangeListener(
                        new SeekBar
                                .OnSeekBarChangeListener() {

                            String comando = null;

                            // When the progress value has changed
                            @Override
                            public void onProgressChanged(
                                    SeekBar seekBar,
                                    int progress,
                                    boolean fromUser)
                            {
                                TextRED3.setText("Intensidade Vermelho - LED 3: "+progress*255);
                                comando = "R3#"+progress;
                                //comando = "RED3#"+progress;
                                try{connectedThread.write(comando);}
                                catch (Exception ignored){}
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar)
                            {

                                // This method will automatically
                                // called when the user touches the SeekBar
                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar)
                            {

                                // This method will automatically
                                // called when the user
                                // stops touching the SeekBar
                            }
                        });

        seekBarGREEN1
                .setOnSeekBarChangeListener(
                        new SeekBar
                                .OnSeekBarChangeListener() {

                            String comando = null;

                            // When the progress value has changed
                            @Override
                            public void onProgressChanged(
                                    SeekBar seekBar,
                                    int progress,
                                    boolean fromUser)
                            {
                                TextGREEN1.setText("Intensidade Verde - LED 1: "+progress);
                                comando = "G1#"+progress;
                                //comando = "GREEN1#"+progress;
                                try{connectedThread.write(comando);}
                                catch (Exception ignored){}
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar)
                            {

                                // This method will automatically
                                // called when the user touches the SeekBar
                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar)
                            {

                                // This method will automatically
                                // called when the user
                                // stops touching the SeekBar
                            }
                        });

        seekBarBLUE1
                .setOnSeekBarChangeListener(
                        new SeekBar
                                .OnSeekBarChangeListener() {

                            String comando = null;

                            // When the progress value has changed
                            @Override
                            public void onProgressChanged(
                                    SeekBar seekBar,
                                    int progress,
                                    boolean fromUser)
                            {
                                TextBLUE1.setText("Intensidade Azul - LED 1: "+progress);
                                comando = "B1#"+progress;
                                //comando = "BLUE1#"+progress;
                                try{connectedThread.write(comando);}
                                catch (Exception ignored){}
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar)
                            {

                                // This method will automatically
                                // called when the user touches the SeekBar
                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar)
                            {

                                // This method will automatically
                                // called when the user
                                // stops touching the SeekBar
                            }
                        });

        seekBarBLUE2
                .setOnSeekBarChangeListener(
                        new SeekBar
                                .OnSeekBarChangeListener() {

                            String comando = null;

                            // When the progress value has changed
                            @Override
                            public void onProgressChanged(
                                    SeekBar seekBar,
                                    int progress,
                                    boolean fromUser)
                            {
                                TextBLUE2.setText("Intensidade Azul - LED 2: "+progress*255);
                                comando = "B2#"+progress;
                                //comando = "BLUE2#"+progress;
                                try{connectedThread.write(comando);}
                                catch (Exception ignored){}
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar)
                            {

                                // This method will automatically
                                // called when the user touches the SeekBar
                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar)
                            {

                                // This method will automatically
                                // called when the user
                                // stops touching the SeekBar
                            }
                        });

        seekBarBLUE3
                .setOnSeekBarChangeListener(
                        new SeekBar
                                .OnSeekBarChangeListener() {

                            String comando = null;

                            // When the progress value has changed
                            @Override
                            public void onProgressChanged(
                                    SeekBar seekBar,
                                    int progress,
                                    boolean fromUser)
                            {
                                TextBLUE3.setText("Intensidade Azul - LED 3: "+progress);
                                comando = "B3#"+progress;
                                //comando = "BLUE3#"+progress;
                                try{connectedThread.write(comando);}
                                catch (Exception ignored){}
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar)
                            {

                                // This method will automatically
                                // called when the user touches the SeekBar
                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar)
                            {

                                // This method will automatically
                                // called when the user
                                // stops touching the SeekBar
                            }
                        });

        LED1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            String comando = null;
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) //Line A
            {
                if(isChecked){
                    int R=255,G=155,B=255;
                    seekBarRED1.setProgress(R);
                    seekBarGREEN1.setProgress(G);
                    seekBarBLUE1.setProgress(B);
                    TextRED1.setText("Intensidade Vermelho - LED 1: "+R);
                    TextGREEN1.setText("Intensidade Verde - LED 1: "+G);
                    TextBLUE1.setText("Intensidade Azul - LED 1: "+B);
                }else { //Desligar LED
                    seekBarRED1.setProgress(0);
                    seekBarGREEN1.setProgress(0);
                    seekBarBLUE1.setProgress(0);
                    TextRED1.setText("Intensidade Vermelho - LED 1: 0");
                    TextGREEN1.setText("Intensidade Verde - LED 1: 0");
                    TextBLUE1.setText("Intensidade Azul - LED 1: 0");
                }
                comando = "L1#"+isChecked;
                //comando = "LED1#"+isChecked;
                try{connectedThread.write(comando);}
                catch (Exception ignored){}
            }
        });

        LED2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            String comando = null;
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) //Line A
            {
                if(isChecked){
                    int R=255,B=1;
                    seekBarRED2.setProgress(R);
                    seekBarBLUE2.setProgress(B);
                    TextRED2.setText("Intensidade Vermelho - LED 2: "+R);
                    TextBLUE2.setText("Intensidade Azul - LED 2: 255");
                }else { //Desligar LED
                    seekBarRED2.setProgress(0);
                    seekBarBLUE2.setProgress(0);
                    TextRED2.setText("Intensidade Vermelho - LED 2: 0");
                    TextBLUE2.setText("Intensidade Azul - LED 2: 0");
                }
                comando = "L2#"+isChecked;
                //comando = "LED2#"+isChecked;
                try{connectedThread.write(comando);}
                catch (Exception ignored){}
            }
        });

        LED3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            String comando = null;
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) //Line A
            {
                if(isChecked){
                    int R=1,B=255;
                    seekBarRED3.setProgress(R);
                    seekBarBLUE3.setProgress(B);
                    TextRED3.setText("Intensidade Vermelho - LED 3: 255");
                    TextBLUE3.setText("Intensidade Azul - LED 3: "+B);
                }else { //Desligar LED
                    seekBarRED3.setProgress(0);
                    seekBarBLUE3.setProgress(0);
                    TextRED3.setText("Intensidade Vermelho - LED 3: 0");
                    TextBLUE3.setText("Intensidade Azul - LED 3: 0");
                }
                comando = "L3#"+isChecked;
                //comando = "LED3#"+isChecked;
                try{connectedThread.write(comando);}
                catch (Exception ignored){}
            }
        });

        // Button to ON/OFF LED on Arduino Board
       /* buttonToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String cmdText = null;
                String btnState = buttonToggle.getText().toString().toLowerCase();
                switch (btnState){
                    case "turn on":
                        buttonToggle.setText("Turn Off");
                        // Command to turn on LED on Arduino. Must match with the command in Arduino code
                        cmdText = "<turn on>";
                        break;
                    case "turn off":
                        buttonToggle.setText("Turn On");
                        // Command to turn off LED on Arduino. Must match with the command in Arduino code
                        cmdText = "<turn off>";
                        break;
                }
                // Send command to Arduino board
                connectedThread.write(cmdText);
            }
        });*/
    }

    /* ============================ Thread to Create Bluetooth Connection =================================== */
    public static class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.e("Arduino Message",readMessage);
                        handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null){
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}