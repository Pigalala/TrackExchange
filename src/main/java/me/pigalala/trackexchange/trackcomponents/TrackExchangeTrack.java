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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class TrackExchangeTrack implements TrackComponent {

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

    public TrackExchangeTrack(JSONObject trackBody) {
        owner = UUID.fromString(String.valueOf(trackBody.get("owner")));
        dateCreated = Long.parseLong(String.valueOf(trackBody.get("dateCreated")));
        guiItem = String.valueOf(trackBody.get("guiItem"));
        weight = Integer.parseInt(String.valueOf(trackBody.get("weight")));
        trackType = String.valueOf(trackBody.get("trackType"));
        boatUtilsMode = Short.parseShort(String.valueOf(trackBody.get("boatUtilsMode")));

        contributors = new ArrayList<>();
        ((JSONArray) trackBody.get("contributors")).forEach(uuidRaw -> contributors.add(UUID.fromString(String.valueOf(uuidRaw))));

        regions = new ArrayList<>();
        ((JSONArray) trackBody.get("regions")).forEach(regionRaw -> regions.add(new TrackExchangeRegion((JSONObject) regionRaw)));

        locations = new ArrayList<>();
        ((JSONArray) trackBody.get("locations")).forEach(locationRaw -> locations.add(new TrackExchangeLocation((JSONObject) locationRaw)));

        tags = new ArrayList<>();
        ((JSONArray) trackBody.get("tags")).forEach(tagRaw -> tags.add(new TrackExchangeTag((JSONObject) tagRaw)));

        options = new ArrayList<>();
        ((JSONArray) trackBody.get("options")).forEach(optionRaw -> options.add(new TrackExchangeOption((JSONObject) optionRaw)));

        spawnLocation = new SimpleLocation((JSONObject) trackBody.get("spawn"));
        origin = new SimpleLocation((JSONObject) trackBody.get("origin"));
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
            track.getTrackOptions().create(option);
        });

        return track;
    }

    @Override
    public JSONObject asJson() {
        JSONObject trackBody = new JSONObject();
        trackBody.put("owner", owner.toString());
        trackBody.put("dateCreated", dateCreated);
        trackBody.put("guiItem", guiItem);
        trackBody.put("weight", weight);
        trackBody.put("trackType", trackType);
        trackBody.put("boatUtilsMode", boatUtilsMode);
        trackBody.put("spawn", spawnLocation.asJson());
        trackBody.put("origin", origin.asJson());

        JSONArray contributorsArray = new JSONArray();
        contributors.forEach(uuid -> contributorsArray.add(uuid.toString()));
        trackBody.put("contributors", contributorsArray);

        JSONArray regionsArray = new JSONArray();
        regions.forEach(region -> regionsArray.add(region.asJson()));
        trackBody.put("regions", regionsArray);

        JSONArray locationsArray = new JSONArray();
        locations.forEach(location -> locationsArray.add(location.asJson()));
        trackBody.put("locations", locationsArray);

        JSONArray tagsArray = new JSONArray();
        tags.forEach(tag -> tagsArray.add(tag.asJson()));
        trackBody.put("tags", tagsArray);

        JSONArray optionsArray = new JSONArray();
        options.forEach(option -> optionsArray.add(option.asJson()));
        trackBody.put("options", optionsArray);

        return trackBody;
    }
}
