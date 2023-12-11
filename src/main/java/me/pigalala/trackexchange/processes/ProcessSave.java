package me.pigalala.trackexchange.processes;

import co.aikar.taskchain.TaskChain;
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

public class ProcessSave implements Process {

    private final Player player;
    private final Track track;
    private final String saveAs;

    private final TaskChain<?> chain;
    private final Location origin;

    public ProcessSave(Player player, Track track, String saveAs) {
        this.player = player;
        this.track = track;
        this.saveAs = saveAs;

        this.chain = TrackExchange.newChain();
        this.origin = player.getLocation();
    }

    @Override
    public void execute() {
        final long startTime = System.currentTimeMillis();
        player.sendMessage(getProcessStartText());

        chain.async(this::doTrackStage)
                .async(this::doSchematicStage)
                .async(this::doWriteStage)
                .execute((success) -> {
                    if(success)
                        player.sendMessage(getProcessFinishText(System.currentTimeMillis() - startTime));
                    else
                        player.sendMessage(getProcessFinishExceptionallyText());
                });
    }

    private void doTrackStage() {
        final String stage = "TRACK";
        final long startTime = System.currentTimeMillis();

        player.sendMessage(getStageBeginText(stage));
        TrackExchangeTrack trackExchangeTrack = new TrackExchangeTrack(track, new SimpleLocation(origin));
        chain.setTaskData("track", trackExchangeTrack);
        player.sendMessage(getStageFinishText(stage, System.currentTimeMillis() - startTime));
    }

    private void doSchematicStage() {
        final String stage = "SCHEMATIC";
        final long startTime = System.currentTimeMillis();

        player.sendMessage(getStageBeginText(stage));
        try {
            Region r = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player)).getSelection();
            BlockArrayClipboard clipboard = new BlockArrayClipboard(r);
            Operations.complete(new ForwardExtentCopy(BukkitAdapter.adapt(player.getWorld()), r, clipboard, r.getMinimumPoint()));
            chain.setTaskData("schematic", new TrackExchangeSchematic(clipboard));
            player.sendMessage(getStageFinishText(stage, System.currentTimeMillis() - startTime));
        } catch (WorldEditException e) {
            if(e instanceof IncompleteRegionException)
                player.sendMessage(Component.text("Saving without Schematic.", NamedTextColor.YELLOW));
            else
                player.sendMessage(Component.text("Saving without Schematic...", NamedTextColor.YELLOW).hoverEvent(Component.text(e.getMessage(), NamedTextColor.RED)));
            chain.setTaskData("schematic", null);
        }
    }

    private void doWriteStage() {
        final String stage = "WRITE";
        final long startTime = System.currentTimeMillis();

        player.sendMessage(getStageBeginText(stage));
        TrackExchangeTrack trackExchangeTrack = chain.getTaskData("track");
        TrackExchangeSchematic trackExchangeSchematic = chain.getTaskData("schematic");
        TrackExchangeFile trackExchangeFile = new TrackExchangeFile(trackExchangeTrack, new SimpleLocation(origin), trackExchangeSchematic);
        try {
            trackExchangeFile.write(new File(TrackExchange.instance.getDataFolder(), saveAs));
            player.sendMessage(getStageFinishText(stage, System.currentTimeMillis() - startTime));
        } catch (IOException e) {
            player.sendMessage(getStageFinishExceptionallyText(stage, e));
        }
    }

    @Override
    public String processName() {
        return "SAVE";
    }
}
