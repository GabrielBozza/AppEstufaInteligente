#include <Servo.h>
#include <DHT.h>
 
#define SERVO 9 // Porta Digital 6 PWM
#define DHTPIN A2 // pino que estamos conectado
#define DHTTYPE DHT11 // DHT 11
#define ldr_pin1 A0

///****************ARRUMAR NUMMEROS DOSPINOS ANTES DE USAR*************
#define Vermelho_LED2 3
#define Azul_LED2 10
#define Verde_LED2 5
#define Vermelho_LED1 3
#define Vermelho_LED3 7
#define Azul_LED1 5
#define Azul_LED3 4

//LED 1 --> FRENTE
//LED 2 --> MEIO
//LED 3 --> FUNDO

DHT dht(DHTPIN, DHTTYPE);
Servo motor; // Variável Servo
int pos; // Posição Servo
String modo = "normal";
String parametros_atuais = "";
String msg,cmd,valor_cmd;

void setup() {
  pinMode(ldr_pin1,INPUT); //ANALÓGICA
  pinMode(Vermelho_LED2, OUTPUT); //DIGITAL PWM
  pinMode(Verde_LED2, OUTPUT); //DIGITAL PWM
  pinMode(Azul_LED2, OUTPUT); //DIGITAL PWM
  //pinMode(Vermelho_LED1, OUTPUT); //DIGITAL PWM
  //pinMode(Azul_LED1, OUTPUT); //DIGITAL
  //pinMode(Vermelho_LED3, OUTPUT); //DIGITAL
  //pinMode(Azul_LED3, OUTPUT); //DIGITAL PWM
  motor.attach(SERVO); //DIGITAL PWM
  Serial.begin(9600); // Communication rate of the Bluetooth Module
  motor.write(0); // Inicia motor posição zero
  dht.begin();
  msg = "";
}

void loop() {
  
  //ler dados dos sensores
  double luminosidade_ldr1 = ( 1023 - analogRead( ldr_pin1 ));
  float umidade = dht.readHumidity();
  float temperatura = dht.readTemperature();
  
  // To read message received from other Bluetooth Device
  if (Serial.available() > 0){ // Check if there is data coming
    msg = Serial.readString(); // Read the message as String
    cmd = msg.substring(0,2);//<S1-Servo1> , <R1-RED/LED1> , <L1-LED1> , <MO-Modo de Operacao>
    valor_cmd = msg.substring(3);
    //Serial.println("Android Command: " + msg);
  }

  if(cmd == "S1"){ //SERVO MOTOR1
     SetarPosicaoEscotilha(valor_cmd.toInt());
  }else if(cmd == "R1"){
     analogWrite(Vermelho_LED2, valor_cmd.toInt());
  }else if(cmd == "G1"){
     analogWrite(Verde_LED2, valor_cmd.toInt());
  }else if(cmd == "B1"){
     analogWrite(Azul_LED2, valor_cmd.toInt());
  }else if(cmd == "R2"){
     analogWrite(Vermelho_LED1, valor_cmd.toInt());
  }else if(cmd == "B2"){
     digitalWrite(Azul_LED1, valor_cmd.toInt()==0?LOW:HIGH);
  }else if(cmd == "R3"){
     digitalWrite(Vermelho_LED3, valor_cmd.toInt()==0?LOW:HIGH);
  }else if(cmd == "B3"){
     analogWrite(Azul_LED3, valor_cmd.toInt());
  }else if(cmd == "L1"){
     analogWrite(Vermelho_LED2, valor_cmd=="false"?0:255);
     analogWrite(Verde_LED2, valor_cmd=="false"?0:255);
     analogWrite(Azul_LED2, valor_cmd=="false"?0:255);
  }else if(cmd == "L2"){
     analogWrite(Vermelho_LED1, valor_cmd=="false"?0:255);
     digitalWrite(Azul_LED1, valor_cmd=="false"?LOW:HIGH);
  }else if(cmd == "L3"){
     digitalWrite(Vermelho_LED3, valor_cmd=="false"?LOW:HIGH);
     analogWrite(Azul_LED3, valor_cmd=="false"?0:255);
  }else if(cmd == "MO"){
     modo = valor_cmd;
  }

  msg = ""; // limpa a mensagem recebida
  parametros_atuais+=(String(temperatura)+'#'+String(umidade)+'#'+String(luminosidade_ldr1)+'#'+String('0')+'#'+String(255)+'#'+String(55)+'#'+String(150)+'#'+String(200)+'#'+String(100)+'#'+String(200)+'#'+String(100)+'#'+modo+'#'+String(motor.read()));
  //parametros_atuais.concat(String(temperatura));//.concat('#').concat(String(umidade));
  Serial.println(parametros_atuais); // Envia mensagem para aplcativo android
  parametros_atuais=" ";
  delay(100);
}

void SetarPosicaoEscotilha(int abertura){
  // abertura --> valor em GRAUS da abertura desejada (-90 a 90 graus) 

  int posicaoAtual = motor.read();
  //Serial.print("Posicao atual");
  //Serial.println(posicaoAtual);
  
  
  if(posicaoAtual < abertura ){ // abrir escotilha até que a abertura seja = 'abertura'
    for(pos = posicaoAtual; pos < abertura; pos++)
    {
      motor.write(pos);
      delay(15);// Movimenta a cada 15 ms --> abertura gradual
    }
  }
  else if (posicaoAtual > abertura){ // fechar escotilha até que a abertura seja = 'abertura'
    for(pos = posicaoAtual; pos >= abertura; pos--)
    {
      motor.write(pos);
      delay(15); // Movimenta a cada 15 ms --> fechamento gradual
    }
  }
 }
