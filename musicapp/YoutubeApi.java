package com.example.musicapp;

import com.example.musicapp.model.YoutubeResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface YoutubeApi {
    @GET("search")
    Call<YoutubeResponse> searchVideos(
            @Query("part") String part,
            @Query("q") String query,
            @Query("type") String type,
            @Query("key") String apiKey,
            @Query("maxResults") int maxResults
    );
}
