package me.pigalala.trackexchange.trackcomponents;

import com.google.gson.JsonObject;
import me.makkuusen.timing.system.boatutils.TrackBoatUtilsSettings;

public class TrackExchangeBoatUtilsSetting implements TrackComponent {

    private final JsonObject settingsJson;

    public TrackExchangeBoatUtilsSetting(TrackBoatUtilsSettings boatUtilsSettings) {
        settingsJson = boatUtilsSettings.toJson();
    }

    public TrackExchangeBoatUtilsSetting(JsonObject settingsJson) {
        this.settingsJson = settingsJson;
    }

    public TrackBoatUtilsSettings asBoatUtilsSetting(int trackId) {
        return new TrackBoatUtilsSettings(trackId, settingsJson);
    }

    @Override
    public JsonObject asJson() {
        return settingsJson;
    }
}
