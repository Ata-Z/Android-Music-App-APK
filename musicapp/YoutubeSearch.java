package com.example.musicapp;

import android.util.Log;

import com.example.musicapp.model.Item;
import com.example.musicapp.model.Snippet;
import com.example.musicapp.model.YoutubeResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class YoutubeSearch {

    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/";
    private static final String API_KEY = "AIzaSyB_fM9LveUGtuHbC0fxAW9EOgEryziNNYA";

    public static void search(String query, SearchCallback callback) {
        Log.d("YoutubeSearch", "Starting search for: " + query);
        long startTime = System.currentTimeMillis();

        // Add timeout configuration and force IPv4
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .dns(hostname -> {
                    // Force IPv4 addresses only because they wouldnt download
                    try {
                        java.net.InetAddress[] addresses = java.net.InetAddress.getAllByName(hostname);
                        List<java.net.InetAddress> ipv4Addresses = new ArrayList<>();
                        for (java.net.InetAddress addr : addresses) {
                            if (addr instanceof java.net.Inet4Address) {
                                ipv4Addresses.add(addr);
                            }
                        }
                        return ipv4Addresses.isEmpty() ?
                                java.util.Arrays.asList(addresses) : ipv4Addresses;
                    } catch (Exception e) {
                        return java.util.Collections.emptyList();
                    }
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        YoutubeApi api = retrofit.create(YoutubeApi.class);

        Call<YoutubeResponse> call = api.searchVideos(
                "snippet", query, "video", API_KEY, 5
        );

        Log.d("YoutubeSearch", "Making API call...");

        call.enqueue(new Callback<YoutubeResponse>() {
            @Override
            public void onResponse(Call<YoutubeResponse> call, Response<YoutubeResponse> response) {
                long elapsed = System.currentTimeMillis() - startTime;
                Log.d("YoutubeSearch", "Got response in " + elapsed + "ms");

                if (!response.isSuccessful()) {
                    Log.e("YoutubeSearch", "Error code: " + response.code());
                    callback.onError("Code: " + response.code());
                    return;
                }

                YoutubeResponse body = response.body();
                if (body == null) {
                    Log.e("YoutubeSearch", "Empty response body");
                    callback.onError("Empty response");
                    return;
                }

                Log.d("YoutubeSearch", "Processing " + body.items.size() + " items");
                List<String[]> results = new ArrayList<>();
                for (Item item : body.items) {
                    Snippet s = item.snippet;
                    String[] data = {
                            item.id.videoId,
                            s.title,
                            s.channelTitle,
                            s.thumbnails.defaultThumbnail.url
                    };
                    results.add(data);
                }

                Log.d("YoutubeSearch", "Calling success callback");
                callback.onSuccess(results);
            }

            @Override
            public void onFailure(Call<YoutubeResponse> call, Throwable t) {
                long elapsed = System.currentTimeMillis() - startTime;
                Log.e("YoutubeSearch", "Request failed after " + elapsed + "ms: " + t.getMessage());
                callback.onError(t.getMessage());
            }
        });
    }

    public interface SearchCallback {
        void onSuccess(List<String[]> results);
        void onError(String errorMsg);
    }
}