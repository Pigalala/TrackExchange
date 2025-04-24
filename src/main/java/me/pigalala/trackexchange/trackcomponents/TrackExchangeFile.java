package me.pigalala.trackexchange.trackcomponents;

import com.google.gson.JsonObject;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import lombok.Getter;
import me.pigalala.trackexchange.TrackExchange;
import me.pigalala.trackexchange.file.load.TrackExchangeFileReader;
import me.pigalala.trackexchange.file.save.TrackExchangeFileSaver;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
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
        File dir = new File(TrackExchange.getTracksPath().toAbsolutePath().toString(), name);
        dir.mkdir();

        File dataFile = new File(dir, "data.component");
        File trackFile = new File(dir, "track.component");
        File schematicFile = new File(dir, "schematic.component");

        var data = new JsonObject();
        data.addProperty("version", TrackExchange.TRACK_VERSION);
        if (getSchematic().isPresent()) {
            data.add("clipboardOffset", new SimpleLocation(SimpleLocation.getOffset(getSchematic().get().getClipboard().getOrigin(), origin.toBlockVector3())).asJson());
        }

        dataFile.createNewFile();
        try (FileWriter writer = new FileWriter(dataFile)) {
            writer.write(data.toString());
        }

        trackFile.createNewFile();
        try (FileWriter writer = new FileWriter(trackFile)) {
            writer.write(track.asJson().toString());
        }

        if (getSchematic().isPresent()) {
            schematicFile.createNewFile();
            schematic.saveTo(schematicFile);
        }

        byte[] bytes = zipDirIntoBytes(dir);
        saver.save(name, bytes);
        cleanup(dir);
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

    private static byte[] zipDirIntoBytes(File dir) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(byteOut)) {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    if(attributes.isSymbolicLink())
                        return FileVisitResult.CONTINUE;

                    try(FileInputStream fileIn = new FileInputStream(file.toFile())) {
                        Path targetFile = dir.toPath().relativize(file);
                        zipOut.putNextEntry(new ZipEntry(targetFile.toString()));

                        byte[] buffer = new byte[1024];
                        int len;
                        while((len = fileIn.read(buffer)) > 0)
                            zipOut.write(buffer, 0, len);
                        zipOut.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    TrackExchange.instance.getLogger().log(Level.SEVERE, "Error visiting file: " + file.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
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

    public static void cleanup(File dir) {
        if (dir.listFiles() != null) {
            Arrays.stream(dir.listFiles()).forEach(File::delete);
        }
        dir.delete();
        new File(TrackExchange.getTracksPath().toFile(), "data.component").delete();
        new File(TrackExchange.getTracksPath().toFile(), "track.component").delete();
        new File(TrackExchange.getTracksPath().toFile(), "schematic.component").delete();
    }
}
