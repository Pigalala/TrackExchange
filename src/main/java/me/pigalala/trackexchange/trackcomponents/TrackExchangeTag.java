package me.pigalala.trackexchange.trackcomponents;

import me.makkuusen.timing.system.TrackTagManager;
import me.makkuusen.timing.system.track.tags.TrackTag;
import org.json.simple.JSONObject;

import java.util.Optional;

public class TrackExchangeTag implements TrackComponent {

    private final String value;

    public TrackExchangeTag(TrackTag tag) {
        value = tag.getValue();
    }

    public TrackExchangeTag(JSONObject tagBody) {
        value = String.valueOf(tagBody.get("value"));
    }

    public Optional<TrackTag> toTrackTag() {
        return Optional.ofNullable(TrackTagManager.getTrackTag(value));
    }

    @Override
    public JSONObject asJson() {
        JSONObject tagBody = new JSONObject();
        tagBody.put("value", value);
        return tagBody;
    }
}
