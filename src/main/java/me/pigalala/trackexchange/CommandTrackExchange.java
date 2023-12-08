package me.pigalala.trackexchange;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.annotation.*;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.Region;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.pigalala.trackexchange.trackcomponents.TrackExchangeFile;
import me.pigalala.trackexchange.trackcomponents.TrackExchangeSchematic;
import me.pigalala.trackexchange.utils.Messages;
import me.pigalala.trackexchange.trackcomponents.TrackExchangeTrack;
import me.pigalala.trackexchange.utils.SimpleLocation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

@CommandAlias("trackexchange|te")
public class CommandTrackExchange extends BaseCommand {

    @Subcommand("copy")
    @CommandCompletion("@track <saveas>")
    @CommandPermission("trackexchange.export")
    public static void onCopy(Player player, Track track, @Optional @Single String saveAs) {
        if(saveAs == null)
            saveAs = track.getCommandName();

        if(TrackExchangeFile.trackExchangeFileAlreadyExists(saveAs))
            throw new ConditionFailedException("This trackexchange file already exists");

        executeSave(player, track, saveAs);
    }

    @Subcommand("paste")
    @CommandCompletion("<filename> <loadas>")
    @CommandPermission("trackexchange.import")
    public static void onPaste(Player player, String fileName, @Optional String loadAs) {
        fileName = fileName.replace(".zip", "");
        if(loadAs == null)
            loadAs = fileName;

        if(!TrackDatabase.trackNameAvailable(loadAs))
            throw new ConditionFailedException("A track with this name already exists");

        executeLoad(player, fileName, loadAs);
    }

    private static void executeSave(Player player, Track track, String saveAs) {
        Messages.sendSaveStarted(player, track.getDisplayName(), saveAs);
        Location spawnLocation = player.getLocation().clone();
        var chain = TrackExchange.newChain();
        chain.async(() -> {
            player.sendMessage(Component.text("Attempting to save track.", TextColor.color(0xF38AFF)));
            TrackExchangeTrack trackExchangeTrack = new TrackExchangeTrack(track, new SimpleLocation(spawnLocation));
            chain.setTaskData("track", trackExchangeTrack);
            player.sendMessage(Component.text("Successfully saved track.", NamedTextColor.GREEN));
        }).async(() -> {
            player.sendMessage(Component.text("Attempting to save track schematic.", TextColor.color(0xF38AFF)));
            try {
                Region r = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player)).getSelection();
                if(r == null)
                    return;
                BlockArrayClipboard clipboard = new BlockArrayClipboard(r);
                Operations.complete(new ForwardExtentCopy(BukkitAdapter.adapt(player.getWorld()), r, clipboard, r.getMinimumPoint()));
                chain.setTaskData("schematic", new TrackExchangeSchematic(clipboard));
                player.sendMessage(Component.text("Successfully saved schematic", NamedTextColor.GREEN));
            } catch (WorldEditException e) {
                if(e instanceof IncompleteRegionException)
                    player.sendMessage(Component.text("Saving without Schematic", NamedTextColor.GREEN));
                else
                    player.sendMessage(Component.text("Saving without Schematic (" + e.getMessage() + ")", NamedTextColor.YELLOW));
            }
        }).async(() -> {
            player.sendMessage(Component.text("Attempting to save track components to file.", TextColor.color(0xF38AFF)));
            TrackExchangeTrack trackExchangeTrack = chain.getTaskData("track");
            TrackExchangeSchematic trackExchangeSchematic = chain.getTaskData("schematic");
            TrackExchangeFile trackExchangeFile = new TrackExchangeFile(trackExchangeTrack, new SimpleLocation(spawnLocation), trackExchangeSchematic);
            try {
                trackExchangeFile.write(new File(TrackExchange.instance.getDataFolder(), saveAs));
                player.sendMessage(Component.text("Successfully saved track to file.", NamedTextColor.GREEN));
            } catch (IOException e) {
                Messages.sendError(player, e);
            }
        }).execute(() -> {
            Messages.sendSaveFinished(player, track.getDisplayName(), saveAs);
        });
    }

    private static void executeLoad(Player player, String fileName, String loadAs) {
        Messages.sendLoadStarted(player, fileName, loadAs);
        Location origin = player.getLocation().clone();
        var chain = TrackExchange.newChain();
        chain.async(() -> {
            player.sendMessage(Component.text("Attempting to read trackexchange file.", TextColor.color(0xF38AFF)));
            try {
                TrackExchangeFile trackExchangeFile = TrackExchangeFile.read(new File(TrackExchange.instance.getDataFolder(), fileName), loadAs);
                chain.setTaskData("trackExchangeFile", trackExchangeFile);
                player.sendMessage(Component.text("Successfully read trackexchange file.", NamedTextColor.GREEN));
            } catch (IOException e) {
                Messages.sendError(player, e);
                e.printStackTrace();
            }
        }).async(() -> {
            player.sendMessage(Component.text("Attempting to create track.", TextColor.color(0xF38AFF)));
            TrackExchangeFile trackExchangeFile = chain.getTaskData("trackExchangeFile");
            try {
                trackExchangeFile.getTrack().createTrack(player);
                player.sendMessage(Component.text("Successfully created track.", NamedTextColor.GREEN));
            } catch (SQLException e) {
                Messages.sendError(player, e);
                e.printStackTrace();
            }
        }).async(() -> {
            TrackExchangeFile trackExchangeFile = chain.getTaskData("trackExchangeFile");
            trackExchangeFile.getSchematic().ifPresent(schematic -> {
                player.sendMessage(Component.text("Attempting to paste track.", TextColor.color(0xF38AFF)));
                try {
                    schematic.pasteAt(origin);
                    player.sendMessage(Component.text("Successfully pasted track.", NamedTextColor.GREEN));
                } catch (WorldEditException e) {
                    Messages.sendError(player, e);
                }
            });
        }).execute(() -> {
            Messages.sendLoadFinished(player, fileName, loadAs);
        });
    }
}
