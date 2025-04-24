package me.pigalala.trackexchange.file.load;

import me.pigalala.trackexchange.TrackExchange;

import java.nio.file.Files;

public final class LocalFileLoader implements TrackExchangeFileReader {

    @Override
    public byte[] read(String trackExchangeName) {
        try {
            return Files.readAllBytes(TrackExchange.getTracksPath().resolve(trackExchangeName.concat(".trackexchange")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
