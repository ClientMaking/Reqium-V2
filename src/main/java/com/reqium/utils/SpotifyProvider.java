package com.reqium.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;

public final class SpotifyProvider {
    public record TrackInfo(String title, String artist, String state, String artPath) {}
    public static volatile String title = "Not Playing";
    public static volatile String artist = "";
    public static volatile float progress = 0.0f;
    public static volatile float duration = 0.0f;
    public static volatile float currentTime = 0.0f;
    public static volatile boolean isPlaying = false;
    public static volatile String albumArtPath = "";
    public static volatile boolean artLoaded = false;

    private static volatile boolean started = false;
    private static volatile String lastTrack = "";
    private static volatile long trackStartTime = 0L;
    private static volatile float trackDuration = 210.0f;

    private SpotifyProvider() {}

    public static synchronized void init() {
        if (started) return;
        started = true;
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    fetchSpotifyInfo();
                    Thread.sleep(500L);
                } catch (Exception ignored) {
                }
            }
        }, "Reqium-Spotify");
        thread.setDaemon(true);
        thread.start();
    }

    private static void fetchSpotifyInfo() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe", "/c",
                    "powershell -NoProfile -Command \"(Get-Process Spotify -ErrorAction SilentlyContinue | Where-Object {$_.MainWindowTitle -and $_.MainWindowTitle -ne 'Spotify' -and $_.MainWindowTitle -ne 'Spotify Premium' -and $_.MainWindowTitle -ne 'Spotify Free'} | Select-Object -First 1).MainWindowTitle\""
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            process.waitFor();

            String output = sb.toString().trim();
            if (output.contains(" - ") && !output.isEmpty()) {
                int sep = output.indexOf(" - ");
                String newArtist = output.substring(0, sep).trim();
                String newTitle = output.substring(sep + 3).trim();
                String trackKey = newArtist + " - " + newTitle;

                if (!trackKey.equals(lastTrack)) {
                    lastTrack = trackKey;
                    trackStartTime = System.currentTimeMillis();
                    artLoaded = false;
                    albumArtPath = "";
                    fetchFromiTunes(newArtist, newTitle);
                }

                artist = newArtist;
                title = newTitle;
                isPlaying = true;
                long elapsed = System.currentTimeMillis() - trackStartTime;
                currentTime = elapsed / 1000.0f;
                duration = trackDuration;
                if (duration > 0.0f) {
                    progress = Math.min(100.0f, (currentTime / duration) * 100.0f);
                }
                return;
            }

            if (output.toLowerCase().contains("advertisement") || output.equals("Spotify") || output.isEmpty()) {
                ProcessBuilder checkPb = new ProcessBuilder(
                        "cmd.exe", "/c",
                        "powershell -NoProfile -Command \"@(Get-Process Spotify -ErrorAction SilentlyContinue).Count\""
                );
                checkPb.redirectErrorStream(true);
                Process checkProcess = checkPb.start();
                String countOutput = "";
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null) countOutput = line;
                }
                checkProcess.waitFor();

                int count = 0;
                try {
                    count = Integer.parseInt(countOutput.trim());
                } catch (Exception ignored) {
                }

                if (count > 0) {
                    if (output.toLowerCase().contains("ad") || lastTrack.isEmpty()) {
                        title = "Advertisement";
                        isPlaying = true;
                    } else {
                        title = "Paused";
                        isPlaying = false;
                    }
                    artist = "";
                } else {
                    title = "Not Running";
                    artist = "";
                    isPlaying = false;
                    albumArtPath = "";
                    artLoaded = false;
                    progress = 0.0f;
                    currentTime = 0.0f;
                    duration = 0.0f;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void fetchFromiTunes(String artistName, String trackTitle) {
        Thread thread = new Thread(() -> {
            try {
                String query = URLEncoder.encode(artistName + " " + trackTitle, "UTF-8");
                String url = "https://itunes.apple.com/search?term=" + query + "&entity=song&limit=1";
                HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                if (connection.getResponseCode() == 200) {
                    StringBuilder json = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            json.append(line);
                        }
                    }

                    String body = json.toString();
                    int durationIndex = body.indexOf("\"trackTimeMillis\":");
                    if (durationIndex > 0) {
                        try {
                            int start = durationIndex + 18;
                            int end = body.indexOf(",", start);
                            if (end > start) {
                                long millis = Long.parseLong(body.substring(start, end).trim());
                                trackDuration = millis / 1000.0f;
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    int artworkIndex = body.indexOf("\"artworkUrl100\":\"");
                    if (artworkIndex > 0) {
                        int start = artworkIndex + 17;
                        int end = body.indexOf("\"", start);
                        if (end > start) {
                            String artUrl = body.substring(start, end).replace("100x100bb", "300x300bb");
                            downloadImage(artUrl);
                        }
                    }
                }
                connection.disconnect();
            } catch (Exception ignored) {
            }
        }, "Spotify-AlbumArt");
        thread.setDaemon(true);
        thread.start();
    }

    private static void downloadImage(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (connection.getResponseCode() == 200) {
                try (InputStream stream = connection.getInputStream()) {
                    BufferedImage image = ImageIO.read(stream);
                    if (image != null) {
                        File file = new File(System.getenv("TEMP"), "spotify_album_art.png");
                        ImageIO.write(image, "png", file);
                        if (file.exists() && file.length() > 0) {
                            albumArtPath = file.getAbsolutePath();
                            artLoaded = true;
                        }
                    }
                }
            }
            connection.disconnect();
        } catch (Exception ignored) {
        }
    }

    public static TrackInfo fetchNowPlaying() {
        init();
        String currentTitle = title == null || title.isBlank() ? "Not Playing" : title;
        String currentArtist = artist == null ? "" : artist;
        String state = isPlaying ? "Playing" : currentTitle;
        String artPath = albumArtPath == null ? "" : albumArtPath;
        return new TrackInfo(currentTitle, currentArtist, state, artPath);
    }

    public static String formatTime(float seconds) {
        if (seconds < 0.0f) seconds = 0.0f;
        int mins = (int) (seconds / 60.0f);
        int secs = (int) (seconds % 60.0f);
        return mins + ":" + (secs < 10 ? "0" : "") + secs;
    }
}
