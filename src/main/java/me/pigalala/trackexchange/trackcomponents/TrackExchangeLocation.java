package me.pigalala.trackexchange.trackcomponents;

import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.locations.TrackLocation;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.io.Serializable;
import java.sql.SQLException;

public class TrackExchangeLocation implements Serializable {

    private final int index;
    private final SimpleLocation location;
    private final String type;

    public TrackExchangeLocation(TrackLocation trackLocation) {
        index = trackLocation.getIndex();
        location = new SimpleLocation(trackLocation.getLocation());
        type = trackLocation.getLocationType().toString();
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
}
