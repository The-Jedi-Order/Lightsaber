package com.example.martinartime.lightsaberapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.example.martinartime.lightsaberapp.bluetooth.ConectarBluetoothActivity;
import com.example.martinartime.lightsaberapp.bluetooth.DeviceListActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    static final String TAG_MAIN = "";
    String address;

    /**
     * Se hace el binding de las views
     * Se obtienen los equipos bluetooth apareados
     * Muestra la conexion actual
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        //Obtengo el parametro, aplicando un Bundle, que me indica la Mac Adress del HC05
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        address = extras != null ? extras.getString("Direccion_Bluethoot") : null;

        //Toast.makeText(this, "MAIN ON RESUME ADDRESS:"+address, Toast.LENGTH_SHORT).show();
    }

    /**
     * Iniciar actividad ManejarBluetooth
     */
    @OnClick(R.id.cardBluetooth)
    public void conectarBluetooth(){
        Intent intent = new Intent(getApplicationContext(), ConectarBluetoothActivity.class);
        startActivity(intent);
    }

    /**
     * Iniciar actividad sensorActivity
     */
    @OnClick(R.id.cardAcciones)
    public void sensorActivity(){
        if(address != null){
            Intent i = new Intent(MainActivity.this, SensorActivity.class);
            i.putExtra("Direccion_Bluethoot", address);

            //Toast.makeText(this, "MAIN A SENSORACTIVITY ADDRESS:"+address, Toast.LENGTH_SHORT).show();
            startActivityForResult(i, 1);
        }
        else{
            Toast.makeText(this, "Conectarse a bluetooth primero", Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            address = data.getStringExtra("Direccion_Bluethoot");

            //Toast.makeText(this, "ON ACTIVITY RESULT ADDRESS:"+address, Toast.LENGTH_SHORT).show();
        }
    }
}
