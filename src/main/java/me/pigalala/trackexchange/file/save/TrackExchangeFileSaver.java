package me.pigalala.trackexchange.file.save;

public interface TrackExchangeFileSaver {

    boolean save(String name, byte[] bytes);
}
