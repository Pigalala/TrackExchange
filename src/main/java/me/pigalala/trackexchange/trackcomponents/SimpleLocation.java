package me.pigalala.trackexchange.trackcomponents;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.json.simple.JSONObject;

import java.io.Serializable;

public class SimpleLocation implements Serializable {

    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public SimpleLocation(Location location) {
        x = location.getX();
        y = location.getY();
        z = location.getZ();
        yaw = location.getYaw();
        pitch = location.getPitch();
    }

    public SimpleLocation(BlockVector3 vector) {
        x = vector.getX();
        y = vector.getY();
        z = vector.getZ();
        yaw = 0;
        pitch = 0;
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    public BlockVector3 toBlockVector3() {
        return BlockVector3.at(x, y, z);
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("x", x);
        json.put("y", y);
        json.put("z", z);
        return json;
    }

    public static SimpleLocation fromJson(JSONObject jsonObject) {
        double x = Double.parseDouble(String.valueOf(jsonObject.get("x")));
        double y = Double.parseDouble(String.valueOf(jsonObject.get("y")));
        double z = Double.parseDouble(String.valueOf(jsonObject.get("z")));
        return new SimpleLocation(BlockVector3.at(x, y, z));
    }

    public static Vector getOffset(Location from, Location to) {
        var vector = new Vector();
        vector.setX(from.getX() - to.getX());
        vector.setY(from.getY() - to.getY());
        vector.setZ(from.getZ() - to.getZ());
        return vector;
    }

    public static BlockVector3 getOffset(BlockVector3 from, BlockVector3 to) {
        double x = from.getX() - to.getX();
        double y = from.getY() - to.getY();
        double z = from.getZ() - to.getZ();
        return BlockVector3.at(x, y, z);
    }
}
