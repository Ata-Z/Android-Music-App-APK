package com.example.musicapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongHolder> {

    private List<String[]> songList; // This holds the YouTube results :D

    public SongAdapter() {
        this.songList = new ArrayList<>(); // No songs yet so its empty
    }

    // Call this when you get search results
    public void setSongs(List<String[]> newSongs) {
        songList.clear();               // Remove old songs
        songList.addAll(newSongs);      // Add new songs
        notifyDataSetChanged();         // Refresh the display
    }

    // How many songs do we have?
    @Override
    public int getItemCount() {
        return songList.size();
    }

    // Create a new row view
    @Override
    public SongHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.songs, parent, false);
        return new SongHolder(view);
    }

    // Fill in data for one row
    @Override
    public void onBindViewHolder(SongHolder holder, int position) {
        String[] song = songList.get(position);

        // song[0] = videoId, song[1] = title, song[2] = channel, song[3] = thumbnail
        holder.titleText.setText(song[1]);
        holder.channelText.setText(song[2]);

        // Load the thumbnail image - ADD THESE LINES
        Glide.with(holder.itemView.getContext())
                .load(song[3])
                .into(holder.thumbnailImage);

        // When download button is clicked
        holder.downloadButton.setOnClickListener(v -> {
            holder.downloadButton.setEnabled(false);
            holder.downloadButton.setText("...");

            Mp3Downloader.downloadMp3(v.getContext(), song[0], song[1], new Mp3Downloader.DownloadCallback() {
                @Override
                public void onProgress(String message) {
                    holder.itemView.post(() -> {
                        holder.downloadButton.setText(message);
                    });
                }

                @Override
                public void onSuccess(String filePath) {
                    holder.itemView.post(() -> {
                        holder.downloadButton.setText("✓ Done");
                        holder.downloadButton.setEnabled(true);
                        Toast.makeText(v.getContext(), "Saved: " + song[1], Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    holder.itemView.post(() -> {
                        holder.downloadButton.setText("Download");
                        holder.downloadButton.setEnabled(true);
                        Toast.makeText(v.getContext(), "Failed: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    // This represents one row
    public static class SongHolder extends RecyclerView.ViewHolder {
        TextView titleText, channelText;
        ImageView thumbnailImage;
        Button downloadButton;

        public SongHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.title);
            channelText = itemView.findViewById(R.id.channel);
            thumbnailImage = itemView.findViewById(R.id.thumbnail);
            downloadButton = itemView.findViewById(R.id.Download_button);
        }
    }
}