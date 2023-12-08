package me.pigalala.trackexchange.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

public class Messages {

    private Messages() {}

    public static void sendSaveStarted(Player player, String trackName, String savedAs) {
        player.sendMessage(Component.text("Beginning track saving progress... (" + trackName + " -> " + savedAs + ".zip)", TextColor.color(0xF38AFF)));
    }

    public static void sendSaveFinished(Player player, String trackName, String savedAs) {
        player.sendMessage(Component.text("TrackExchange save process completed ('" + trackName + "' to '" + savedAs + ".zip')", TextColor.color(0xF38AFF)));
    }

    public static void sendLoadStarted(Player player, String fileName, String loadAs) {
        player.sendMessage(Component.text("Loading TrackExchange file '" + fileName + "' as '" + loadAs + "'", TextColor.color(0xF38AFF)));
    }

    public static void sendLoadFinished(Player player, String fileName, String loadAs) {
        player.sendMessage(Component.text("TrackExchange load process completed ('" + fileName + ".zip' -> '" + loadAs + "')", TextColor.color(0xF38AFF)));
    }

    public static void sendError(Player player, Exception exception) {
        player.sendMessage(Component.text("An error occurred: " + exception.getMessage(), NamedTextColor.RED));
    }
}
