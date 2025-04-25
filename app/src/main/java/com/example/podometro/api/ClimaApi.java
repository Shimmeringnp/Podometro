package com.example.podometro.api;

import com.example.podometro.model.ClimaRespuesta;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ClimaApi {
    @GET("weather")
    Call<ClimaRespuesta> obtenerClimaPorCordenadas(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("units") String units,
            @Query("lang") String lang,
            @Query("appid") String apiKey
    );

    @GET("weather")
    Call<ClimaRespuesta> obtenerClimaPorCiudad(
            @Query("q") String city,
            @Query("units") String units,
            @Query("lang") String lang,
            @Query("appid") String apiKey
    );
}
