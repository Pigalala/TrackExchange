package me.pigalala.trackexchange.file.load;

import me.pigalala.trackexchange.butler.ButlerFacade;

public final class ButlerFileReader implements TrackExchangeFileReader {

    @Override
    public byte[] read(String trackExchangeName) {
        return ButlerFacade.get(trackExchangeName).orElseThrow();
    }
}
