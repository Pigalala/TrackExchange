package me.pigalala.trackexchange.processes;

import co.aikar.taskchain.TaskChain;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.track.Track;
import me.pigalala.trackexchange.TrackExchange;
import me.pigalala.trackexchange.file.load.ButlerFileReader;
import me.pigalala.trackexchange.file.load.LocalFileLoader;
import me.pigalala.trackexchange.file.load.TrackExchangeFileReader;
import me.pigalala.trackexchange.trackcomponents.TrackExchangeFile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;

public class ProcessLoad extends Process {

    private final String fileName;
    private final String loadAs;

    private final TaskChain<?> chain;
    private final Location origin;

    boolean createdTrack;
    boolean createdSchematic;

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
                    CompletableFuture.supplyAsync(this::doTrackStage),
                    CompletableFuture.supplyAsync(this::doPasteStage)
                    )).execute((success) -> {
                if (success) {
                    notifyProcessFinishText(System.currentTimeMillis() - startTime);
                } else {
                    notifyProcessFinishExceptionallyText();
                }
            }
        );
    }

    @Override
    public Optional<Runnable> createInverse() {
        if (!createdTrack && !createdSchematic) {
            return Optional.empty();
        }

        return Optional.of(() -> {
            if (createdSchematic) {
                Bukkit.getScheduler().runTaskAsynchronously(TrackExchange.instance, () -> {
                    try (EditSession undoSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player))) {
                        EditSession session = chain.getTaskData("editSession");
                        session.undo(undoSession);
                    }
                });
            }

            if (createdTrack) {
                Track track = chain.getTaskData("track");
                TrackDatabase.removeTrack(track);
            }
        });
    }

    private void doReadStage() {
        final String stage = "READ";
        final long startTime = System.currentTimeMillis();

        notifyStageBeginText(stage);

        TrackExchangeFileReader fileReader;
        if (TrackExchange.isButlerEnabled()) {
            try {
                fileReader = new ButlerFileReader();
            } catch (Exception e) {
                notifyStageFinishExceptionallyText(stage, e);
                chain.abortChain();
                return;
            }
        } else {
            fileReader = new LocalFileLoader();
        }

        try {
            TrackExchangeFile trackExchangeFile = TrackExchangeFile.read(fileName, fileReader, loadAs);
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

        createdTrack = true;
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
                createdSchematic = true;
            } catch (WorldEditException e) {
                notifyStageFinishExceptionallyText(stage, e);
            }
        }, () -> {
            notifyPlayer(Component.text("Skipping stage '" + stage + "'.", NamedTextColor.YELLOW));
        });

        return null;
    }
}
