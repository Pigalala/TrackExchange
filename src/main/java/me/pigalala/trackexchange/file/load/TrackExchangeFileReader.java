package me.pigalala.trackexchange.file.load;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public interface TrackExchangeFileReader {

    byte[] read(String trackExchangeName);

    record TrackExchangeBytes(byte[] dataBytes, byte[] trackBytes, @Nullable byte[] schematicBytes) {

        public JsonObject readDataJson() {
            return JsonParser.parseString(new String(dataBytes)).getAsJsonObject();
        }

        public JsonObject readTrackJson() {
            return JsonParser.parseString(new String(trackBytes)).getAsJsonObject();
        }

        public Optional<Clipboard> readSchematic() {
            if (schematicBytes == null) {
                return Optional.empty();
            }

            try {
                return Optional.ofNullable(BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(new ByteArrayInputStream(schematicBytes)).read());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static TrackExchangeBytes splitBytes(byte[] bigBytes) {
        byte[] dataBytes = null;
        byte[] trackBytes = null;
        byte[] schematicBytes = null;

        try (ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(bigBytes))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipStream.getNextEntry()) != null) {
                byte[] zipBytes = zipStream.readAllBytes();
                switch (zipEntry.getName()) {
                    case "data.component" -> dataBytes = zipBytes;
                    case "track.component" -> trackBytes = zipBytes;
                    case "schematic.component" -> schematicBytes = zipBytes;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (dataBytes == null || trackBytes == null) {
            throw new RuntimeException("Missing bytes!!!");
        }

        return new TrackExchangeBytes(dataBytes, trackBytes, schematicBytes);
    }
}
