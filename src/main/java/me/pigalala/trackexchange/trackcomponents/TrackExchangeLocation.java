package me.pigalala.trackexchange.trackcomponents;

import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.locations.TrackLocation;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.sql.SQLException;

public class TrackExchangeLocation implements TrackComponent {

    private final int index;
    private final SimpleLocation location;
    private final String type;

    public TrackExchangeLocation(TrackLocation trackLocation) {
        index = trackLocation.getIndex();
        location = new SimpleLocation(trackLocation.getLocation());
        type = trackLocation.getLocationType().toString();
    }

    public TrackExchangeLocation(JSONObject locationBody) {
        index = Integer.parseInt(String.valueOf(locationBody.get("index")));
        location = new SimpleLocation((JSONObject) locationBody.get("location"));
        type = String.valueOf(locationBody.get("type"));
    }

    public TrackLocation.Type getType() {
        return TrackLocation.Type.valueOf(type);
    }

    public TrackLocation toTrackLocation(Track track, World world, Vector offset) {
        Location location = this.location.toLocation(world).clone();
        location.setWorld(world);
        location.subtract(offset);

        try {
            return TrackDatabase.trackLocationNew(track.getId(), index, getType(), location);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public JSONObject asJson() {
        JSONObject locationBody = new JSONObject();
        locationBody.put("index", index);
        locationBody.put("location", location.asJson());
        locationBody.put("type", type);
        return locationBody;
    }
}
