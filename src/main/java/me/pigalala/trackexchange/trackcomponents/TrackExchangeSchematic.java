package me.pigalala.trackexchange.trackcomponents;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Getter
public class TrackExchangeSchematic {

    private final Clipboard clipboard;

    @Setter
    private SimpleLocation offset;

    public TrackExchangeSchematic(Clipboard clipboard) {
        this.clipboard = clipboard;
    }

    public byte[] clipboardAsBytes() throws IOException {
        var byteStream = new ByteArrayOutputStream();
        clipboard.save(byteStream, BuiltInClipboardFormat.FAST);
        return byteStream.toByteArray();
    }

    public EditSession pasteAt(Location location) throws WorldEditException {
        return clipboard.paste(BukkitAdapter.adapt(location.getWorld()), offset.toBlockVector3().add(new SimpleLocation(location).toBlockVector3()));
    }
}
