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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;
import java.util.Stack;
import java.util.UUID;

public final class TrackExchange extends JavaPlugin {
    public static final int TRACK_VERSION = 5;

    public static HashMap<UUID, Stack<Runnable>> playerActions = new HashMap<>();

    public static TrackExchange instance;
    private static TaskChainFactory taskChainFactory;

    @Override
    public void onEnable() {
        instance = this;
        taskChainFactory = BukkitTaskChainFactory.create(this);

        saveDefaultConfig();

        if (Files.notExists(getTracksPath())) {
            try {
                Files.createDirectory(getTracksPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        PaperCommandManager paperCommandManager = new PaperCommandManager(this);
        paperCommandManager.getCommandCompletions().registerAsyncCompletion("track", context -> TrackDatabase.getTracksAsStrings(context.getPlayer()));
        paperCommandManager.getCommandContexts().registerContext(Track.class, getTrackContextResolver());
        paperCommandManager.registerCommand(new CommandTrackExchange());

        moveTrackFiles();
    }

    public static TaskChain<?> newChain() {
        return taskChainFactory.newChain();
    }

    public static boolean isButlerEnabled() {
        return instance.getConfig().getBoolean("butler.enabled", false);
    }

    public static Optional<String> getButlerKey() {
        return Optional.ofNullable(instance.getConfig().getString("butler.key", null));
    }

    public static Optional<String> getButlerUrl() {
        return Optional.ofNullable(instance.getConfig().getString("butler.url", null));
    }

    public static Path getTracksPath() {
        return instance.getDataPath().resolve("tracks");
    }

    private void moveTrackFiles() {
        getDataPath().forEach(path -> {
            if (path.endsWith(".trackexchange")) {
                try {
                    Files.move(path, instance.getDataPath().resolve("tracks"));
                } catch (IOException e) {
                    getSLF4JLogger().error("Could not migrate {} to /tracks/", path, e);
                }
            }
        });
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
