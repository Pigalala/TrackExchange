package me.pigalala.trackexchange.processes;

import co.aikar.taskchain.TaskChain;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.track.Track;
import me.pigalala.trackexchange.TrackExchange;
import me.pigalala.trackexchange.UndoAction;
import me.pigalala.trackexchange.trackcomponents.TrackExchangeFile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;

public class ProcessLoad extends Process {

    private final String fileName;
    private final String loadAs;

    private final TaskChain<?> chain;
    private final Location origin;

    public ProcessLoad(Player player, String fileName, String loadAs) {
        super(player, "LOAD");
        this.fileName = fileName;
        this.loadAs = loadAs;

        chain = TrackExchange.newChain();
        origin = player.getLocation().clone();
    }

    @Override
    public void execute() {
        final long startTime = System.currentTimeMillis();
        notifyProcessStartText();

        chain.async(this::doReadStage)
            .asyncFutures((f) -> List.of(
                    CompletableFuture.supplyAsync(this::doTrackStage, Bukkit.getScheduler().getMainThreadExecutor(TrackExchange.instance)),
                    CompletableFuture.supplyAsync(this::doPasteStage, Bukkit.getScheduler().getMainThreadExecutor(TrackExchange.instance))
                    )).execute((success) -> {
                if (success) {
                    createUndo();
                    notifyProcessFinishText(System.currentTimeMillis() - startTime);
                } else {
                    notifyProcessFinishExceptionallyText();
                }
            }
        );
    }

    private void doReadStage() {
        final String stage = "READ";
        final long startTime = System.currentTimeMillis();

        notifyStageBeginText(stage);
        try {
            TrackExchangeFile trackExchangeFile = TrackExchangeFile.read(new File(TrackExchange.instance.getDataFolder(), fileName), loadAs);
            chain.setTaskData("trackExchangeFile", trackExchangeFile);
            notifyStageFinishText(stage, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            notifyStageFinishExceptionallyText(stage, e);
            chain.abortChain();
        }
    }

    private Void doTrackStage() {
        final String stage = "TRACK";
        final long startTime = System.currentTimeMillis();

        notifyStageBeginText(stage);
        TrackExchangeFile trackExchangeFile = chain.getTaskData("trackExchangeFile");
        try {
            Track track = trackExchangeFile.getTrack().createTrack(player);
            chain.setTaskData("track", track);
            notifyStageFinishText(stage, System.currentTimeMillis() - startTime);
        } catch (SQLException e) {
            notifyStageFinishExceptionallyText(stage, e);
            chain.abortChain();
        }

        return null;
    }

    private Void doPasteStage() {
        final String stage = "PASTE";
        final long startTime = System.currentTimeMillis();

        TrackExchangeFile trackExchangeFile = chain.getTaskData("trackExchangeFile");
        trackExchangeFile.getSchematic().ifPresentOrElse(schematic -> {
            notifyStageBeginText(stage);
            try {
                EditSession session = schematic.pasteAt(origin);
                chain.setTaskData("editSession", session);
                notifyStageFinishText(stage, System.currentTimeMillis() - startTime);
            } catch (WorldEditException e) {
                notifyStageFinishExceptionallyText(stage, e);
            }
        }, () -> {
            notifyPlayer(Component.text("Skipping stage '" + stage + "'.", NamedTextColor.YELLOW));
        });

        return null;
    }

    private void createUndo() {
        UndoAction undoAction = () -> {
            EditSession session = chain.getTaskData("editSession");
            Track track = chain.getTaskData("track");
            if (session != null) {
                Bukkit.getScheduler().runTaskAsynchronously(TrackExchange.instance, () -> {
                    try (EditSession undoSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player))) {
                        session.undo(undoSession);
                    }
                });
            }
            TrackDatabase.removeTrack(track);
            player.sendMessage(Component.text("Undid your thingy.", NamedTextColor.GREEN));
        };

        TrackExchange.playerActions.putIfAbsent(player.getUniqueId(), new Stack<>());
        TrackExchange.playerActions.get(player.getUniqueId()).push(undoAction);
    }
}
