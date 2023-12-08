package me.pigalala.trackexchange.trackcomponents;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import lombok.Getter;
import me.pigalala.trackexchange.TrackExchange;
import me.pigalala.trackexchange.utils.SimpleLocation;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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

        File dataFile = new File(dir, "data.trackexchange");
        dataFile.createNewFile();
        File trackFile = new File(dir, "track.trackexchange");
        trackFile.createNewFile();
        File schematicFile = new File(dir, "schematic.trackexchange");
        schematicFile.createNewFile();

        JSONObject data = new JSONObject();
        data.put("version", TrackExchange.TRACK_VERSION);
        if(getSchematic().isPresent())
            data.put("clipboardOffset", new SimpleLocation(SimpleLocation.getOffset(getSchematic().get().getClipboard().getOrigin(), origin.toBlockVector3())).toJson());
        try (FileWriter writer = new FileWriter(dataFile)) {
            writer.write(data.toJSONString());
        }

        try(FileOutputStream fileOut = new FileOutputStream(trackFile); ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(track);
        }

        getSchematic().ifPresent(schem -> {
            try(ClipboardWriter clipboardWriter = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(schematicFile))) {
                clipboardWriter.write(schem.getClipboard());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        zipDir(dir);
        dataFile.delete();
        trackFile.delete();
        schematicFile.delete();
        dir.delete();
    }

    public static TrackExchangeFile read(File trackDir, String newName) throws IOException {
        unzipDir(new File(TrackExchange.instance.getDataFolder(), trackDir.getName() + ".zip"), TrackExchange.instance.getDataFolder());

        File dataFile = new File(TrackExchange.instance.getDataFolder(), "data.trackexchange");
        File trackFile = new File(TrackExchange.instance.getDataFolder(), "track.trackexchange");
        File schematicFile = new File(TrackExchange.instance.getDataFolder(), "schematic.trackexchange");

        SimpleLocation clipboardOffset;
        try(FileReader reader = new FileReader(dataFile)) {
            JSONParser parser = new JSONParser();
            JSONObject data = (JSONObject) parser.parse(reader);
            int version = Integer.parseInt(String.valueOf(data.get("version")));
            clipboardOffset = SimpleLocation.fromJson((JSONObject) data.get("clipboardOffset"));
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        TrackExchangeTrack trackExchangeTrack;
        try(FileInputStream fileIn = new FileInputStream(trackFile); ObjectInputStream in = new ObjectInputStream(fileIn)) {
            trackExchangeTrack = (TrackExchangeTrack) in.readObject();
            trackExchangeTrack.setDisplayName(newName);
        } catch (Exception e) {
            throw new RuntimeException(e);
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

        trackFile.delete();
        dataFile.delete();
        schematicFile.delete();
        trackDir.delete();

        return new TrackExchangeFile(trackExchangeTrack, trackExchangeTrack.getOrigin(), trackExchangeSchematic);
    }

    private static void zipDir(File dir) throws IOException {
        try(ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(dir.getPath() + ".zip"))) {
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

    public static boolean trackExchangeFileAlreadyExists(String fileName) {
        File f = new File(TrackExchange.instance.getDataFolder(), fileName + ".zip");
        return f.exists();
    }
}
