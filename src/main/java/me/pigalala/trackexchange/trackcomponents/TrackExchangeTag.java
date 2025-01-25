package me.pigalala.trackexchange.trackcomponents;

import com.google.gson.JsonObject;
import me.makkuusen.timing.system.TrackTagManager;
import me.makkuusen.timing.system.track.tags.TrackTag;

import java.util.Optional;

public class TrackExchangeTag implements TrackComponent {

    private final String value;

    public TrackExchangeTag(TrackTag tag) {
        value = tag.getValue();
    }

    public TrackExchangeTag(JsonObject tagBody) {
        value = tagBody.get("value").getAsString();
    }

    public Optional<TrackTag> toTrackTag() {
        return Optional.ofNullable(TrackTagManager.getTrackTag(value));
    }

    @Override
    public JsonObject asJson() {
        JsonObject tagBody = new JsonObject();
        tagBody.addProperty("value", value);
        return tagBody;
    }
}
