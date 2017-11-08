package com.example.martinartime.lightsaberapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.example.martinartime.lightsaberapp.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.ContentValues.TAG;

/**
 * Created by pablo on 28/9/2017.
 * Clase que implementa los metodos necesarios para registrar, desregistar y escuchar los sensores
 *
 * Datos enviados:
 *  - Luz debil: 0
 *  - Luz fuerte: 1
 *  - Choque: 2
 *  - Aceleracion: 3
 */

public class SensorActivity extends Activity implements SensorEventListener {

    //CONSTANTES
    private static final int SENSIBILIDAD_PROXIMIDAD = 4;
    private static int UMBRAL_MOVIMIENTO = 15;
    private static float UMBRAL_LUZ = 230;
    //SENSOR
    private SensorManager sensorManager;
    private Sensor aceletrometro;
    private Sensor proximidad;
    private Sensor luminico;
    //ACELEROMETRO
    private long ultimaActualizacion = 0;
    private float xAnterior;
    private float yAnterior;
    private float zAnterior;
    //DATOS A ENVIAR
    private static String DEBIL = "0";
    private static String FUERTE = "1";
    private static String CHOQUE = "2";
    private static String ACELERACION = "3";

    //BLUETOOTH
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();
    private ConnectedThread mConnectedThread;

    // SPP UUID service  - Funciona en la mayoria de los dispositivos
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address del Hc05
    private static String address = null;

