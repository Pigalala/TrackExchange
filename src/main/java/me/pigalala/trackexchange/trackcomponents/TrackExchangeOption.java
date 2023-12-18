package me.pigalala.trackexchange.trackcomponents;

import me.makkuusen.timing.system.track.options.TrackOption;

import java.io.Serializable;

public class TrackExchangeOption implements Serializable {

    private final int id;

    public TrackExchangeOption(TrackOption trackOption) {
        id = trackOption.getId();
    }

    public TrackOption toTrackOption() {
        return TrackOption.fromID(id);
    }
}
