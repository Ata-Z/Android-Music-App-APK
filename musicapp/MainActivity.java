package com.example.musicapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.example.musicapp.YoutubeSearch.SearchCallback;

public class MainActivity extends AppCompatActivity {
    private SearchView searchView;
    private RecyclerView searchResultsRecycler, downloadedSongsRecycler;
    private SongAdapter searchAdapter;
    private DownloadedSongsAdapter downloadedAdapter;
    private View homeScreen, searchScreen;
    private Button backButton, shuffleButton;

    // Music service
    private MusicService musicService;
    private boolean serviceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            serviceBound = true;
            Log.d("MainActivity", "Music service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find views
        homeScreen = findViewById(R.id.homeScreen);
        searchScreen = findViewById(R.id.searchScreen);
        searchView = findViewById(R.id.SearchBar);
        backButton = findViewById(R.id.backButton);
        shuffleButton = findViewById(R.id.shuffleButton);
        searchResultsRecycler = findViewById(R.id.recyclerView);
        downloadedSongsRecycler = findViewById(R.id.downloadedSongsRecycler);

        // Setup search results adapter
        searchAdapter = new SongAdapter();
        searchResultsRecycler.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecycler.setAdapter(searchAdapter);

        // Setup downloaded songs adapter with play click listener
        downloadedAdapter = new DownloadedSongsAdapter(this, new DownloadedSongsAdapter.OnPlayClickListener() {
            @Override
            public void onPlayClick(File song, int position) {
                playSong(song, position);
            }
        });
        downloadedSongsRecycler.setLayoutManager(new LinearLayoutManager(this));
        downloadedSongsRecycler.setAdapter(downloadedAdapter);

        loadDownloadedSongs();

        // Bind to music service
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Search bar click - show search screen
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showSearchScreen();
            }
        });

        // Search functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d("MainActivity", "Search submitted: " + query);
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        // Back button
        backButton.setOnClickListener(v -> {
            showHomeScreen();
            searchView.setQuery("", false);
            searchView.clearFocus();
        });

        // Shuffle button
        shuffleButton.setOnClickListener(v -> {
            shuffleAndPlay();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDownloadedSongs();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    private void showHomeScreen() {
        homeScreen.setVisibility(View.VISIBLE);
        searchScreen.setVisibility(View.GONE);
    }

    private void showSearchScreen() {
        homeScreen.setVisibility(View.GONE);
        searchScreen.setVisibility(View.VISIBLE);
    }

    private void performSearch(String query) {
        long startTime = System.currentTimeMillis();

        YoutubeSearch.search(query, new SearchCallback() {
            @Override
            public void onSuccess(List<String[]> results) {
                long elapsed = System.currentTimeMillis() - startTime;
                Log.d("MainActivity", "Got results in " + elapsed + "ms");

                runOnUiThread(() -> {
                    searchAdapter.setSongs(results);
                });
            }

            @Override
            public void onError(String errorMsg) {
                Log.e("MainActivity", "Search failed: " + errorMsg);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Search failed: " + errorMsg,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadDownloadedSongs() {
        File musicDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), "MusicApp");

        List<File> mp3Files = new ArrayList<>();

        if (musicDir.exists() && musicDir.isDirectory()) {
            File[] files = musicDir.listFiles((dir, name) -> name.endsWith(".mp3"));
            if (files != null) {
                mp3Files.addAll(Arrays.asList(files));
            }
        }

        downloadedAdapter.setSongs(mp3Files);
        Log.d("MainActivity", "Loaded " + mp3Files.size() + " songs");
    }

    private void playSong(File song, int position) {
        if (!serviceBound) {
            Toast.makeText(this, "Music service not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        List<File> allSongs = downloadedAdapter.getAllSongs();

        // Start service and play from this position
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);

        musicService.setPlaylist(allSongs, position);

        Toast.makeText(this, "Playing: " + song.getName(), Toast.LENGTH_SHORT).show();
    }

    private void shuffleAndPlay() {
        List<File> songs = downloadedAdapter.getAllSongs();

        if (songs.isEmpty()) {
            Toast.makeText(this, "No songs downloaded yet!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!serviceBound) {
            Toast.makeText(this, "Music service not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        // Shuffle the songs
        List<File> shuffled = new ArrayList<>(songs);
        Collections.shuffle(shuffled);

        // Start service and play shuffled playlist
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);

        musicService.setPlaylist(shuffled, 0);

        Toast.makeText(this, "Shuffling " + songs.size() + " songs",
                Toast.LENGTH_SHORT).show();
    }
}