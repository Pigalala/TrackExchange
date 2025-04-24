package me.pigalala.trackexchange.file.save;

import me.pigalala.trackexchange.TrackExchange;

import java.nio.file.Files;
import java.nio.file.Path;

public final class LocalFileSaver implements TrackExchangeFileSaver {

    @Override
    public void save(String name, byte[] bytes) {
        Path dir = TrackExchange.getTracksPath().resolve(name);
        try {
            Files.createDirectory(dir);
        } catch (Exception e) {
            TrackExchange.instance.getSLF4JLogger().error("Failed to save trackexchange locally", e);
            return;
        }
    }
}