    @BindView(R.id.tv_aceleracion_value)
    TextView tv_aceleracion;
    @BindView(R.id.tv_luminosidad_value)
    TextView tv_luminosidad;
    @BindView(R.id.tv_proximidad_value)
    TextView tv_proximidad;
    @BindView (R.id.barAzul)
    SeekBar barAzul;
    @BindView (R.id.barRojo)
    SeekBar barRojo;
    @BindView (R.id.botonColor)
    BootstrapButton botonColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_activity);

        ButterKnife.bind(this);

        //Obtengo el parametro, aplicando un Bundle, que me indica la Mac Adress del HC05

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        aceletrometro = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
        proximidad = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) : null;
        luminico = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) : null;

        //obtengo el adaptador del bluethoot
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        barAzul.getProgressDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.MULTIPLY);
        barAzul.getThumb().setColorFilter(Color.BLUE, PorterDuff.Mode.MULTIPLY);

        barRojo.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
        barRojo.getThumb().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);

        botonColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rojo = barRojo.getProgress();
                int azul = barAzul.getProgress();
                String color;

                if (rojo == 0 && azul == 0) {
                    Toast.makeText(getApplicationContext(), "Debe setear por lo menos un color", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (rojo > 0 && azul == 0) {
                    botonColor.setBackgroundColor(Color.RED);
                    color = "R";
                } else if (rojo == 0 && azul > 0) {
                    botonColor.setBackgroundColor(Color.BLUE);
                    color = "A";
                } else {
                    botonColor.setBackgroundColor(Color.rgb(255, 0, 255));
                    color = "V";
                }
                mConnectedThread.write(color);
            }
        });
    }

    @Override
    //Cada vez que se detecta el evento OnResume se establece la comunicacion con el HC05, creando un
    //socketBluethoot
    public void onResume() {
        super.onResume();

        Intent intent=getIntent();
        Bundle extras=intent.getExtras();

        address= extras != null ? extras.getString("Direccion_Bluethoot") : null;

        sensorManager.registerListener(this, aceletrometro, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, proximidad, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, luminico, SensorManager.SENSOR_DELAY_NORMAL);

        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        //se realiza la conexion del Bluethoot crea y se conectandose a atraves de un socket
        try
        {
            btSocket = createBluetoothSocket(device);
        }
        catch (IOException e)
        {
            Toast.makeText(getApplication().getApplicationContext(),"La creacción del Socket fallo",Toast.LENGTH_SHORT).show();
        }
        // Establish the Bluetooth socket connection.
        try
        {
            btSocket.connect();
        }
        catch (IOException e)
        {
            try
            {
                btSocket.close();
            }
            catch (IOException e2)
            {
                //insert code to deal with this
            }
        }


        //Una establecida la conexion con el Hc05 se crea el hilo secundario, el cual va a recibir
        // los datos de Arduino atraves del bluethoot
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
        mConnectedThread.write("x");
    }

    @Override
    //Cuando se ejecuta el evento onPause se cierra el socket Bluethoot, para no recibiendo datos
    public void onDestroy()
    {
        super.onDestroy();

        sensorManager.unregisterListener(this);

        try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();

            Intent data = new Intent();
            data.putExtra("Direccion_Bluethoot", address);

            //Toast.makeText(this, "SENSORACTIVITY A MAIN ADDRESS:"+address, Toast.LENGTH_SHORT).show();

            setResult(RESULT_OK, data);
            finish();

        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    //Metodo que crea el socket bluethoot
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    public SensorActivity() {}

    @Override
    public void onSensorChanged(SensorEvent event) {

        synchronized (this) {

            Sensor sensorQueCambio = event.sensor;

            if (sensorQueCambio.getType() == Sensor.TYPE_ACCELEROMETER) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                long tiempoActual = System.currentTimeMillis();
                if ((tiempoActual - ultimaActualizacion) > 100) {

                    ultimaActualizacion = tiempoActual;

                    double aceleracion = calcularAceleracion(x, y, z);

                    tv_aceleracion.setText(String.valueOf(aceleracion));
                    Log.e(TAG, "onSensorChanged: "+String.valueOf(aceleracion) );
                    if (aceleracion > UMBRAL_MOVIMIENTO) {
                        if (btSocket != null) {
                            mConnectedThread.write(ACELERACION);
                        }
                        //Toast.makeText(getApplicationContext(), "Hubo aceleración", Toast.LENGTH_SHORT).show();
                    } else {
                        //Toast.makeText(getApplicationContext(), "Neutro", Toast.LENGTH_SHORT).show();
                    }

                    xAnterior = x;
                    yAnterior = y;
                    zAnterior = z;
                }
            } else if (sensorQueCambio.getType() == Sensor.TYPE_PROXIMITY) {
                float valor = event.values[0];

                tv_proximidad.setText(String.valueOf(valor));

                if (valor >= -SENSIBILIDAD_PROXIMIDAD && valor <= SENSIBILIDAD_PROXIMIDAD) {
                    if (btSocket != null) {
                        mConnectedThread.write(CHOQUE);
                    }
                    //Toast.makeText(getApplicationContext(), "cerca", Toast.LENGTH_SHORT).show();
                } else {
                    //Toast.makeText(getApplicationContext(), "lejos", Toast.LENGTH_SHORT).show();
                }
            } else if (sensorQueCambio.getType() == Sensor.TYPE_LIGHT) {
                float valor = event.values[0];

                tv_luminosidad.setText(String.valueOf(valor));

                if (valor >= UMBRAL_LUZ) {
                    if (btSocket != null) {
                        mConnectedThread.write(FUERTE);
                    }
                   // Toast.makeText(getApplicationContext(), "luz fuerte", Toast.LENGTH_SHORT).show();
                } else {
                    if (btSocket != null) {
                        mConnectedThread.write(DEBIL);
                    }
                 //   Toast.makeText(getApplicationContext(), "luz débil", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    /**
     * Calcula la aceleración a partir de los valores x, y, z actuales y los anteriores.
     *
     * @param x
     * @param y
     * @param z
     * @return
     */
    private double calcularAceleracion(float x, float y, float z) {
        double normal = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
        double normalAnterior = Math.sqrt(Math.pow(xAnterior, 2) + Math.pow(yAnterior, 2) + Math.pow(zAnterior, 2));
        return Math.abs(normal - normalAnterior);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //Constructor de la clase del hilo secundario
        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try
            {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        //metodo run del hilo, que va a entrar en una espera activa para recibir los msjs del HC05
        public void run() {
            while (true) {}
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getApplication().getApplicationContext(),"La conexion fallo",Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
