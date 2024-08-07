package me.pigalala.trackexchange.trackcomponents;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.regions.TrackPolyRegion;
import me.makkuusen.timing.system.track.regions.TrackRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TrackExchangeRegion implements TrackComponent {

    private final int regionIndex;
    private final String regionType;
    private final String regionShape;
    private final SimpleLocation spawnLocation;
    private final SimpleLocation minP;
    private final SimpleLocation maxP;
    private final List<String> points;

    public TrackExchangeRegion(TrackRegion trackRegion) {
        regionIndex = trackRegion.getRegionIndex();
        regionType = trackRegion.getRegionType().toString();
        regionShape = trackRegion.getShape().toString();
        spawnLocation = new SimpleLocation(trackRegion.getSpawnLocation());
        minP = new SimpleLocation(trackRegion.getMinP());
        maxP = new SimpleLocation(trackRegion.getMaxP());

        if(trackRegion instanceof TrackPolyRegion polyRegion)
            points = polyRegion.getPolygonal2DRegion().getPoints().stream().map(vec -> vec.getX() + " " + vec.getZ()).toList();
        else
            points = new ArrayList<>();
    }

    public TrackExchangeRegion(JSONObject regionBody) {
        regionIndex = Integer.parseInt(String.valueOf(regionBody.get("index")));
        regionType = String.valueOf(regionBody.get("type"));
        regionShape = String.valueOf(regionBody.get("shape"));
        spawnLocation = new SimpleLocation((JSONObject) regionBody.get("spawn"));
        minP = new SimpleLocation((JSONObject) regionBody.get("minP"));
        maxP = new SimpleLocation((JSONObject) regionBody.get("maxP"));

        points = new ArrayList<>();
        Object pointsArrayRaw = regionBody.get("points");
        if (pointsArrayRaw != null) {
            ((JSONArray) pointsArrayRaw).forEach(pont -> points.add(String.valueOf(pont)));
        }
    }

    public TrackRegion.RegionType getRegionType() {
        return TrackRegion.RegionType.valueOf(regionType);
    }

    public TrackRegion.RegionShape getRegionShape() {
        return TrackRegion.RegionShape.valueOf(regionShape);
    }

    public Location getSpawnLocation(World world) {
        return spawnLocation.toLocation(world);
    }

    public Location getMinP(World world) {
        return minP.toLocation(world);
    }

    public Location getMaxP(World world) {
        return maxP.toLocation(world);
    }

    public List<BlockVector2> getPoints() {
        return points.stream().map(s -> {
            String[] vectorString = s.split(" ");
            return BlockVector2.at(Integer.parseInt(vectorString[0]), Integer.parseInt(vectorString[1]));
        }).toList();
    }

    public TrackRegion toTrackRegion(Track track, World world, Vector offset) {
        Location origin = getSpawnLocation(world).subtract(offset);
        Location minP = getMinP(world).subtract(offset);
        Location maxP = getMaxP(world).subtract(offset);

        try {
            if (getRegionShape() == TrackRegion.RegionShape.POLY) {
                List<BlockVector2> points = new ArrayList<>();
                getPoints().forEach(point -> {
                    points.add(point.subtract(offset.getBlockX(), offset.getBlockZ()));
                });
                return TrackDatabase.trackRegionNew(new Polygonal2DRegion(BukkitAdapter.adapt(world), points, minP.getBlockY(), maxP.getBlockY()), track.getId(), regionIndex, getRegionType(), origin);
            }
            return TrackDatabase.trackRegionNew(new CuboidRegion(BukkitAdapter.adapt(world), BlockVector3.at(minP.getBlockX(), minP.getBlockY(), minP.getBlockZ()), BlockVector3.at(maxP.getBlockX(), maxP.getBlockY(), maxP.getBlockZ())), track.getId(), regionIndex, getRegionType(), origin);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public JSONObject asJson() {
        JSONObject regionBody = new JSONObject();
        regionBody.put("index", regionIndex);
        regionBody.put("type", regionType);
        regionBody.put("shape", regionShape);
        regionBody.put("spawn", spawnLocation.asJson());
        regionBody.put("minP", minP.asJson());
        regionBody.put("maxP", maxP.asJson());

        if (!points.isEmpty()) {
            JSONArray pointsArray = new JSONArray();
            pointsArray.addAll(points);
            regionBody.put("points", pointsArray);
        }

        return regionBody;
    }
}
