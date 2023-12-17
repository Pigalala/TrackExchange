package me.pigalala.trackexchange.trackcomponents;

import me.makkuusen.timing.system.TrackTagManager;
import me.makkuusen.timing.system.track.tags.TrackTag;

import java.io.Serializable;
import java.util.Optional;

public class TrackExchangeTag implements Serializable {

    private final String value;

    public TrackExchangeTag(TrackTag tag) {
        value = tag.getValue();
    }

    public Optional<TrackTag> toTrackTag() {
        return Optional.ofNullable(TrackTagManager.getTrackTag(value));
    }
}
