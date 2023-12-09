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

public class ProcessLoad implements Process {

    private final Player player;
    private final String fileName;
    private final String loadAs;

    private final TaskChain<?> chain;
    private final Location origin;

    public ProcessLoad(Player player, String fileName, String loadAs) {
        this.player = player;
        this.fileName = fileName;
        this.loadAs = loadAs;

        chain = TrackExchange.newChain();
        origin = player.getLocation().clone();
    }

    @Override
    public void execute() {
        player.sendMessage(getProcessStartText());
        chain.async(this::doReadStage)
                .async(this::doTrackStage)
                .async(this::doPasteStage)
                .execute((success) -> {
                    if (success)
                        player.sendMessage(getProcessFinishText());
                    else
                        player.sendMessage(getProcessFinishExceptionallyText());
                    TrackExchangeFile.cleanup();
                }
        );
    }

    private void doReadStage() {
        final String stage = "READ";
        player.sendMessage(getStageBeginText(stage));
        try {
            TrackExchangeFile trackExchangeFile = TrackExchangeFile.read(new File(TrackExchange.instance.getDataFolder(), fileName), loadAs);
            chain.setTaskData("trackExchangeFile", trackExchangeFile);
            player.sendMessage(getStageFinishText(stage));
        } catch (Exception e) {
            player.sendMessage(getStageFinishExceptionallyText(stage, e));
            chain.abortChain();
        }
    }

    private void doTrackStage() {
        final String stage = "TRACK";
        player.sendMessage(getStageBeginText(stage));
        TrackExchangeFile trackExchangeFile = chain.getTaskData("trackExchangeFile");
        try {
            trackExchangeFile.getTrack().createTrack(player);
            player.sendMessage(getStageFinishText(stage));
        } catch (SQLException e) {
            player.sendMessage(getStageFinishExceptionallyText(stage, e));
            chain.abortChain();
        }
    }

    private void doPasteStage() {
        final String stage = "PASTE";
        TrackExchangeFile trackExchangeFile = chain.getTaskData("trackExchangeFile");
        trackExchangeFile.getSchematic().ifPresentOrElse(schematic -> {
            player.sendMessage(getStageBeginText(stage));
            try {
                schematic.pasteAt(origin);
                player.sendMessage(getStageFinishText(stage));
            } catch (WorldEditException e) {
                player.sendMessage(getStageFinishExceptionallyText(stage, e));
            }
        }, () -> {
            player.sendMessage(Component.text("Skipping stage '" + stage + "'.", NamedTextColor.YELLOW));
        });
    }

    @Override
    public String processName() {
        return "LOAD";
    }
}
