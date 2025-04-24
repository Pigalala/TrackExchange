package me.pigalala.trackexchange.file.save;

import me.pigalala.trackexchange.butler.ButlerFacade;

public final class ButlerFileSaver implements TrackExchangeFileSaver {

    @Override
    public void save(String name, byte[] bytes) {
        ButlerFacade.upload(name, bytes);
    }
}
