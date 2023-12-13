package me.pigalala.trackexchange.trackcomponents;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.session.ClipboardHolder;
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

    public void saveTo(File file) {
        FaweAPI.getTaskManager().taskNow(() -> {
            try(FileOutputStream outputStream = new FileOutputStream(file); ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(outputStream)) {
                writer.write(clipboard); // clipboard.save() gave errors for some reason :(
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, false);
    }

    public void pasteAt(Location location) throws WorldEditException {
        FaweAPI.getTaskManager().taskNow(() -> clipboard.paste(BukkitAdapter.adapt(location.getWorld()), offset.toBlockVector3().add(new SimpleLocation(location).toBlockVector3())), false);
    }
}
