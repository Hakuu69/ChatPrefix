package com.Hakuuu.chatPrefix;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ColorCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // Solid gray line for the header sides (increased to make it longer)
        String sideLine = ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                             ";

        // 1. Header
        String header = sideLine + ChatColor.RESET + " " +
                ChatColor.RED + "C" +
                ChatColor.GOLD + "O" +
                ChatColor.YELLOW + "L" +
                ChatColor.GREEN + "O" +
                ChatColor.AQUA + "R" +
                ChatColor.LIGHT_PURPLE + "S" +
                ChatColor.RESET + " " + sideLine;

        sender.sendMessage(header);

        // 2. All Colors
        sender.sendMessage(ChatColor.GOLD + "Colors: " +
                ChatColor.DARK_BLUE + "&1 " + ChatColor.DARK_GREEN + "&2 " +
                ChatColor.DARK_AQUA + "&3 " + ChatColor.DARK_RED + "&4 " +
                ChatColor.DARK_PURPLE + "&5 " + ChatColor.GOLD + "&6 " +
                ChatColor.GRAY + "&7 " + ChatColor.DARK_GRAY + "&8 " +
                ChatColor.BLUE + "&9 " + ChatColor.GREEN + "&a " +
                ChatColor.AQUA + "&b " + ChatColor.RED + "&c " +
                ChatColor.LIGHT_PURPLE + "&d " + ChatColor.YELLOW + "&e " +
                ChatColor.WHITE + "&f");

        // 3. Special Line (No space in magic part)
        sender.sendMessage(ChatColor.GOLD + "Special: " +
                ChatColor.WHITE + "&k(" + ChatColor.MAGIC + "A" + ChatColor.RESET + ChatColor.WHITE + ") " + ChatColor.RESET +
                ChatColor.BOLD + "&l"+ " " + ChatColor.RESET +
                ChatColor.STRIKETHROUGH + "&m" + " " + ChatColor.RESET +
                ChatColor.UNDERLINE + "&n" + " " + ChatColor.RESET +
                ChatColor.ITALIC + "&o");

        // 4. Balanced Footer
        // I added a massive amount of spaces here to ensure it catches up to the header length.
        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                                                     ");

        return true;
    }
}