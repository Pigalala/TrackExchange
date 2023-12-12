package me.pigalala.trackexchange.processes;

import co.aikar.taskchain.TaskChain;
import com.sk89q.worldedit.WorldEditException;
import me.pigalala.trackexchange.TrackExchange;
import me.pigalala.trackexchange.trackcomponents.TrackExchangeFile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.SQLException;

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
                .async(this::doTrackStage)
                .async(this::doPasteStage)
                .execute((success) -> {
                    if (success)
                        notifyProcessFinishText(System.currentTimeMillis() - startTime);
                    else
                        notifyProcessFinishExceptionallyText();
                    TrackExchangeFile.cleanup();
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

    private void doTrackStage() {
        final String stage = "TRACK";
        final long startTime = System.currentTimeMillis();

        notifyStageBeginText(stage);
        TrackExchangeFile trackExchangeFile = chain.getTaskData("trackExchangeFile");
        try {
            trackExchangeFile.getTrack().createTrack(player);
            notifyStageFinishText(stage, System.currentTimeMillis() - startTime);
        } catch (SQLException e) {
            notifyStageFinishExceptionallyText(stage, e);
            chain.abortChain();
        }
    }

    private void doPasteStage() {
        final String stage = "PASTE";
        final long startTime = System.currentTimeMillis();

        TrackExchangeFile trackExchangeFile = chain.getTaskData("trackExchangeFile");
        trackExchangeFile.getSchematic().ifPresentOrElse(schematic -> {
            notifyStageBeginText(stage);
            try {
                schematic.pasteAt(origin);
                notifyStageFinishText(stage, System.currentTimeMillis() - startTime);
            } catch (WorldEditException e) {
                notifyStageFinishExceptionallyText(stage, e);
            }
        }, () -> {
            notifyPlayer(Component.text("Skipping stage '" + stage + "'.", NamedTextColor.YELLOW));
        });
    }
}
