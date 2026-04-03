package com.Hakuuu.chatPrefix;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.MetaNode;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent; // Added Import
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;
import java.util.ArrayList;

public class ChatPrefix extends JavaPlugin implements Listener, CommandExecutor {

    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            this.luckPerms = LuckPermsProvider.get();
        }
        getCommand("titles").setExecutor(this);
        getCommand("titlecolor").setExecutor(this);
        getCommand("titlereload").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
        }
    }

    // --- AUTO-DEMOTION FIX: Reset title on join if permission is lost ---
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String currentId = getTitleData(player, "active_title_id");

        // If current title is member (or null), do nothing
        if (currentId == null || currentId.equalsIgnoreCase("member")) return;

        // Check if player still has the permission for the title they have selected
        String perm = getConfig().getString("prefixes." + currentId + ".permission");
        if (perm != null && !player.hasPermission(perm)) {
            // Force reset to member
            saveTitleData(player, "active_title_id", "member", "member");
            saveTitleData(player, "custom_style", "", "member");
            player.sendMessage(ChatColor.RED + "Your title was reset because you no longer have access to it.");
        }
    }

    private void saveTitleData(Player player, String key, String value, String titleId) {
        boolean useLP = getConfig().getBoolean("sync-to-luckperms", true);
        List<String> syncList = getConfig().getStringList("sync-titles");

        if (luckPerms != null) {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                // Wipe old global data first to prevent ghosting
                user.data().clear(node -> node instanceof MetaNode && ((MetaNode) node).getMetaKey().equalsIgnoreCase(key));

                // Only write NEW global data if the title is in the sync list
                if (useLP && syncList.contains(titleId)) {
                    user.data().add(MetaNode.builder(key, value).build());
                }
                luckPerms.getUserManager().saveUser(user);
            }
        }

        getConfig().set("players." + player.getUniqueId() + "." + key, value);
        saveConfig();
    }

    private String getTitleData(Player player, String key) {
        if (luckPerms != null) {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                String val = user.getCachedData().getMetaData().getMetaValue(key);
                if (val != null && !val.isEmpty()) return val;
            }
        }
        return getConfig().getString("players." + player.getUniqueId() + "." + key);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (cmd.getName().equalsIgnoreCase("titles")) {
            if (args.length == 0) {
                player.sendMessage(ChatColor.GOLD + "Your available prefixes:");
                List<String> prefixKeys = getConfig().getStringList("prefix-list");
                List<String> available = new ArrayList<>();

                for (String k : prefixKeys) {
                    String perm = getConfig().getString("prefixes." + k + ".permission");
                    if (perm != null && player.hasPermission(perm)) {
                        String raw = getConfig().getString("prefixes." + k + ".prefix", k);
                        available.add(ChatColor.GRAY + "[" + ChatColor.translateAlternateColorCodes('&', raw) + ChatColor.GRAY + "]");
                    }
                }

                if (available.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "You don't have any special titles available!");
                } else {
                    player.sendMessage(String.join(ChatColor.GOLD + ", ", available));
                }

                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6Your current prefix: &7[" + getActivePrefix(player) + "&7]"));
                return true;
            }

            String choice = args[0].toLowerCase();
            if (getConfig().contains("prefixes." + choice)) {
                String perm = getConfig().getString("prefixes." + choice + ".permission");
                if (player.hasPermission(perm)) {
                    saveTitleData(player, "active_title_id", choice, choice);
                    saveTitleData(player, "custom_style", "", choice);
                    String rawPrefix = getConfig().getString("prefixes." + choice + ".prefix");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6Prefix/title set to: &7[" + rawPrefix + "&7]"));
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission for this title!");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Title not found.");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("titlecolor")) {
            String currentId = getTitleData(player, "active_title_id");
            if (currentId == null) currentId = "member";

            List<String> syncList = getConfig().getStringList("sync-titles");
            boolean isSynced = syncList.contains(currentId);

            boolean canStyle = false;
            if (player.hasPermission("prefixes.bypass")) canStyle = true;
            else if (isSynced && player.hasPermission("prefixes.style.sync")) canStyle = true;
            else if (!isSynced && player.hasPermission("prefixes.style.standard")) canStyle = true;

            if (!canStyle) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cInsufficient Permission!"));
                return true;
            }

            if (args.length == 0) {
                player.sendMessage(ChatColor.GOLD + "Set a custom color for your title.");
                return true;
            }

            String input = String.join(" ", args);
            if (input.toLowerCase().matches(".*&[lno mk].*")) {
                player.sendMessage(ChatColor.RED + "Formatting codes (bold/italic/etc) are forbidden!");
                return true;
            }

            if (player.hasPermission("prefixes.bypass")) {
                saveTitleData(player, "custom_style", input, currentId);
                String preview = ChatColor.translateAlternateColorCodes('&', input);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b[Bypass Mode] &7Style applied: &7[" + preview + "&7]"));
                return true;
            }

            String rawBase = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefixes." + currentId + ".prefix", "Member")));
            String strippedInput = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', input));

            if (strippedInput.equals(rawBase)) {
                saveTitleData(player, "custom_style", input, currentId);
                String preview = ChatColor.translateAlternateColorCodes('&', input);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6New colors applied correctly! Your new title is &7[" + preview + "&7]"));
            } else {
                player.sendMessage(ChatColor.GOLD + "The input title is wrong! Please match: " + ChatColor.WHITE + rawBase);
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("titlereload") && sender.hasPermission("prefixes.reload")) {
            reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "ChatPrefix Reloaded!");
            return true;
        }
        return true;
    }

    public String getActivePrefix(Player player) {
        String custom = getTitleData(player, "custom_style");
        if (custom != null && !custom.isEmpty()) return ChatColor.translateAlternateColorCodes('&', custom);
        String id = getTitleData(player, "active_title_id");
        if (id == null) id = "member";
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefixes." + id + ".prefix", "&eMember"));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        String format = getConfig().getString("format").replace("%msg%", event.getMessage());
        format = PlaceholderAPI.setPlaceholders(event.getPlayer(), format);
        event.setFormat(ChatColor.translateAlternateColorCodes('&', format.replace("%", "%%")));
    }
}