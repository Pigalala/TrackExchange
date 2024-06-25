package me.pigalala.trackexchange.trackcomponents;

import me.makkuusen.timing.system.track.options.TrackOption;
import org.json.simple.JSONObject;

public class TrackExchangeOption implements TrackComponent {

    private final int id;

    public TrackExchangeOption(TrackOption trackOption) {
        id = trackOption.getId();
    }

    public TrackExchangeOption(JSONObject optionBody) {
        id = Integer.parseInt(String.valueOf(optionBody.get("id")));
    }

    public TrackOption toTrackOption() {
        return TrackOption.fromID(id);
    }

    public JSONObject asJson() {
        JSONObject optionBody = new JSONObject();
        optionBody.put("id", id);
        return optionBody;
    }
}
