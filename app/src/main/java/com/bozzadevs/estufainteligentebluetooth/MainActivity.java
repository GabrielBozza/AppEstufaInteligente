package com.bozzadevs.estufainteligentebluetooth;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import static android.content.ContentValues.TAG;
import static androidx.core.app.NotificationCompat.*;

public class MainActivity extends AppCompatActivity {

    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

    Spinner spinnerModoOp;
    SeekBar seekBarServo, seekBarRED1, seekBarGREEN2, seekBarBLUE1, seekBarRED2, seekBarRED3, seekBarBLUE2, seekBarBLUE3;
    TextView TextCondicoesAtuais, TextServo, TextRED1, TextGREEN2, TextBLUE1, TextRED2, TextBLUE2,TextRED3, TextBLUE3;
    Switch LED1, LED2, LED3;
    Button buttonAtualizaDataHora;
    Integer tempAlta, tempBaixa;
    Boolean AtualizarValoresInterface=true;
    //Builder builder;
    //NotificationManagerCompat notificationManager;
    //NotificationCompat.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.getSupportActionBar().hide();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel("Notif","Minha notif",NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builderQuente = new NotificationCompat.Builder(this, "Notif")
                .setSmallIcon(R.drawable.ic_termometro)
                .setContentTitle("Estufa muito quente!")
                .setContentText("Cuide bem de suas plantas!");

        NotificationCompat.Builder builderFrio = new NotificationCompat.Builder(this, "Notif")
                .setSmallIcon(R.drawable.ic_termometro)
                .setContentTitle("Estufa muito fria!")
                .setContentText("Cuide bem de suas plantas!");

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // UI Initialization
        final Button buttonConnect = findViewById(R.id.buttonConnect);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        tempAlta = 0;
        tempBaixa = 0;
        //final ProgressBar progressBar = findViewById(R.id.progressBar);
        //progressBar.setVisibility(View.GONE);
        TextCondicoesAtuais = findViewById(R.id.textParametrosAtuais);
        TextServo = findViewById(R.id.textServo);
        TextRED2 = findViewById(R.id.textRED1);
        TextGREEN2 = findViewById(R.id.textGREEN1);
        TextBLUE2 = findViewById(R.id.textBLUE1);
        TextRED1= findViewById(R.id.textRED2);
        //TextGREEN2 = findViewById(R.id.textGREEN2);
        TextBLUE1 = findViewById(R.id.textBLUE2);
        TextRED3 = findViewById(R.id.textRED3);
        //TextGREEN3 = findViewById(R.id.textGREEN3);
        TextBLUE3 = findViewById(R.id.textBLUE3);

        seekBarServo = findViewById(R.id.seekBarServo);
        seekBarRED2 = findViewById(R.id.seekBarLED1Red);
        seekBarGREEN2 = findViewById(R.id.seekBarLED1Green);
        seekBarBLUE2 = findViewById(R.id.seekBarLED1Blue);
        seekBarRED1 = findViewById(R.id.seekBarLED2Red);
        //seekBarGREEN2 = findViewById(R.id.seekBarLED2Green);
        seekBarBLUE1 = findViewById(R.id.seekBarLED2Blue);
        seekBarRED3 = findViewById(R.id.seekBarLED3Red);
        //seekBarGREEN3 = findViewById(R.id.seekBarLED3Green);
        seekBarBLUE3 = findViewById(R.id.seekBarLED3Blue);

        LED2 = findViewById(R.id.switch1);
        LED1 = findViewById(R.id.switch2);
        LED3 = findViewById(R.id.switch3);

        List<String> ListaModosOp = new ArrayList<>(Arrays.asList("Normal","Crescimento","Floração","Dormência"));

        spinnerModoOp = findViewById(R.id.spinnerModoOp);
        buttonAtualizaDataHora = findViewById(R.id.buttonAtualizaData);

