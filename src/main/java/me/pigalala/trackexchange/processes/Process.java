package me.pigalala.trackexchange.processes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public interface Process {

    void execute();

    String processName();

    default Component getProcessStartText() {
        return Component.text("Beginning process '" + processName() + "'.", TextColor.color(0xF38AFF));
    }

    default Component getProcessFinishText(long timeTaken) {
        return Component.text("Process '" + processName() + "' completed in " + timeTaken + "ms.", NamedTextColor.GREEN);
    }

    default Component getProcessFinishExceptionallyText() {
        return Component.text("Process '" + processName() + "' completed exceptionally. Check any error messages above for more info.", NamedTextColor.RED);
    }

    default Component getStageBeginText(String stage) {
        return Component.text("Beginning stage '" + stage +"'", TextColor.color(0xF38AFF));
    }

    default Component getStageFinishText(String stage, long timeTaken) {
        return Component.text("Completed stage '" + stage + "' in " + timeTaken + "ms.", NamedTextColor.GREEN);
    }

    default Component getStageFinishExceptionallyText(String stage, Throwable cause) {
        return Component.text("Stage '" + stage + "' completed exceptionally.", NamedTextColor.RED).hoverEvent(Component.text(cause.getMessage(), NamedTextColor.RED));
    }
}
