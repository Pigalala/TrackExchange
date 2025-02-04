package me.pigalala.trackexchange.trackcomponents;

import com.google.gson.JsonObject;
import me.makkuusen.timing.system.track.options.TrackOption;

public class TrackExchangeOption implements TrackComponent {

    private final int id;

    public TrackExchangeOption(TrackOption trackOption) {
        id = trackOption.getId();
    }

    public TrackExchangeOption(JsonObject optionBody) {
        id = optionBody.get("id").getAsInt();
    }

    public TrackOption toTrackOption() {
        return TrackOption.fromID(id);
    }

    public JsonObject asJson() {
        var optionBody = new JsonObject();
        optionBody.addProperty("id", id);
        return optionBody;
    }
}