        ArrayAdapter<String> adapterModo = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ListaModosOp);
        adapterModo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModoOp.setAdapter(adapterModo);

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
                                buttonConnect.setEnabled(true);
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM-HH:mm", Locale.getDefault());
                                //sdf.setTimeZone(TimeZone.getDefault());
                                String DataHoraAtual = sdf.format(new Date());
                                try{connectedThread.write("DT#"+DataHoraAtual);
                                Log.i(TAG,"Comando enviado: "+"DT#"+DataHoraAtual);}//Envia a data e hora ao se conectar
                                catch (Exception ignored){}
                                break;
                            case -1:
                                toolbar.setSubtitle("Problema ao conectar com o dispositivo!");
                                buttonConnect.setEnabled(true);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        String arduinoMsg = msg.obj.toString(); // Ler mensagem recebida do Arduino
                                                                // --> Envia a cada seg o estado do sistema
                                                                // ****--> ou fazer um botao pro app pedir o estado atual
                        try {
                            String temperatura = arduinoMsg.split("#")[0];
                            String umidade = arduinoMsg.split("#")[1];
                            String luminosidade1 = arduinoMsg.split("#")[2];
                            String luminosidade2 = arduinoMsg.split("#")[3];
                            String VermelhoLED2 = arduinoMsg.split("#")[4];
                            String VerdeLED2 = arduinoMsg.split("#")[5];
                            String AzulLED2 = arduinoMsg.split("#")[6];
                            String VermelhoLED1 = arduinoMsg.split("#")[7];
                            String AzulLED1 = arduinoMsg.split("#")[8];
                            String VermelhoLED3 = arduinoMsg.split("#")[9];
                            String AzulLED3 = arduinoMsg.split("#")[10];
                            String ModoOperacao = arduinoMsg.split("#")[11];
                            String posicaoServo = arduinoMsg.split("#")[12];
                            String dia = arduinoMsg.split("#")[13];
                            String mes = arduinoMsg.split("#")[14];
                            String hora = arduinoMsg.split("#")[15];
                            String minuto = arduinoMsg.split("#")[16];
                            String estacao_ano = arduinoMsg.split("#")[17];

                            Integer posicao = 180-Integer.parseInt(posicaoServo);

                            switch (estacao_ano) {
                                case "0":
                                    estacao_ano = "Verão";
                                    break;
                                case "1":
                                    estacao_ano = "Outono";
                                    break;
                                case "2":
                                    estacao_ano = "Inverno";
                                    break;
                                case "3":
                                    estacao_ano = "Primavera";
                                    break;
                            }

                            estacao_ano = "Outono";

                            switch (ModoOperacao) {
                                case "NORM":
                                    ModoOperacao = "Normal";
                                    break;
                                case "CRES":
                                    ModoOperacao = "Crescimento";
                                    break;
                                case "FLOR":
                                    ModoOperacao = "Floração";
                                    break;
                                case "DORM":
                                    ModoOperacao = "Dormência";
                                    break;
                            }

                            String EstadoAtual = "Estado atual da estufa:\n";
                            EstadoAtual+=("Data: "+dia+"/"+mes+"  -  "+hora+":"+minuto+"  Estação: "+estacao_ano+"\n");
                            EstadoAtual+=("Modo de Operação: "+ModoOperacao+"\n");
                            EstadoAtual+=("Posição das escotilhas: "+posicao+"º\n");
                            EstadoAtual+=("Temperatura: "+temperatura+"ºC      "+"Umidade Rel.: "+umidade+"%"+"\n");
                            EstadoAtual+=("Luminosidade Frente: "+luminosidade1+"      "+"\nLuminosidade Fundo: "+luminosidade2+"\n");
                            EstadoAtual+=("LED 1 (Frente): R: "+VermelhoLED1+"   G: 0"+"   B: "+AzulLED1+"\n");
                            EstadoAtual+=("LED 2 (Meio): R: "+VermelhoLED2+"   G: "+VerdeLED2+"   B: "+AzulLED2+"\n");
                            EstadoAtual+=("LED 3 (Fundo): R: "+VermelhoLED3+"   G: 0"+"   B: "+AzulLED3+"\n");
                            TextCondicoesAtuais.setText(EstadoAtual);

                            if(Float.parseFloat(temperatura) > 28.0){
                                tempAlta+=1;
                                if(tempAlta>240){ //Mais de 2 minutos seguidos com temp > 28
                                    tempAlta = 0; //Zera  "timer"
                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                                    notificationManager.notify(1, builderQuente.build());
                                }
                            } else {
                                tempAlta = 0;
                            }
                            if(Float.parseFloat(temperatura) < 20.0){
                                tempBaixa+=1;
                                if(tempBaixa>240){ //Mais de 2 min com temp < 20
                                    tempBaixa=0; //Zera  "timer"
                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                                    notificationManager.notify(2, builderFrio.build());
                                }
                            } else{
                                tempBaixa=0;
                            }

                            // ***********************************SETAR SWITCHES E SLIDERS DE ACORDO COM OS DADOS RECEBIDOS********************************
                            //COLOCAR UM SEMAFORO PARA SOH SETAR OS VALORES UMA UNICA VEZ
                            //SE FOR DIFERENTE DO ESTADO ATUAL --> SETAR OS VALORES --> SENAO IGNORA

                           if(AtualizarValoresInterface){
                                AtualizarValoresInterface = false; //semaforo
                                seekBarServo.setProgress(Integer.parseInt(posicaoServo));
                                seekBarRED1.setProgress(Integer.parseInt(VermelhoLED1));
                                seekBarBLUE1.setProgress(Integer.parseInt(AzulLED1));
                                seekBarRED2.setProgress(Integer.parseInt(VermelhoLED2));
                                seekBarGREEN2.setProgress(Integer.parseInt(VerdeLED2));
                                seekBarBLUE2.setProgress(Integer.parseInt(AzulLED2));
                                seekBarRED3.setProgress(Integer.parseInt(VermelhoLED3));
                                seekBarBLUE3.setProgress(Integer.parseInt(AzulLED3));

                                LED1.setChecked(((Integer.parseInt(VermelhoLED1)+Integer.parseInt(AzulLED1))!=0));
                                LED2.setChecked(((Integer.parseInt(VermelhoLED2)+Integer.parseInt(VerdeLED2)+Integer.parseInt(AzulLED2))!=0));
                                LED3.setChecked(((Integer.parseInt(VermelhoLED3)+Integer.parseInt(AzulLED3))!=0));
                            }
                        }
                        catch (Exception ignored){
                        }
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

        buttonAtualizaDataHora.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM-HH:mm", Locale.getDefault());
                //sdf.setTimeZone(TimeZone.getDefault());
                String DataHoraAtual = sdf.format(new Date());
                Toast.makeText(getApplicationContext(), "Data - Hora : " + DataHoraAtual, Toast.LENGTH_LONG).show();
                //notificationManager.notify(1, builderQuente.build());
                //notificationManager.notify(2, builderFrio.build());

                try {
                    connectedThread.write("DT#" + DataHoraAtual);
                }
                catch (Exception ignored){}
            }
        });

        spinnerModoOp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // Get the spinner selected item text
                String selectedItemText = (String) adapterView.getItemAtPosition(i);
                if(selectedItemText=="Normal"){
                    selectedItemText = "NORM";
                }else if(selectedItemText=="Crescimento"){
                    selectedItemText = "CRES";
                }else if(selectedItemText=="Floração"){
                    selectedItemText = "FLOR";
                }else if(selectedItemText=="Dormência"){
                    selectedItemText = "DORM";
                }

                String comando = "MO#"+selectedItemText;
                try{connectedThread.write(comando);}
                catch (Exception ignored){}
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        } );

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
                                String ProgressString = String.valueOf(progress);
                                if(progress<10){
                                    ProgressString = "0"+progress; //deixa os comandos com o mesmo tamanho
                                }
                                comando = "S1#"+ProgressString;
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
                                TextRED1.setText("Intensidade Vermelho - LED 1 (Frente): "+progress*255);
                                String Progress = String.valueOf(progress);
                                if(progress<10){
                                    Progress = "00"+progress;
                                } else if(progress<100){
                                    Progress = "0"+progress;
                                }
                                comando = "R2#"+Progress;
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
                                TextRED2.setText("Intensidade Vermelho - LED 2 (Meio): "+progress);
                                String Progress = String.valueOf(progress);
                                if(progress<10){
                                    Progress = "00"+progress;
                                } else if(progress<100){
                                    Progress = "0"+progress;
                                }
                                comando = "R1#"+Progress;
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
                                TextRED3.setText("Intensidade Vermelho - LED 3 (Fundo): "+progress*255);
                                String Progress = String.valueOf(progress);
                                if(progress<10){
                                    Progress = "00"+progress;
                                } else if(progress<100){
                                    Progress = "0"+progress;
                                }
                                comando = "R3#"+Progress;
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

        seekBarGREEN2
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
                                TextGREEN2.setText("Intensidade Verde - LED 2 (Meio): "+progress);
                                String Progress = String.valueOf(progress);
                                if(progress<10){
                                    Progress = "00"+progress;
                                } else if(progress<100){
                                    Progress = "0"+progress;
                                }
                                comando = "G1#"+Progress;
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
                                TextBLUE1.setText("Intensidade Azul - LED 1 (Frente): "+progress*255);
                                String Progress = String.valueOf(progress);
                                if(progress<10){
                                    Progress = "00"+progress;
                                } else if(progress<100){
                                    Progress = "0"+progress;
                                }
                                comando = "B2#"+Progress;
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
                                TextBLUE2.setText("Intensidade Azul - LED 2 (Meio): "+progress);
                                String Progress = String.valueOf(progress);
                                if(progress<10){
                                    Progress = "00"+progress;
                                } else if(progress<100){
                                    Progress = "0"+progress;
                                }
                                comando = "B1#"+Progress;
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
                                TextBLUE3.setText("Intensidade Azul - LED 3 (Fundo): "+progress*255);
                                String Progress = String.valueOf(progress);
                                if(progress<10){
                                    Progress = "00"+progress;
                                } else if(progress<100){
                                    Progress = "0"+progress;
                                }
                                comando = "B3#"+Progress;
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

        LED2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            String comando = null;
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) //Line A
            {
                if(isChecked){
                    int R=255,G=155,B=255;
                    seekBarRED2.setProgress(R);
                    seekBarGREEN2.setProgress(G);
                    seekBarBLUE2.setProgress(B);
                    TextRED2.setText("Intensidade Vermelho - LED 2 (Meio): "+R);
                    TextGREEN2.setText("Intensidade Verde - LED 2 (Meio): "+G);
                    TextBLUE2.setText("Intensidade Azul - LED 2 (Meio): "+B);
                }else { //Desligar LED
                    seekBarRED2.setProgress(0);
                    seekBarGREEN2.setProgress(0);
                    seekBarBLUE2.setProgress(0);
                    TextRED2.setText("Intensidade Vermelho - LED 2 (Meio): 0");
                    TextGREEN2.setText("Intensidade Verde - LED 2 (Meio): 0");
                    TextBLUE2.setText("Intensidade Azul - LED 2 (Meio): 0");
                }
                String isCheckedString = "0";
                if(isChecked) isCheckedString = "1";
                comando = "L1#"+isCheckedString;
                //comando = "LED1#"+isChecked;
                try{connectedThread.write(comando);}
                catch (Exception ignored){}
            }
        });

        LED1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            String comando = null;
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) //Line A
            {
                if(isChecked){
                    int R=255,B=1;
                    seekBarRED1.setProgress(R);
                    seekBarBLUE1.setProgress(B);
                    TextRED1.setText("Intensidade Vermelho - LED 1 (Frente): "+R);
                    TextBLUE1.setText("Intensidade Azul - LED 1 (Frente): 255");
                }else { //Desligar LED
                    seekBarRED1.setProgress(0);
                    seekBarBLUE1.setProgress(0);
                    TextRED1.setText("Intensidade Vermelho - LED 1 (Frente): 0");
                    TextBLUE1.setText("Intensidade Azul - LED 1 (Frente): 0");
                }
                String isCheckedString = "0";
                if(isChecked) isCheckedString = "1";
                comando = "L2#"+isCheckedString;
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
                    TextRED3.setText("Intensidade Vermelho - LED 3 (Funndo): 255");
                    TextBLUE3.setText("Intensidade Azul - LED 3 (Funndo): "+B);
                }else { //Desligar LED
                    seekBarRED3.setProgress(0);
                    seekBarBLUE3.setProgress(0);
                    TextRED3.setText("Intensidade Vermelho - LED 3 (Funndo): 0");
                    TextBLUE3.setText("Intensidade Azul - LED 3 (Funndo): 0");
                }
                String isCheckedString = "0";
                if(isChecked) isCheckedString = "1";
                comando = "L3#"+isCheckedString;
                //comando = "LED3#"+isChecked;
                try{connectedThread.write(comando);}
                catch (Exception ignored){}
            }
        });
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