package me.pigalala.trackexchange.trackcomponents;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.idb.DB;
import me.makkuusen.timing.system.idb.DbRow;
import me.makkuusen.timing.system.track.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.Serial;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class TrackExchangeTrack implements Serializable {
    @Serial
    private static final long serialVersionUID = 7656876361793249411L;

    @Setter
    private String displayName;
    private final UUID owner;
    private final long dateCreated;
    private final String guiItem;
    private final int weight;
    private final String trackType;
    private final String trackMode;
    private final short boatUtilsMode;

    private final List<UUID> contributors;
    private final List<TrackExchangeRegion> regions;
    private final List<TrackExchangeLocation> locations;
    private final List<TrackExchangeTag> tags;
    private final List<Character> options;

    private final SimpleLocation spawnLocation;
    private final SimpleLocation origin;

    public TrackExchangeTrack(Track track, SimpleLocation origin) {
        displayName = track.getDisplayName();
        owner = track.getOwner().getUniqueId();
        dateCreated = track.getDateCreated();
        guiItem = ApiUtilities.itemToString(track.getGuiItem());
        weight = track.getWeight();
        trackType = track.getType().toString();
        trackMode = track.getMode().toString();
        boatUtilsMode = track.getBoatUtilsMode().getId();

        contributors = track.getContributors().stream().map(TPlayer::getUniqueId).toList();
        regions = track.getRegions().stream().map(TrackExchangeRegion::new).toList();
        locations = track.getTrackLocations().stream().map(TrackExchangeLocation::new).toList();
        tags = track.getTags().stream().map(TrackExchangeTag::new).toList();

        options = new ArrayList<>();
        for (char option : track.getOptions()) {
            options.add(option);
        }

        spawnLocation = new SimpleLocation(track.getSpawnLocation());
        this.origin = origin;
    }

    public Track createTrack(Player playerPasting) throws SQLException {
        TPlayer owner = TimingSystemAPI.getTPlayer(this.owner);
        if(owner == null)
            owner = TimingSystemAPI.getTPlayer(playerPasting.getUniqueId());
        World world = playerPasting.getWorld();

        Vector offset = SimpleLocation.getOffset(origin.toLocation(world).toBlockLocation(), playerPasting.getLocation().toBlockLocation());
        Location newSpawnLocation = spawnLocation.toLocation(world).subtract(offset);

        long trackId = DB.executeInsert("INSERT INTO `ts_tracks` " +
                "(`uuid`, `name`, `dateCreated`, `weight`, `guiItem`, `spawn`, `leaderboard`, `type`, `mode`, `toggleOpen`, `options`, `boatUtilsMode`, `isRemoved`) " +
                "VALUES('" + owner.getUniqueId() + "', " + Database.sqlString(displayName) + ", " + dateCreated + ", " + weight + ", " + Database.sqlString(guiItem) + ", '" + ApiUtilities.locationToString(newSpawnLocation) + "', '" + ApiUtilities.locationToString(newSpawnLocation) + "', " + Database.sqlString(trackType) + "," + Database.sqlString(trackMode) + ", 0, NULL, " + boatUtilsMode + " , 0);");
        DbRow dbRow = DB.getFirstRow("SELECT * FROM `ts_tracks` WHERE `id` = " + trackId + ";");
        Track track = new Track(dbRow);
        TrackDatabase.getTracks().add(track);

        regions.stream().map(region -> region.toTrackRegion(track, world, offset)).forEach(trackRegion -> {
            track.addRegion(trackRegion);
            if(trackRegion.getRegionType() == TrackRegion.RegionType.START)
                TrackDatabase.addTrackRegion(trackRegion);
        });

        locations.stream().map(loc -> loc.toTrackLocation(track, world, offset)).forEach(location -> {
            track.addTrackLocation(location);
            if(location instanceof TrackLeaderboard leaderboard)
                leaderboard.createOrUpdateHologram();
        });

        tags.stream().map(TrackExchangeTag::toTrackTag).forEach(_trackTag -> {
            if(_trackTag.isEmpty())
                return;
            track.createTag(_trackTag.get());
        });

        contributors.forEach(uuid -> {
            TPlayer contributor = Database.getPlayer(uuid);
            if(contributor == null)
                return;
            track.addContributor(contributor);
        });

        String newOptions = "";
        for(char c : options)
            newOptions = newOptions.concat(String.valueOf(c));
        track.setOptions(newOptions);

        return track;
    }
}
