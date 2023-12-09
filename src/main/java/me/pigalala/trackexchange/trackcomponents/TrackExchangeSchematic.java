package me.pigalala.trackexchange.trackcomponents;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.session.ClipboardHolder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

@Getter
public class TrackExchangeSchematic {

    private final Clipboard clipboard;

    @Setter
    private SimpleLocation offset;

    public TrackExchangeSchematic(Clipboard clipboard) {
        this.clipboard = clipboard;
    }

    public void pasteAt(Location location) throws WorldEditException {
        EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()));
        Operations.complete(new ClipboardHolder(clipboard).createPaste(editSession).to(offset.toBlockVector3().add(new SimpleLocation(location).toBlockVector3())).copyEntities(true).build());
        editSession.close();
    }
}
