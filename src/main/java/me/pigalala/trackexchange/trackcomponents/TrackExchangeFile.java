package me.pigalala.trackexchange.trackcomponents;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import lombok.Getter;
import me.pigalala.trackexchange.TrackExchange;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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

    public void write(File dir) throws IOException {
        dir.mkdir();

        File dataFile = new File(dir, "data.component");
        File trackFile = new File(dir, "track.component");
        File schematicFile = new File(dir, "schematic.component");

        JSONObject data = new JSONObject();
        data.put("version", TrackExchange.TRACK_VERSION);
        if (getSchematic().isPresent()) {
            data.put("clipboardOffset", new SimpleLocation(SimpleLocation.getOffset(getSchematic().get().getClipboard().getOrigin(), origin.toBlockVector3())).asJson());
        }

        dataFile.createNewFile();
        try (FileWriter writer = new FileWriter(dataFile)) {
            writer.write(data.toJSONString());
        }

        trackFile.createNewFile();
        try (FileWriter writer = new FileWriter(trackFile)) {
            writer.write(track.asJson().toJSONString());
        }

        if (getSchematic().isPresent()) {
            schematicFile.createNewFile();
            schematic.saveTo(schematicFile);
        }

        zipDir(dir);
        cleanup(dir);
    }

    public static TrackExchangeFile read(File trackDir, String newName) throws Exception {
        unzipDir(findFile(trackDir.getName() + ".trackexchange", TrackExchange.instance.getDataFolder()), TrackExchange.instance.getDataFolder());

        File dataFile = new File(TrackExchange.instance.getDataFolder(), "data.component");
        File trackFile = new File(TrackExchange.instance.getDataFolder(), "track.component");
        File schematicFile = new File(TrackExchange.instance.getDataFolder(), "schematic.component");

        SimpleLocation clipboardOffset = new SimpleLocation(BlockVector3.at(0, 0, 0));
        try (FileReader reader = new FileReader(dataFile)) {
            JSONParser parser = new JSONParser();
            JSONObject data = (JSONObject) parser.parse(reader);
            int version = Integer.parseInt(String.valueOf(data.get("version")));
            if (version != TrackExchange.TRACK_VERSION) {
                throw new RuntimeException("This track's version does not match the server's version. (Track: " + version + ". Server: " + TrackExchange.TRACK_VERSION + ")");
            }
            JSONObject clipboardOffsetObject = (JSONObject) data.get("clipboardOffset");
            if (clipboardOffsetObject != null) {
                clipboardOffset = SimpleLocation.fromJson(clipboardOffsetObject);
            }
        }

        TrackExchangeTrack trackExchangeTrack;
        try (FileReader reader = new FileReader(trackFile)) {
            JSONParser parser = new JSONParser();
            JSONObject trackData = (JSONObject) parser.parse(reader);
            trackExchangeTrack = new TrackExchangeTrack(trackData);
            trackExchangeTrack.setDisplayName(newName);
        }

        TrackExchangeSchematic trackExchangeSchematic = null;
        if (schematicFile.exists()) {
            try(FileInputStream fileIn = new FileInputStream(schematicFile)) {
                ClipboardReader clipboardReader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(fileIn);
                Clipboard clipboard = clipboardReader.read();
                trackExchangeSchematic = new TrackExchangeSchematic(clipboard);
                trackExchangeSchematic.setOffset(clipboardOffset);
            }
        }

        cleanup(trackDir);
        return new TrackExchangeFile(trackExchangeTrack, trackExchangeTrack.getOrigin(), trackExchangeSchematic);
    }

    private static void zipDir(File dir) throws IOException {
        try(ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(dir.getPath() + ".trackexchange"))) {
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
    }

    private static void unzipDir(File dir, File dest) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(dir.getPath()))) {
            ZipEntry zipEntry = zipIn.getNextEntry();
            while(zipEntry != null) {
                Path newPath = dest.toPath().resolve(zipEntry.getName()).normalize();
                Files.copy(zipIn, newPath, StandardCopyOption.REPLACE_EXISTING);
                zipIn.closeEntry();
                zipEntry = zipIn.getNextEntry();
            }
        }
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
        File f = findFile(fileName + ".trackexchange", TrackExchange.instance.getDataFolder());
        return f != null && f.exists();
    }

    public static void cleanup(File dir) {
        if(dir.listFiles() != null)
            Arrays.stream(dir.listFiles()).forEach(File::delete);
        dir.delete();
        new File(TrackExchange.instance.getDataFolder(), "data.component").delete();
        new File(TrackExchange.instance.getDataFolder(), "track.component").delete();
        new File(TrackExchange.instance.getDataFolder(), "schematic.component").delete();
    }
}
