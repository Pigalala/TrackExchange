package me.pigalala.trackexchange.trackcomponents;

import com.google.gson.JsonObject;
import me.makkuusen.timing.system.track.options.TrackOption;

import java.util.Optional;

public class TrackExchangeOption implements TrackComponent {

    private final int id;

    public TrackExchangeOption(TrackOption trackOption) {
        id = trackOption.getId();
    }

    public TrackExchangeOption(JsonObject optionBody) {
        id = optionBody.get("id").getAsInt();
    }

    public Optional<TrackOption> toTrackOption() {
        try {
            return Optional.of(TrackOption.fromID(id));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public JsonObject asJson() {
        var optionBody = new JsonObject();
        optionBody.addProperty("id", id);
        return optionBody;
    }
}
