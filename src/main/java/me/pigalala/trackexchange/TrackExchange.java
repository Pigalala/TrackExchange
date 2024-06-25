package me.pigalala.trackexchange;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Optional;
import java.util.Stack;
import java.util.UUID;

public final class TrackExchange extends JavaPlugin {
    public static final int TRACK_VERSION = 4;

    public static HashMap<UUID, Stack<Runnable>> playerActions = new HashMap<>();

    public static TrackExchange instance;
    private static TaskChainFactory taskChainFactory;

    @Override
    public void onEnable() {
        instance = this;
        taskChainFactory = BukkitTaskChainFactory.create(this);

        if(!getDataFolder().exists())
            getDataFolder().mkdir();

        PaperCommandManager paperCommandManager = new PaperCommandManager(this);
        paperCommandManager.getCommandCompletions().registerAsyncCompletion("track", context -> TrackDatabase.getTracksAsStrings(context.getPlayer()));
        paperCommandManager.getCommandContexts().registerContext(Track.class, getTrackContextResolver());
        paperCommandManager.registerCommand(new CommandTrackExchange());
    }

    public static TaskChain<?> newChain() {
        return taskChainFactory.newChain();
    }

    private static ContextResolver<Track, BukkitCommandExecutionContext> getTrackContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            Optional<Track> _track = TimingSystemAPI.getTrack(name);
            if (_track.isPresent())
                return _track.get();
            throw new InvalidCommandArgument(name + " is not a recognised track.", false);
        };
    }
}
