package me.pigalala.trackexchange.file.save;

import me.pigalala.trackexchange.TrackExchange;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class LocalFileSaver implements TrackExchangeFileSaver {

    @Override
    public void save(String name, byte[] bytes) {
        Path trackExchangePath = TrackExchange.getTracksPath().resolve(name.concat(".trackexchange"));
        try {
            Files.write(trackExchangePath, bytes, StandardOpenOption.CREATE);
        } catch (Exception e) {
            TrackExchange.instance.getSLF4JLogger().error("Failed to save trackexchange locally", e);
        }
    }
}
