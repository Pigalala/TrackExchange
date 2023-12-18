package me.pigalala.trackexchange.trackcomponents;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Getter
public class TrackExchangeSchematic {

    private final Clipboard clipboard;

    @Setter
    private SimpleLocation offset;

    public TrackExchangeSchematic(Clipboard clipboard) {
        this.clipboard = clipboard;
    }

    public void saveTo(File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file); ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(outputStream)) {
            writer.write(clipboard);
        }
    }

    public void pasteAt(Location location) throws WorldEditException {
        clipboard.paste(BukkitAdapter.adapt(location.getWorld()), offset.toBlockVector3().add(new SimpleLocation(location).toBlockVector3()));
    }
}
