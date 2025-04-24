package me.pigalala.trackexchange.trackcomponents;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Getter
public class TrackExchangeTrack implements TrackComponent {

    @Setter
    private String displayName;
    private final UUID owner;
    private final String guiItem;
    private final int weight;
    private final String trackType;
    private final short boatUtilsMode;

    private final List<UUID> contributors;
    private final List<TrackExchangeRegion> regions;
    private final List<TrackExchangeLocation> locations;
    private final List<TrackExchangeTag> tags;
    private final List<TrackExchangeOption> options;
    private final TrackExchangeBoatUtilsSetting boatUtilsSetting;

    private final SimpleLocation spawnLocation;
    private final SimpleLocation origin;

    public TrackExchangeTrack(Track track, SimpleLocation origin) {
        displayName = track.getDisplayName();
        owner = track.getOwner().getUniqueId();
        guiItem = ApiUtilities.itemToString(track.getItem());
        weight = track.getWeight();
        trackType = track.getType().toString();
        boatUtilsMode = track.getBoatUtilsMode().getId();

        contributors = track.getContributors().stream().map(TPlayer::getUniqueId).toList();
        regions = track.getTrackRegions().getRegions().stream().map(TrackExchangeRegion::new).toList();
        locations = track.getTrackLocations().getLocations().stream().map(TrackExchangeLocation::new).toList();
        tags = track.getTrackTags().get().stream().map(TrackExchangeTag::new).toList();
        options = track.getTrackOptions().getTrackOptions().stream().map(TrackExchangeOption::new).toList();
        boatUtilsSetting = new TrackExchangeBoatUtilsSetting(track.getBoatUtilsSetting());

        spawnLocation = new SimpleLocation(track.getSpawnLocation());
        this.origin = origin;
    }

    public TrackExchangeTrack(JsonObject trackBody) {
        owner = UUID.fromString(trackBody.get("owner").getAsString());
        guiItem = trackBody.get("guiItem").getAsString();
        weight = trackBody.get("weight").getAsInt();
        trackType = trackBody.get("trackType").getAsString();
        boatUtilsMode = trackBody.get("boatUtilsMode").getAsShort();

        contributors = trackBody.get("contributors").getAsJsonArray().asList().stream()
                .map(json -> UUID.fromString(json.getAsString()))
                .toList();

        regions = trackBody.get("regions").getAsJsonArray().asList().stream()
                .map(json -> new TrackExchangeRegion(json.getAsJsonObject()))
                .toList();

        boatUtilsSetting = new TrackExchangeBoatUtilsSetting(trackBody.get("boatUtilsSetting").getAsJsonObject());

        locations = trackBody.get("locations").getAsJsonArray().asList().stream()
                .map(json -> new TrackExchangeLocation(json.getAsJsonObject()))
                .toList();

        tags = trackBody.get("tags").getAsJsonArray().asList().stream()
                .map(json -> new TrackExchangeTag(json.getAsJsonObject()))
                .toList();

        options = trackBody.get("options").getAsJsonArray().asList().stream()
                .map(json -> new TrackExchangeOption(json.getAsJsonObject()))
                .toList();

        spawnLocation = new SimpleLocation(trackBody.get("spawn").getAsJsonObject());
        origin = new SimpleLocation(trackBody.get("origin").getAsJsonObject());
    }

    public Track createTrack(Player playerPasting) throws SQLException {
        TPlayer owner = TimingSystemAPI.getTPlayer(this.owner);
        if(owner == null)
            owner = TimingSystemAPI.getTPlayer(playerPasting.getUniqueId());
        World world = playerPasting.getWorld();

        Vector offset = SimpleLocation.getOffset(origin.toLocation(world).toBlockLocation(), playerPasting.getLocation().toBlockLocation());
        Location newSpawnLocation = spawnLocation.toLocation(world).subtract(offset);

        long trackId = TimingSystem.getDatabase().createTrack(owner.getUniqueId().toString(), displayName, ApiUtilities.getTimestamp(), weight, ApiUtilities.stringToItem(guiItem), newSpawnLocation, Track.TrackType.valueOf(trackType), BoatUtilsMode.getMode(boatUtilsMode));
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
    public JsonObject asJson() {
        var trackBody = new JsonObject();
        trackBody.addProperty("owner", owner.toString());
        trackBody.addProperty("dateCreated", ApiUtilities.getTimestamp()); // Send date created for backwards compatibility
        trackBody.addProperty("guiItem", guiItem);
        trackBody.addProperty("weight", weight);
        trackBody.addProperty("trackType", trackType);
        trackBody.addProperty("boatUtilsMode", boatUtilsMode);
        trackBody.add("spawn", spawnLocation.asJson());
        trackBody.add("origin", origin.asJson());

        trackBody.addProperty("boatUtilsMode", boatUtilsMode); // For compatibility between TimingSystems
        trackBody.add("boatUtilsSetting", boatUtilsSetting.asJson());

        var contributorsArray = new JsonArray();
        contributors.stream()
                .map(UUID::toString)
                .forEach(contributorsArray::add);
        trackBody.add("contributors", contributorsArray);

        var regionsArray = new JsonArray();
        regions.stream()
                .map(TrackExchangeRegion::asJson)
                .forEach(regionsArray::add);
        trackBody.add("regions", regionsArray);

        var locationsArray = new JsonArray();
        locations.forEach(location -> locationsArray.add(location.asJson()));
        trackBody.add("locations", locationsArray);

        var tagsArray = new JsonArray();
        tags.stream()
                .map(TrackExchangeTag::asJson)
                .forEach(tagsArray::add);
        trackBody.add("tags", tagsArray);

        var optionsArray = new JsonArray();
        options.stream()
                .map(TrackExchangeOption::asJson)
                .forEach(optionsArray::add);
        trackBody.add("options", optionsArray);

        return trackBody;
    }
}
