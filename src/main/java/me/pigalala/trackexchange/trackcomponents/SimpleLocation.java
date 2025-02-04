package me.pigalala.trackexchange.trackcomponents;

import com.google.gson.JsonObject;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class SimpleLocation implements TrackComponent {

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

    public SimpleLocation(JsonObject locationBody) {
        x = locationBody.get("x").getAsDouble();
        y = locationBody.get("y").getAsDouble();
        z = locationBody.get("z").getAsDouble();
        yaw = locationBody.get("yaw").getAsFloat();
        pitch = locationBody.get("pitch").getAsFloat();
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    public BlockVector3 toBlockVector3() {
        return BlockVector3.at(x, y, z);
    }

    public static SimpleLocation fromJson(JsonObject jsonObject) {
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

    @Override
    public JsonObject asJson() {
        var locationBody = new JsonObject();
        locationBody.addProperty("x", x);
        locationBody.addProperty("y", y);
        locationBody.addProperty("z", z);
        locationBody.addProperty("yaw", yaw);
        locationBody.addProperty("pitch", pitch);
        return locationBody;
    }
}
