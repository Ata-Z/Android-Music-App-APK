package com.example.musicapp.model;

public class Mp3Response {
    public String status;
    public String link;  // The MP3 download URL
    public String title;
    public String msg;

    // API returns something like:
    // {"status":"ok", "link":"https://...", "title":"Song Name"}
}