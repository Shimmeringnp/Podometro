package com.example.podometro;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    SensorManager sensorManager;
    Sensor stepSensor; //acelerometro
    boolean run= false;
    final float ACCELEROMETER_THRESHOLD=6f; //necesario
    float previousX = 0f; //cambia la posicion en donde se quedo el eje
    int numSteps=0; // numero de pasos hechos

    TextView textViewSteps;
    Button botonIniciar;
    Button botonParar;
    SensorEventListener sensorListener;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        sensorListener = sensorListener1;

        sensorManager=(SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor= sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        textViewSteps=findViewById(R.id.textView);
        botonIniciar=findViewById(R.id.btnIniciar);
        botonParar=findViewById(R.id.btnParar);

        botonIniciar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numSteps= 0;
                textViewSteps.setText("0");
                run = true;
            }
        });
        botonParar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                run= false;
            }
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(sensorListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        run=true;
    }
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(sensorListener);
        run=false;
    }
    private final SensorEventListener sensorListener1 = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (run){
                float x=event.values[0];
                float y=event.values[1];
                float z=event.values[2];

                float deltaX = x - previousX;
                if (deltaX > ACCELEROMETER_THRESHOLD){
                    numSteps++;
                    textViewSteps.setText(String.valueOf(numSteps));
                }

                previousX = x;
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };
}