package me.pigalala.trackexchange.trackcomponents;

import com.google.gson.JsonObject;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import lombok.Getter;
import me.pigalala.trackexchange.TrackExchange;
import me.pigalala.trackexchange.file.load.TrackExchangeFileReader;
import me.pigalala.trackexchange.file.save.TrackExchangeFileSaver;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Getter
public class TrackExchangeFile {
    private final TrackExchangeTrack track;
    private final SimpleLocation origin;
    private final TrackExchangeSchematic schematic;

    public TrackExchangeFile(TrackExchangeTrack track, SimpleLocation origin, TrackExchangeSchematic schematic) {
        this.track = track;
        this.origin = origin;
        this.schematic = schematic;
    }

    public Optional<TrackExchangeSchematic> getSchematic() {
        return Optional.ofNullable(schematic);
    }

    public void write(String name, TrackExchangeFileSaver saver) throws IOException {
        var data = new JsonObject();
        data.addProperty("version", TrackExchange.TRACK_VERSION);
        if (getSchematic().isPresent()) {
            data.add("clipboardOffset", new SimpleLocation(SimpleLocation.getOffset(getSchematic().get().getClipboard().getOrigin(), origin.toBlockVector3())).asJson());
        }

        byte[] dataBytes = data.toString().getBytes(StandardCharsets.UTF_8);
        byte[] trackBytes = track.asJson().toString().getBytes(StandardCharsets.UTF_8);
        byte[] schematicBytes = null;

        if (getSchematic().isPresent()) {
            schematicBytes = schematic.clipboardAsBytes();
        }

        byte[] bytes = compressBytes(dataBytes, trackBytes, schematicBytes);
        saver.save(name, bytes);
    }

    public static TrackExchangeFile read(String trackFileName, TrackExchangeFileReader fileReader, String newName) {
        TrackExchangeFileReader.TrackExchangeBytes trackExchangeBytes = TrackExchangeFileReader.splitBytes(fileReader.read(trackFileName));

        JsonObject dataJson = trackExchangeBytes.readDataJson();
        int version = dataJson.get("version").getAsInt();
        if (version != TrackExchange.TRACK_VERSION) {
            throw new RuntimeException("This track's version does not match the server's version. (Track: " + version + ". Server: " + TrackExchange.TRACK_VERSION + ")");
        }

        SimpleLocation clipboardOffset = new SimpleLocation(BlockVector3.at(0, 0, 0));
        if (dataJson.has("clipboardOffset")) {
            JsonObject clipboardOffsetObject = dataJson.get("clipboardOffset").getAsJsonObject();
            clipboardOffset = SimpleLocation.fromJson(clipboardOffsetObject);
        }

        JsonObject trackJson = trackExchangeBytes.readTrackJson();
        TrackExchangeTrack trackExchangeTrack = new TrackExchangeTrack(trackJson);
        trackExchangeTrack.setDisplayName(newName);

        TrackExchangeSchematic trackExchangeSchematic;
        Optional<Clipboard> schematic = trackExchangeBytes.readSchematic();
        if (schematic.isPresent()) {
            trackExchangeSchematic = new TrackExchangeSchematic(schematic.get());
            trackExchangeSchematic.setOffset(clipboardOffset);
        } else {
            trackExchangeSchematic = null;
        }

        return new TrackExchangeFile(trackExchangeTrack, trackExchangeTrack.getOrigin(), trackExchangeSchematic);
    }

    private static byte[] compressBytes(byte[] dataBytes, byte[] trackBytes, @Nullable byte[] schematicBytes) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (var zipOut = new ZipOutputStream(byteOut)) {
            zipOut.putNextEntry(new ZipEntry("data.component"));
            zipOut.write(dataBytes);
            zipOut.closeEntry();

            zipOut.putNextEntry(new ZipEntry("track.component"));
            zipOut.write(trackBytes);
            zipOut.closeEntry();

            if (schematicBytes != null) {
                zipOut.putNextEntry(new ZipEntry("schematic.component"));
                zipOut.write(schematicBytes);
                zipOut.closeEntry();
            }
        }

        return byteOut.toByteArray();
    }

    // Check to see if file 'find' is in 'dir' when the file name cases do not match.
    private static File findFile(String find, File dir) {
        File[] files = dir.listFiles();
        if(files == null)
            return null;

        for(File file : files) {
            if(find.equalsIgnoreCase(file.getName()))
                return file;
        }

        return null;
    }

    public static boolean trackExchangeFileExists(String fileName) {
        File f = findFile(fileName + ".trackexchange", TrackExchange.getTracksPath().toFile());
        return f != null && f.exists();
    }
}
