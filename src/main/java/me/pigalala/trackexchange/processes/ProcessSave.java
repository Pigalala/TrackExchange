package me.pigalala.trackexchange.processes;

import co.aikar.taskchain.TaskChain;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.Region;
import me.makkuusen.timing.system.track.Track;
import me.pigalala.trackexchange.TrackExchange;
import me.pigalala.trackexchange.file.save.ButlerFileSaver;
import me.pigalala.trackexchange.file.save.LocalFileSaver;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

        chain.asyncFutures((f) -> List.of(CompletableFuture.supplyAsync(this::doTrackStage), CompletableFuture.supplyAsync(this::doSchematicStage)))
                .async(this::doWriteStage)
                .execute((success) -> {
                    if(success)
                        notifyProcessFinishText(System.currentTimeMillis() - startTime);
                    else
                        notifyProcessFinishExceptionallyText();
                });
    }

    private Void doTrackStage() {
        final String stage = "TRACK";
        final long startTime = System.currentTimeMillis();

        notifyStageBeginText(stage);
        TrackExchangeTrack trackExchangeTrack = new TrackExchangeTrack(track, new SimpleLocation(origin));
        chain.setTaskData("track", trackExchangeTrack);
        notifyStageFinishText(stage, System.currentTimeMillis() - startTime);

        return null;
    }

    private Void doSchematicStage() {
        final String stage = "SCHEMATIC";
        final long startTime = System.currentTimeMillis();

        notifyStageBeginText(stage);
        try {
            Region selection = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player)).getSelection();
            BlockArrayClipboard clipboard = new BlockArrayClipboard(selection);
            try(EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
                ForwardExtentCopy op = new ForwardExtentCopy(session, selection, clipboard, selection.getMinimumPoint());
                op.setCopyingEntities(true);
                Operations.complete(op);
            }
            chain.setTaskData("schematic", new TrackExchangeSchematic(clipboard));
            notifyStageFinishText(stage, System.currentTimeMillis() - startTime);
        } catch (WorldEditException e) {
            if(e instanceof IncompleteRegionException)
                notifyPlayer(Component.text("Saving without Schematic.", NamedTextColor.YELLOW));
            else
                notifyPlayer(Component.text("Saving without Schematic...", NamedTextColor.YELLOW).hoverEvent(Component.text(e.getMessage(), NamedTextColor.RED)));
            chain.setTaskData("schematic", null);
        }

        return null;
    }

    private void doWriteStage() {
        final String stage = "WRITE";
        final long startTime = System.currentTimeMillis();

        notifyStageBeginText(stage);
        TrackExchangeTrack trackExchangeTrack = chain.getTaskData("track");
        TrackExchangeSchematic trackExchangeSchematic = chain.getTaskData("schematic");
        TrackExchangeFile trackExchangeFile = new TrackExchangeFile(trackExchangeTrack, new SimpleLocation(origin), trackExchangeSchematic);
        try {
            if (!trackExchangeFile.write(saveAs, TrackExchange.isButlerEnabled() ? new ButlerFileSaver(): new LocalFileSaver())) {
                notifyProcessFinishExceptionallyText();
                return;
            }
            notifyStageFinishText(stage, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            notifyStageFinishExceptionallyText(stage, e);
        }
    }
}
