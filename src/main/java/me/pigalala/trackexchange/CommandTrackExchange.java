package me.pigalala.trackexchange;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.pigalala.trackexchange.processes.ProcessLoad;
import me.pigalala.trackexchange.processes.ProcessSave;
import me.pigalala.trackexchange.trackcomponents.TrackExchangeFile;
import org.bukkit.entity.Player;

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

        new ProcessSave(player, track, saveAs).execute();
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

        new ProcessLoad(player, fileName, loadAs).execute();
    }
}
