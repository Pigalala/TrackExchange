package me.pigalala.trackexchange.trackcomponents;

import me.makkuusen.timing.system.TrackTagManager;
import me.makkuusen.timing.system.track.TrackTag;

import java.util.Optional;

public class TrackExchangeTag {

    private final String value;

    public TrackExchangeTag(TrackTag tag) {
        value = tag.getValue();
    }

    public Optional<TrackTag> toTrackTag() {
        return Optional.ofNullable(TrackTagManager.getTrackTag(value));
    }
}
