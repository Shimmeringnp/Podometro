package com.example.podometro.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitCliente {
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static Retrofit retrofit;

    public static ClimaApi getWeatherApi() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ClimaApi.class);
    }
}
