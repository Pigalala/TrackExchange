package me.pigalala.trackexchange.processes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

public abstract class Process {

    protected final Player player;
    private final String processName;

    public Process(Player player, String processName) {
        this.player = player;
        this.processName = processName.toUpperCase();
    }

    protected void notifyPlayer(Component text) {
        if(player == null)
            return;
        player.sendMessage(text);
    }

    protected void notifyProcessStartText() {
        notifyPlayer(Component.text("Beginning process '" + processName + "'.", TextColor.color(0xF38AFF)));
    }

    protected void notifyProcessFinishText(long timeTaken) {
        notifyPlayer(Component.text("Process '" + processName + "' completed in " + timeTaken + "ms.", NamedTextColor.GREEN));
    }

    protected void notifyProcessFinishExceptionallyText() {
        notifyPlayer(Component.text("Process '" + processName + "' completed exceptionally. Check any error messages above for more info.", NamedTextColor.RED));
    }

    protected void notifyStageBeginText(String stage) {
        notifyPlayer(Component.text("Beginning stage '" + stage +"'", TextColor.color(0xF38AFF)));
    }

    protected void notifyStageFinishText(String stage, long timeTaken) {
        notifyPlayer(Component.text("Completed stage '" + stage + "' in " + timeTaken + "ms.", NamedTextColor.GREEN));
    }

    protected void notifyStageFinishExceptionallyText(String stage, Throwable cause) {
        cause.printStackTrace();
        String info = cause.getMessage();
        notifyPlayer(Component.text("Stage '" + stage + "' completed exceptionally.", NamedTextColor.RED).hoverEvent(Component.text(info == null ? "No info." : info, NamedTextColor.RED)));
    }

    public abstract void execute();
}
