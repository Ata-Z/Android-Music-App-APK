package com.example.musicapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadedSongsAdapter extends RecyclerView.Adapter<DownloadedSongsAdapter.SongViewHolder> {

    private List<File> songFiles;
    private Context context;
    private OnPlayClickListener playListener;

    public interface OnPlayClickListener {
        void onPlayClick(File song, int position);
    }

    public DownloadedSongsAdapter(Context context, OnPlayClickListener listener) {
        this.context = context;
        this.songFiles = new ArrayList<>();
        this.playListener = listener;
    }

    public void setSongs(List<File> files) {
        songFiles.clear();
        songFiles.addAll(files);
        notifyDataSetChanged();
    }

    public List<File> getAllSongs() {
        return new ArrayList<>(songFiles);
    }

    @Override
    public int getItemCount() {
        return songFiles.size();
    }

    @Override
    public SongViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_downloaded_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SongViewHolder holder, int position) {
        File songFile = songFiles.get(position);

        String filename = songFile.getName().replace(".mp3", "").replace("_", " ");
        holder.titleText.setText(filename);
        holder.pathText.setText(songFile.getAbsolutePath());

        holder.playButton.setOnClickListener(v -> {
            if (playListener != null) {
                playListener.onPlayClick(songFile, position);
            }
        });
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, pathText;
        Button playButton;

        public SongViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.songTitle);
            pathText = itemView.findViewById(R.id.songPath);
            playButton = itemView.findViewById(R.id.playButton);
        }
    }
}