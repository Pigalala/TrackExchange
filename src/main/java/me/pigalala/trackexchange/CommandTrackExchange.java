package me.pigalala.trackexchange;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.track.Track;
import me.pigalala.trackexchange.processes.ProcessLoad;
import me.pigalala.trackexchange.processes.ProcessSave;
import me.pigalala.trackexchange.trackcomponents.TrackExchangeFile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Stack;

@CommandAlias("trackexchange|tex|tx")
public class CommandTrackExchange extends BaseCommand {

    @Subcommand("copy")
    @CommandCompletion("@track <saveas>")
    @CommandPermission("trackexchange.export")
    public static void onCopy(Player player, Track track, @Optional @Single String saveAs) {
        if (saveAs == null) {
            saveAs = track.getCommandName();
        }
        if (!TrackExchange.isButlerEnabled() && TrackExchangeFile.trackExchangeFileExists(saveAs)) {
            throw new ConditionFailedException("This trackexchange file already exists");
        }

        if (!saveAs.matches("[A-Za-z0-9_]+")) {
            throw new ConditionFailedException("You cannot save a trackexchange track with that name");
        }

        var process = new ProcessSave(player, track, saveAs);
        process.execute();
    }

    @Subcommand("paste")
    @CommandCompletion("<filename> <loadas>")
    @CommandPermission("trackexchange.import")
    public static void onPaste(Player player, String fileName, @Optional String loadAs) {
        fileName = fileName.replace(".trackexchange", "");
        if (loadAs == null) {
            loadAs = fileName;
        }

        if (!TrackExchange.isButlerEnabled() && !TrackExchangeFile.trackExchangeFileExists(fileName))
            throw new ConditionFailedException("This trackexchange file does not exist");

        if (TrackDatabase.trackNameNotAvailable(loadAs)) {
            throw new ConditionFailedException("A track with this name already exists");
        }

        if (!loadAs.matches("[A-Za-z0-9 ]+")) {
            throw new ConditionFailedException("You cannot load a trackexchange track with that name");
        }

        if(ApiUtilities.checkTrackName(loadAs))
            throw new ConditionFailedException("That name is not legal");

        var processLoad = new ProcessLoad(player, fileName, loadAs);
        processLoad.execute();

        // What the syntax
        processLoad.createInverse().ifPresent(TrackExchange.playerActions.computeIfAbsent(player.getUniqueId(), uuid -> new Stack<>())::push);
    }

    @Subcommand("undo")
    public static void onUndo(Player player) {
        var actions = TrackExchange.playerActions.get(player.getUniqueId());
        if (actions == null || actions.isEmpty()) {
            throw new ConditionFailedException("You have nothing to undo.");
        }

        actions.pop().run();
    }

    @Subcommand("reload")
    @CommandPermission("trackexchange.reload")
    void onReload(CommandSender sender) {
        TrackExchange.instance.reloadConfig();
        sender.sendMessage(Component.text("Reloaded config.", NamedTextColor.GREEN));
    }
}
