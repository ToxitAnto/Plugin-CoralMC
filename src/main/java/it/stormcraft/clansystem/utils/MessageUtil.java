package it.stormcraft.clansystem.utils;

import org.bukkit.command.CommandSender;

public final class MessageUtil {

    private static String prefix = "§8[§6Clan§8] §r";

    private MessageUtil() {}

    public static void setPrefix(String rawPrefix) {
        prefix = rawPrefix.replace("&", "§");
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(prefix + message);
    }

    public static String colorize(String text) {
        return text.replace("&", "§");
    }
}
