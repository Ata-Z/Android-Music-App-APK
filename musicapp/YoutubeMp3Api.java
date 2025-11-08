package com.example.musicapp;

import com.example.musicapp.model.Mp3Response;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface YoutubeMp3Api {

    @GET("/dl")
    Call<Mp3Response> getMp3Link(
            @Query("id") String videoId,
            @Header("x-rapidapi-key") String apiKey,
            @Header("x-rapidapi-host") String apiHost
    );
}