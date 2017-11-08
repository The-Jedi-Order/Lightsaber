#include <Wire.h>
#include <I2Cdev.h>
#include <MPU6050.h>
#include <SoftwareSerial.h>
#include <SD.h>
#include <TMRpcm.h>

#define DEBUGG_MODE 0

MPU6050 mpu;

TMRpcm tmrpcm;
SoftwareSerial BTserial(3,7); // RX | TX

int16_t ax, ay, az;
int16_t gx, gy, gz;
double normal;
double normal2;
int contadorDeTurno=0;
int rojo=0;
int azul=0; 


//Constantes para pines de sensores
#define PIN_SENSOR_LDR_ANALOGICO A3
#define PIN_SCL_6050_ANALOGICO A5
#define PIN_SDA_6050_ANALOGICO A4
#define PIN_REED_DIGITAL 8

//Constantes para pines de Actuadores
#define PIN_VIBRADOR_DIGITAL 10
#define PIN_COLOR_AZUL 6
#define PIN_COLOR_ROJO 5

// Supuestamente se necesita esto para haer andar el LED RGB Anodo
#define COMMON_ANODE

#define DEFAULT_COLOR 0
#define SD_ChipSelectPin 4
//umbrales
#define UMBRAL_ACELEROMETRO 18000

//contadores de sonido
int luz=0;
boolean encendido=false;

void setup() {
  // Seteo de pines ANALOGICOS
  pinMode(PIN_SENSOR_LDR_ANALOGICO, INPUT);
  Wire.begin();
  Serial.begin(9600);
  // Seteo de pines DIGITALES
  pinMode(PIN_REED_DIGITAL, INPUT);

  //configuracion de vibrador
    pinMode(PIN_VIBRADOR_DIGITAL, OUTPUT);
     Serial.println("Apagamos");
  //fin
  //configuracion luces
    pinMode(PIN_COLOR_ROJO, OUTPUT);
    pinMode(PIN_COLOR_AZUL, OUTPUT);
  //fin

  //configuracion sonido
   tmrpcm.speakerPin = 9;
   if (!SD.begin(SD_ChipSelectPin)) {  // see if the card is present and can be initialized:
    Serial.println("SD fail");  
    return;   // don't do anything more if not
  }else{
    Serial.println("todo okkkkkkkkkk");
   }
   
  //configuracion mpu
  mpu.initialize();
  pinMode(PIN_SCL_6050_ANALOGICO, INPUT);
  pinMode(PIN_SDA_6050_ANALOGICO, INPUT);
  Serial.println(mpu.testConnection() ? "Connected" : "Connection failed");
  mpu.setXAccelOffset(-1537);
  mpu.setYAccelOffset(53);
  mpu.setZAccelOffset(1190);
  mpu.setXGyroOffset(-70);
  mpu.setYGyroOffset(63);
  mpu.setZGyroOffset(-14);
  //fin

   BTserial.begin(9600); 
  Serial.print("ready");
}

void loop() {
   analogWrite(PIN_VIBRADOR_DIGITAL,0);
   encendido = reedEncendido();
  //encendido=true;
   //Serial.println(encendido);
  if(encendido){
    azul=255;
    rojo=255;
    setearColor(azul,rojo); 
    tmrpcm.play("on.wav");
    while(tmrpcm.isPlaying()){ 
      analogWrite(PIN_VIBRADOR_DIGITAL,255);
    }
    analogWrite(PIN_VIBRADOR_DIGITAL,0);
    while(encendido){
        leerBluetooth(); 
        sensarMovimiento();
        sensar();
     // Serial.println(encendido);
        if(!tmrpcm.isPlaying()){
          tmrpcm.play("quieto.wav");
        }    
     }
    tmrpcm.play("off.wav");
    
    while(tmrpcm.isPlaying()){
        analogWrite(PIN_VIBRADOR_DIGITAL,255);
    }
    azul=0;
    rojo=0;
    setearColor(azul,rojo);
    analogWrite(PIN_VIBRADOR_DIGITAL,0);
  }
}

 

/*
 * devuelve verdadero si el reed detecta el magnetismo
 */
boolean reedEncendido(){
  if(digitalRead(PIN_REED_DIGITAL) == HIGH){
      return true;
  }
  else{
    return false;
  }
}

/*
 * obtiene el valor sensado por el ldr
 * @author pablo
 */
void sensarLuz(){
    luz=analogRead(PIN_SENSOR_LDR_ANALOGICO);
    Serial.println("LUZZZZZZ");
    Serial.println(luz);
    if(luz>270){
        if(azul>0){
          azul=255;
        }
        if(rojo>0){
          rojo=255;
        }
    }else{
        if(azul>0){
          azul=10;
        }
        if(rojo>0){
          rojo=10;
        }
    }
     setearColor(azul,rojo);
}
 /*
 * obtiene el valor sensado por el acelerometro
 */
void sensarMovimiento(){
 mpu.getAcceleration(&ax, &ay, &az);

  normal=sqrt(pow(ax,2)+pow(ay,2)+pow(az,2));
  if(normal>UMBRAL_ACELEROMETRO){
    Serial.println(normal);
    
    tmrpcm.play("swing.wav");
    while(tmrpcm.isPlaying()){
     // analogWrite(PIN_VIBRADOR_DIGITAL,255);
     Serial.println("reproduciendo sonido movimiento");
      sensar();
      leerBluetooth(); 
    }
    analogWrite(PIN_VIBRADOR_DIGITAL,0);   
    tmrpcm.play("quieto.wav");
  }
} 

/*
   mÃ©todo que indicarÃ¡ si el sable de luz produjo un movimiento.
   @author: Pablo
*/
//detecta movimiento segun el umbral
bool hayMovimiento(double movimiento) {
  bool flagMovimiento = 0;
  if(movimiento>UMBRAL_ACELEROMETRO){
    flagMovimiento = 1;
   Serial.println("Hay movimiento");
  }
  return flagMovimiento;
}
/*
 * obtiene el valor enviado por bluetooth
 */
void leerBluetooth(){
  Serial.println("leyendo BT");
  if(BTserial.available()>0){
    switch((char)BTserial.read()){
      case '0':
        Serial.println("DEBIL");
        if(azul>0){
          azul=1;
        }
        if(rojo>0){
          rojo=1;
        }
        setearColor(azul,rojo);
      break;
      case '1':
        Serial.println("FUERTE");
        if(azul>0){
          azul=255;
        }
        if(rojo>0){
          rojo=255;
        }
        setearColor(azul,rojo);
      break;
      case '2':
         Serial.println("CHOQUE");
         tmrpcm.play("Saberblk.wav");
         while(tmrpcm.isPlaying()){
          sensar();
         }
         break;
       case '3':
         Serial.println("ACELERACION");
         tmrpcm.play("swing.wav");
         while(tmrpcm.isPlaying()){
          sensar();
         }
         break;
        case 'A':
         azul=255;
         rojo=0;
   
         setearColor(azul,rojo);
        break;
        case 'R':
         azul=0;
         rojo=255;
         setearColor(azul,rojo);
        break;
        case 'V':
         azul=255;
         rojo=255;
         setearColor(azul,rojo);
        break;

    }
  }
}

/*
 * setea el color del sable
 * @author pablo
 */
void setearColor(int azul, int rojo){
   analogWrite(PIN_COLOR_AZUL,azul);
   analogWrite(PIN_COLOR_ROJO,rojo);
}


/*
 * Llama a los metodos que leen los sensores y el BT.
 */
void sensar(){
    
    sensarLuz();
  encendido=reedEncendido();
}


