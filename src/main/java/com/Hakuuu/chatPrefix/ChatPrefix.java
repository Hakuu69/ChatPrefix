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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

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

    private int getPlayerPower(Player player) {
        if (luckPerms == null) return 1;
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return 1;

        String weightStr = user.getCachedData().getMetaData().getMetaValue("weight");
        if (weightStr != null) {
            try { return Integer.parseInt(weightStr); } catch (NumberFormatException e) { return 1; }
        }
        return 1;
    }

    public String getHighestRank(Player player) {
        int playerPower = getPlayerPower(player);
        String highestId = "member";
        int highestWeight = -1;

        List<String> prefixKeys = getConfig().getStringList("prefix-list");
        for (String k : prefixKeys) {
            int titleWeight = getConfig().getInt("prefixes." + k + ".weight", 0);
            String perm = getConfig().getString("prefixes." + k + ".permission");
            if (player.hasPermission(perm) && playerPower >= titleWeight && titleWeight > highestWeight) {
                highestWeight = titleWeight;
                highestId = k;
            }
        }
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefixes." + highestId + ".prefix", "&eMember"));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String currentId = getTitleData(player, "active_title_id");
        if (currentId == null || currentId.equalsIgnoreCase("member")) return;

        String perm = getConfig().getString("prefixes." + currentId + ".permission");
        if (perm != null && !player.hasPermission(perm)) {
            saveTitleData(player, "active_title_id", "member", "member");
            saveTitleData(player, "custom_style", "", "member");
        }
    }

    private void saveTitleData(Player player, String key, String value, String titleId) {
        if (luckPerms != null) {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                user.data().clear(node -> node instanceof MetaNode && ((MetaNode) node).getMetaKey().equalsIgnoreCase(key));
                if (getConfig().getBoolean("sync-to-luckperms", true) && getConfig().getStringList("sync-titles").contains(titleId)) {
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
            int playerPower = getPlayerPower(player);
            List<String> prefixKeys = new ArrayList<>(getConfig().getStringList("prefix-list"));

            // --- SMART SORT: Visual order (Member first, Staff last) ---
            prefixKeys.sort((a, b) -> {
                if (a.equalsIgnoreCase("member")) return -1;
                if (b.equalsIgnoreCase("member")) return 1;
                int weightA = getConfig().getInt("prefixes." + a + ".weight", 0);
                int weightB = getConfig().getInt("prefixes." + b + ".weight", 0);
                boolean isStaffA = weightA >= 100;
                boolean isStaffB = weightB >= 100;
                if (isStaffA && !isStaffB) return 1;
                if (!isStaffA && isStaffB) return -1;
                return Integer.compare(weightA, weightB);
            });

            // Prepare the display list of available titles
            List<String> available = new ArrayList<>();
            for (String k : prefixKeys) {
                int titleWeight = getConfig().getInt("prefixes." + k + ".weight", 0);
                String perm = getConfig().getString("prefixes." + k + ".permission");
                if (playerPower >= titleWeight && player.hasPermission(perm)) {
                    String raw = getConfig().getString("prefixes." + k + ".prefix", k);
                    available.add(ChatColor.GRAY + "[" + ChatColor.translateAlternateColorCodes('&', raw) + ChatColor.GRAY + "]");
                }
            }

            // Case 1: Simple list view
            if (args.length == 0) {
                player.sendMessage(ChatColor.GOLD + "Your available prefixes:");
                if (!available.isEmpty()) player.sendMessage(String.join(ChatColor.GOLD + ", ", available));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6Your current prefix: &7[" + getActivePrefix(player) + "&7]"));
                return true;
            }

            // Case 2: Attempting to select a title
            String choice = args[0].toLowerCase();
            if (getConfig().contains("prefixes." + choice)) {
                int targetWeight = getConfig().getInt("prefixes." + choice + ".weight", 0);
                String perm = getConfig().getString("prefixes." + choice + ".permission");

                if (playerPower >= targetWeight && player.hasPermission(perm)) {
                    saveTitleData(player, "active_title_id", choice, choice);
                    saveTitleData(player, "custom_style", "", choice);
                    String rawPrefix = getConfig().getString("prefixes." + choice + ".prefix");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6Prefix/title set to: &7[" + rawPrefix + "&7]"));
                } else {
                    player.sendMessage(ChatColor.RED + "No permission!");
                }
            } else {
                // Case 3: Title doesn't exist - Show Error + Available List
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cThat title doesnt exist! &6Available prefixes/titles:"));
                if (!available.isEmpty()) player.sendMessage(String.join(ChatColor.GOLD + ", ", available));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6Your current prefix: &7[" + getActivePrefix(player) + "&7]"));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("titlecolor")) {
            String currentId = getTitleData(player, "active_title_id");
            if (currentId == null) currentId = "member";

            boolean isStaffTitle = getConfig().getStringList("sync-titles").contains(currentId);
            boolean canStyle = player.hasPermission("prefixes.bypass") ||
                    (isStaffTitle ? player.hasPermission("prefixes.style.staff") : player.hasPermission("prefixes.style.standard"));

            if (!canStyle) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou don't have permission to style this type of title!"));
                return true;
            }

            if (args.length == 0) {
                player.sendMessage(ChatColor.GOLD + "Usage: /titlecolor <colored name>");
                return true;
            }

            String input = String.join(" ", args);
            if (input.toLowerCase().matches(".*&[l-o rk].*")) {
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
            if (ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', input)).equals(rawBase)) {
                saveTitleData(player, "custom_style", input, currentId);
                String preview = ChatColor.translateAlternateColorCodes('&', input);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6New colors applied correctly! Your new title is &7[" + preview + "&7]"));
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6The input title is wrong! Please match: " + ChatColor.WHITE + rawBase));
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