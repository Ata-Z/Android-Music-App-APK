package com.example.musicapp;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.example.musicapp.model.Mp3Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Mp3Downloader {

    private static final String BASE_URL = "https://youtube-mp36.p.rapidapi.com/";
    private static final String RAPIDAPI_KEY = "The_Key_mwuahaha";
    private static final String RAPIDAPI_HOST = "youtube-mp36.p.rapidapi.com";

    public interface DownloadCallback {
        void onProgress(String message);
        void onSuccess(String filePath);
        void onError(String error);
    }

    public static void downloadMp3(Context context, String videoId, String title, DownloadCallback callback) {
        Log.d("Mp3Downloader", "Starting download for videoId: " + videoId);
        callback.onProgress("Getting download link...");

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        YoutubeMp3Api api = retrofit.create(YoutubeMp3Api.class);
        Call<Mp3Response> call = api.getMp3Link(videoId, RAPIDAPI_KEY, RAPIDAPI_HOST);

        call.enqueue(new Callback<Mp3Response>() {
            @Override
            public void onResponse(Call<Mp3Response> call, Response<Mp3Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Mp3Response mp3Response = response.body();

                    if ("ok".equals(mp3Response.status) && mp3Response.link != null) {
                        Log.d("Mp3Downloader", "Got MP3 link: " + mp3Response.link);
                        callback.onProgress("Downloading file...");

                        // Download the actual MP3 file
                        downloadFile(context, mp3Response.link, title, callback);
                    } else {
                        callback.onError("Failed: " + mp3Response.msg);
                    }
                } else {
                    Log.e("Mp3Downloader", "API Error: " + response.code());
                    callback.onError("API error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Mp3Response> call, Throwable t) {
                Log.e("Mp3Downloader", "Request failed: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    private static void downloadFile(Context context, String downloadUrl, String title, DownloadCallback callback) {
        new Thread(() -> {
            try {
                // Create music directory
                File musicDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MUSIC), "MusicApp");

                if (!musicDir.exists()) {
                    musicDir.mkdirs();
                }

                // Clean filename - REMOVES THESE CHARACTERS BECAUSE IT KEPT BREAKING IDK
                String filename = title.replaceAll("[^a-zA-Z0-9.-]", "_") + ".mp3";
                File outputFile = new File(musicDir, filename);

                Log.d("Mp3Downloader", "Downloading to: " + outputFile.getAbsolutePath());

                // Download the file
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(downloadUrl).build();
                okhttp3.Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    InputStream inputStream = response.body().byteStream();
                    FileOutputStream outputStream = new FileOutputStream(outputFile);

                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    outputStream.close();
                    inputStream.close();

                    Log.d("Mp3Downloader", "Download complete!");
                    callback.onSuccess(outputFile.getAbsolutePath());
                } else {
                    callback.onError("Download failed: " + response.code());
                }

            } catch (Exception e) {
                Log.e("Mp3Downloader", "Download error: " + e.getMessage());
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
}