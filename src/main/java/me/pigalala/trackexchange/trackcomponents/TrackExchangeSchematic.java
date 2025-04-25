package me.pigalala.trackexchange.trackcomponents;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Getter
public class TrackExchangeSchematic {

    public static final ClipboardFormat CLIPBOARD_FORMAT = BuiltInClipboardFormat.FAST_V3;

    private final Clipboard clipboard;
    private final ClipboardFormat format;

    @Setter
    private SimpleLocation offset;

    public TrackExchangeSchematic(Clipboard clipboard) {
        this(clipboard, CLIPBOARD_FORMAT);
    }

    public TrackExchangeSchematic(Clipboard clipboard, ClipboardFormat format) {
        this.clipboard = clipboard;
        this.format = format;
    }

    public byte[] clipboardAsBytes() throws IOException {
        var byteStream = new ByteArrayOutputStream();
        clipboard.save(byteStream, format);
        return byteStream.toByteArray();
    }

    public EditSession pasteAt(Location location) throws WorldEditException {
        return clipboard.paste(BukkitAdapter.adapt(location.getWorld()), offset.toBlockVector3().add(new SimpleLocation(location).toBlockVector3()));
    }
}
