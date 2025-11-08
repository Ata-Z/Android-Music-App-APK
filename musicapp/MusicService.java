package com.example.musicapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {

    private static final String CHANNEL_ID = "MusicPlayerChannel";
    private static final int NOTIFICATION_ID = 1;

    private MediaPlayer mediaPlayer;
    private List<File> playlist;
    private int currentIndex = 0;
    private MediaSessionCompat mediaSession;
    private final IBinder binder = new MusicBinder();

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        playlist = new ArrayList<>();
        createNotificationChannel();
        setupMediaSession();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void setupMediaSession() {
        mediaSession = new MediaSessionCompat(this, "MusicService");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                play();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onSkipToNext() {
                playNext();
            }

            @Override
            public void onSkipToPrevious() {
                playPrevious();
            }
        });
        mediaSession.setActive(true);
    }

    public void setPlaylist(List<File> songs, int startIndex) {
        playlist.clear();
        playlist.addAll(songs);
        currentIndex = startIndex;
        playSongAtIndex(currentIndex);
    }

    private void playSongAtIndex(int index) {
        if (index < 0 || index >= playlist.size()) return;

        currentIndex = index;
        File song = playlist.get(currentIndex);

        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(song.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            mediaPlayer.setOnCompletionListener(mp -> playNext());

            updateNotification();
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void play() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            updateNotification();
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
        }
    }

    private void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updateNotification();
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
        }
    }

    public void playNext() {
        if (currentIndex < playlist.size() - 1) {
            playSongAtIndex(currentIndex + 1);
        } else {
            playSongAtIndex(0);
        }
    }

    public void playPrevious() {
        if (currentIndex > 0) {
            playSongAtIndex(currentIndex - 1);
        } else {
            playSongAtIndex(playlist.size() - 1);
        }
    }

    private void updatePlaybackState(int state) {
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(state, 0, 1.0f);
        mediaSession.setPlaybackState(stateBuilder.build());
    }

    private void updateNotification() {
        String songName = "No song";
        if (currentIndex >= 0 && currentIndex < playlist.size()) {
            songName = playlist.get(currentIndex).getName().replace(".mp3", "");
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Now Playing")
                .setContentText(songName)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken()))
                .addAction(android.R.drawable.ic_media_previous, "Previous",
                        createAction("PREVIOUS"))
                .addAction(mediaPlayer != null && mediaPlayer.isPlaying() ?
                                android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        "Play/Pause", createAction("PLAY_PAUSE"))
                .addAction(android.R.drawable.ic_media_next, "Next",
                        createAction("NEXT"));

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private PendingIntent createAction(String action) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "PLAY_PAUSE":
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        pause();
                    } else {
                        play();
                    }
                    break;
                case "NEXT":
                    playNext();
                    break;
                case "PREVIOUS":
                    playPrevious();
                    break;
            }
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaSession.release();
    }
}