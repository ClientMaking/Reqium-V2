package com.reqium.modules.client;

import com.reqium.Module;
import com.reqium.SliderSetting;
import com.reqium.utils.SpotifyProvider;

public class SpotifyPlay extends Module {
    public final SliderSetting x = new SliderSetting("X", 14.0, 0.0, 6000.0, 1.0);
    public final SliderSetting y = new SliderSetting("Y", 48.0, 0.0, 6000.0, 1.0);

    public SpotifyPlay() {
        super("SpotifyPlay", "Shows Spotify track info on a small bottom-right panel", "Client");
        addSetting(x);
        addSetting(y);
    }

    @Override
    public void onEnable() {
        SpotifyProvider.init();
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onTick() {
        SpotifyProvider.init();
    }

    public static String getSongTitle() {
        return SpotifyProvider.title == null || SpotifyProvider.title.isBlank() ? "Not Playing" : SpotifyProvider.title;
    }

    public static String getArtistName() {
        return SpotifyProvider.artist == null ? "" : SpotifyProvider.artist;
    }

    public static boolean hasCover() {
        return SpotifyProvider.artLoaded && SpotifyProvider.albumArtPath != null && !SpotifyProvider.albumArtPath.isBlank();
    }
}
