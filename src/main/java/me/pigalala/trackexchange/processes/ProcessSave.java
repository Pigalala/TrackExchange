package me.pigalala.trackexchange.processes;

import co.aikar.taskchain.TaskChain;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.Region;
import me.makkuusen.timing.system.track.Track;
import me.pigalala.trackexchange.TrackExchange;
import me.pigalala.trackexchange.trackcomponents.TrackExchangeFile;
import me.pigalala.trackexchange.trackcomponents.TrackExchangeSchematic;
import me.pigalala.trackexchange.trackcomponents.TrackExchangeTrack;
import me.pigalala.trackexchange.trackcomponents.SimpleLocation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class ProcessSave extends Process {

    private final Track track;
    private final String saveAs;

    private final TaskChain<?> chain;
    private final Location origin;

    public ProcessSave(Player player, Track track, String saveAs) {
        super(player, "SAVE");
        this.track = track;
        this.saveAs = saveAs;

        this.chain = TrackExchange.newChain();
        this.origin = player.getLocation();
    }

    @Override
    public void execute() {
        final long startTime = System.currentTimeMillis();
        notifyProcessStartText();

        chain.async(this::doTrackStage)
                .async(this::doSchematicStage)
                .async(this::doWriteStage)
                .execute((success) -> {
                    if(success)
                        notifyProcessFinishText(System.currentTimeMillis() - startTime);
                    else
                        notifyProcessFinishExceptionallyText();
                });
    }

    private void doTrackStage() {
        final String stage = "TRACK";
        final long startTime = System.currentTimeMillis();

        notifyStageBeginText(stage);
        TrackExchangeTrack trackExchangeTrack = new TrackExchangeTrack(track, new SimpleLocation(origin));
        chain.setTaskData("track", trackExchangeTrack);
        notifyStageFinishText(stage, System.currentTimeMillis() - startTime);
    }

    private void doSchematicStage() {
        final String stage = "SCHEMATIC";
        final long startTime = System.currentTimeMillis();

        notifyStageBeginText(stage);
        try {
            Region r = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player)).getSelection();
            BlockArrayClipboard clipboard = new BlockArrayClipboard(r);
            Operations.complete(new ForwardExtentCopy(BukkitAdapter.adapt(player.getWorld()), r, clipboard, r.getMinimumPoint()));
            chain.setTaskData("schematic", new TrackExchangeSchematic(clipboard));
            notifyStageFinishText(stage, System.currentTimeMillis() - startTime);
        } catch (WorldEditException e) {
            if(e instanceof IncompleteRegionException)
                notifyPlayer(Component.text("Saving without Schematic.", NamedTextColor.YELLOW));
            else
                notifyPlayer(Component.text("Saving without Schematic...", NamedTextColor.YELLOW).hoverEvent(Component.text(e.getMessage(), NamedTextColor.RED)));
            chain.setTaskData("schematic", null);
        }
    }

    private void doWriteStage() {
        final String stage = "WRITE";
        final long startTime = System.currentTimeMillis();

        notifyStageBeginText(stage);
        TrackExchangeTrack trackExchangeTrack = chain.getTaskData("track");
        TrackExchangeSchematic trackExchangeSchematic = chain.getTaskData("schematic");
        TrackExchangeFile trackExchangeFile = new TrackExchangeFile(trackExchangeTrack, new SimpleLocation(origin), trackExchangeSchematic);
        try {
            trackExchangeFile.write(new File(TrackExchange.instance.getDataFolder(), saveAs));
            notifyStageFinishText(stage, System.currentTimeMillis() - startTime);
        } catch (IOException e) {
            notifyStageFinishExceptionallyText(stage, e);
            notifyStageFinishExceptionallyText(stage, e);
        }
    }
}
