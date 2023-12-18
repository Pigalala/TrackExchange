package me.pigalala.trackexchange.trackcomponents;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.idb.DB;
import me.makkuusen.timing.system.idb.DbRow;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.locations.TrackLeaderboard;
import me.makkuusen.timing.system.track.regions.TrackRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.Serial;
import java.io.Serializable;
import java.sql.SQLException;
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
    private final short boatUtilsMode;

    private final List<UUID> contributors;
    private final List<TrackExchangeRegion> regions;
    private final List<TrackExchangeLocation> locations;
    private final List<TrackExchangeTag> tags;
    private final List<TrackExchangeOption> options;

    private final SimpleLocation spawnLocation;
    private final SimpleLocation origin;

    public TrackExchangeTrack(Track track, SimpleLocation origin) {
        displayName = track.getDisplayName();
        owner = track.getOwner().getUniqueId();
        dateCreated = track.getDateCreated();
        guiItem = ApiUtilities.itemToString(track.getItem());
        weight = track.getWeight();
        trackType = track.getType().toString();
        boatUtilsMode = track.getBoatUtilsMode().getId();

        contributors = track.getContributors().stream().map(TPlayer::getUniqueId).toList();
        regions = track.getTrackRegions().getRegions().stream().map(TrackExchangeRegion::new).toList();
        locations = track.getTrackLocations().getLocations().stream().map(TrackExchangeLocation::new).toList();
        tags = track.getTrackTags().get().stream().map(TrackExchangeTag::new).toList();
        options = track.getTrackOptions().getTrackOptions().stream().map(TrackExchangeOption::new).toList();

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

        var trackId = TimingSystem.getTrackDatabase().createTrack(owner.getUniqueId().toString(), displayName, dateCreated, weight, ApiUtilities.stringToItem(guiItem), newSpawnLocation, Track.TrackType.valueOf(trackType), BoatUtilsMode.getMode(boatUtilsMode));
        DbRow dbRow = DB.getFirstRow("SELECT * FROM `ts_tracks` WHERE `id` = " + trackId + ";");
        Track track = new Track(dbRow);
        TrackDatabase.tracks.add(track);

        regions.stream().map(region -> region.toTrackRegion(track, world, offset)).forEach(trackRegion -> {
            track.getTrackRegions().add(trackRegion);
            if(trackRegion.getRegionType() == TrackRegion.RegionType.START)
                TrackDatabase.addTrackRegion(trackRegion);
        });

        locations.stream().map(loc -> loc.toTrackLocation(track, world, offset)).forEach(location -> {
            track.getTrackLocations().add(location);
            if(location instanceof TrackLeaderboard leaderboard)
                leaderboard.createOrUpdateHologram();
        });

        tags.stream().map(TrackExchangeTag::toTrackTag).forEach(_trackTag -> {
            if(_trackTag.isEmpty())
                return;
            track.getTrackTags().create(_trackTag.get());
        });

        contributors.forEach(uuid -> {
            TPlayer contributor = TSDatabase.getPlayer(uuid);
            if(contributor == null)
                return;
            track.addContributor(contributor);
        });

        options.stream().map(TrackExchangeOption::toTrackOption).forEach(option -> {
            track.getTrackOptions().add(option);
        });

        return track;
    }
}
