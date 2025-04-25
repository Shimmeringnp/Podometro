package com.example.podometro;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;
import android.Manifest;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.podometro.api.ClimaApi;
import com.example.podometro.api.RetrofitCliente;
import com.example.podometro.model.ClimaRespuesta;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private FusedLocationProviderClient fusedLocationClient;
    private final String API_KEY = "e5e5d93f2c643ed4d7f6914a2263a8d2";
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
    TextView textViewCiudad;
    Button botonConsultar;
    ProgressBar progressBar;
    int idClima=0;
    String descripcionClima;
    double temperatura;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            getLocationAndWeather();
        }

        sensorListener = sensorListener1;

        sensorManager=(SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor= sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        textViewSteps=findViewById(R.id.textView);
        botonIniciar=findViewById(R.id.btnIniciar);
        botonParar=findViewById(R.id.btnParar);
        textViewCiudad = findViewById(R.id.textViewCiudad);
        botonConsultar = findViewById(R.id.buttonConsultarCiudad);
        progressBar = findViewById(R.id.loader);

        botonIniciar.setOnClickListener(v -> {
            validarClima();
        });
        botonParar.setOnClickListener(v -> run= false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        botonConsultar.setOnClickListener(view -> {
            mostrarBuscador();
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

    private void mostrarLoader() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void ocultarLoader() {
        progressBar.setVisibility(View.GONE);
    }

    private void mostrarBuscador() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ingresa ciudad");

        final EditText input = new EditText(this);
        input.setHint("Ciudad");
        builder.setView(input);

        builder.setPositiveButton("Aceptar", (dialog, which) -> {
            String ciudad = input.getText().toString();
            consultarClimaPorCiudad(ciudad);
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void getLocationAndWeather() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        consultarClimaLocal(lat, lon);
                    } else {
                        Toast.makeText(this,"No se pudo obtener ubicación.", Toast.LENGTH_SHORT ).show();
                    }
                });
    }


    //desde aquí se manda a llamar el API
    private void consultarClimaLocal(double lat, double lon) {
        mostrarLoader();
        ClimaApi api = RetrofitCliente.getWeatherApi();
        //REQUEST
        Call<ClimaRespuesta> call = api.obtenerClimaPorCordenadas(
                lat,
                lon,
                "metric",
                "es",
                API_KEY
        );

        call.enqueue(new Callback<>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(Call<ClimaRespuesta> call, Response<ClimaRespuesta> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ocultarLoader();

                    ClimaRespuesta data = response.body();
                    String ciudad = data.name;
                    temperatura = data.main.temp;
                    String clima = data.weather.get(0).description;
                    idClima = data.weather.get(0).id;
                    descripcionClima = clima;

                    textViewCiudad.setText("Ciudad: " + ciudad + " | " + temperatura + "°C | " + clima + " | ID: " + data.weather.get(0).id);
                    Log.d("CLIMA", "Ciudad: " + ciudad + " | " + temperatura + "°C | " + clima + " | ID: " + data.weather.get(0).id);

                } else {
                    ocultarLoader();
                    textViewCiudad.setText("Respuesta no exitosa del api.");
                    Log.e("CLIMA", "Respuesta no exitosa.");
                }
            }

            @Override
            public void onFailure(Call<ClimaRespuesta> call, Throwable t) {
                ocultarLoader();
                textViewCiudad.setText("Fallo de red: " + t.getMessage());
                Log.e("CLIMA", "Fallo de red: " + t.getMessage());
            }
        });
    }

    private void consultarClimaPorCiudad(String ciudad) {
        mostrarLoader();
        ClimaApi api = RetrofitCliente.getWeatherApi();
        Call<ClimaRespuesta> call = api.obtenerClimaPorCiudad(
                ciudad,
                "metric",
                "es",
                API_KEY
        );

        call.enqueue(new Callback<>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(Call<ClimaRespuesta> call, Response<ClimaRespuesta> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ocultarLoader();
                    ClimaRespuesta data = response.body();
                    String ciudad = data.name;
                    temperatura = data.main.temp;
                    String clima = data.weather.get(0).description;
                    textViewCiudad.setText("Ciudad: " + ciudad + " | " + temperatura + "°C | " + clima + " | ID: " + data.weather.get(0).id);
                    Log.d("CLIMA", "Ciudad: " + ciudad + " | " + temperatura + "°C | " + clima + " | ID: " + data.weather.get(0).id);
                    Gson gson = new Gson();
                    String json = gson.toJson(response.body());
                    Log.d("JSON_RESPONSE", json);
                    idClima = data.weather.get(0).id;
                    descripcionClima = clima;
                } else {
                    textViewCiudad.setText("Respuesta no exitosa del api.");
                    Log.e("CLIMA", "Respuesta no exitosa.");
                }
            }

            @Override
            public void onFailure(Call<ClimaRespuesta> call, Throwable t) {
                ocultarLoader();
                textViewCiudad.setText("Fallo de red: " + t.getMessage());
                Log.e("CLIMA", "Fallo de red: " + t.getMessage());
            }
        });
    }

    public void validarClima(){
        if (idClima >= 800 && temperatura > 20 ){
            mostrarAlerta(true);
        } else if (idClima >= 200 && idClima <= 781){
            mostrarAlerta(false);
        }  else {
            mostrarAlerta(false);
        }
    }

    public void mostrarAlerta(boolean puedeSalir) {
        if (puedeSalir){
            new AlertDialog.Builder(this)
                    .setTitle("Clima")
                    .setMessage("El clima es bueno para salir a caminar, hoy es un día de: " + descripcionClima)
                    .setPositiveButton("Aceptar", (dialogInterface, i) -> {
                        numSteps= 0;
                        textViewSteps.setText("0");
                        run = true;
                    })
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Clima")
                    .setMessage("El clima es malo para salir a caminar, hoy es un día de: " + descripcionClima + " y la temperatura es: " + temperatura)
                    .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    })
                    .show();
        }

    }

    // Solicitud de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocationAndWeather();
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
        }
    }
}