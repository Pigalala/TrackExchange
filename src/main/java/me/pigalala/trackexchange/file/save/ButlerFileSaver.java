package me.pigalala.trackexchange.file.save;

import me.pigalala.trackexchange.butler.ButlerFacade;

public final class ButlerFileSaver implements TrackExchangeFileSaver {

    @Override
    public boolean save(String name, byte[] bytes) {
        return ButlerFacade.upload(name, bytes);
    }
}
