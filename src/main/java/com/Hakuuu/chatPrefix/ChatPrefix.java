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
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class ChatPrefix extends JavaPlugin implements Listener, CommandExecutor {

    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            this.luckPerms = LuckPermsProvider.get();
        }

        // Fix NullPointer Warnings by checking if command exists
        setupCommand("titles");
        setupCommand("titlecolor");
        setupCommand("titlereload");

        getServer().getPluginManager().registerEvents(this, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
        }
    }

    private void setupCommand(String name) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) cmd.setExecutor(this);
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

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String currentId = getTitleData(player, "active_title_id");
        if (currentId == null || currentId.equalsIgnoreCase("member")) return;

        String perm = getConfig().getString("prefixes." + currentId + ".permission", "prefixes.default");
        if (!player.hasPermission(perm)) {
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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {

        if (cmd.getName().equalsIgnoreCase("titlereload")) {
            if (sender.hasPermission("prefixes.reload") || !(sender instanceof Player)) {
                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "ChatPrefix Reloaded!");
                return true;
            }
            sender.sendMessage(ChatColor.RED + "No permission!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("titles")) {
            Player target;
            String choice;

            if (args.length >= 2 && (!(sender instanceof Player) || sender.hasPermission("prefixes.change.others"))) {
                target = Bukkit.getPlayer(args[0]);
                choice = args[1].toLowerCase();
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
            }
            else if (args.length >= 1 && sender instanceof Player player) {
                target = player;
                choice = args[0].toLowerCase();
            }
            else if (args.length == 0 && sender instanceof Player player) {
                showAvailableTitles(player);
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /titles <prefix> or /titles <player> <prefix>");
                return true;
            }

            if (getConfig().contains("prefixes." + choice)) {
                int targetWeight = getConfig().getInt("prefixes." + choice + ".weight", 0);
                String perm = getConfig().getString("prefixes." + choice + ".permission", "prefixes.default");
                int targetPower = getPlayerPower(target);

                if (!(sender instanceof Player) || (targetPower >= targetWeight && target.hasPermission(perm))) {
                    saveTitleData(target, "active_title_id", choice, choice);
                    saveTitleData(target, "custom_style", "", choice);
                    String rawPrefix = getConfig().getString("prefixes." + choice + ".prefix", choice);

                    target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6Prefix/title set to: &7[" + rawPrefix + "&7]"));
                    if (target != sender) {
                        sender.sendMessage(ChatColor.GREEN + "Successfully set " + target.getName() + "'s title to " + choice);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "That player does not have permission for that title!");
                }
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cThat title doesnt exist! &6Available prefixes/titles:"));
                if (sender instanceof Player p) showAvailableTitles(p);
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("titlecolor") && sender instanceof Player player) {
            handleTitleColor(player, args);
            return true;
        }
        return true;
    }

    private void handleTitleColor(Player player, String[] args) {
        String currentId = getTitleData(player, "active_title_id");
        if (currentId == null) currentId = "member";
        boolean isStaffTitle = getConfig().getStringList("sync-titles").contains(currentId);
        boolean canStyle = player.hasPermission("prefixes.bypass") || (isStaffTitle ? player.hasPermission("prefixes.style.staff") : player.hasPermission("prefixes.style.standard"));

        if (!canStyle) {
            player.sendMessage(ChatColor.RED + "No permission to style this title!");
            return;
        }
        if (args.length == 0) {
            player.sendMessage(ChatColor.GOLD + "Usage: /titlecolor <colored name>");
            return;
        }
        String input = String.join(" ", args);
        if (input.toLowerCase().matches(".*&[l-o rk].*")) {
            player.sendMessage(ChatColor.RED + "Formatting codes forbidden!");
            return;
        }

        String rawBase = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefixes." + currentId + ".prefix", "Member")));
        if (player.hasPermission("prefixes.bypass") || ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', input)).equals(rawBase)) {
            saveTitleData(player, "custom_style", input, currentId);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6New colors applied! Title: &7[" + input + "&7]"));
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6Input wrong! Match: " + ChatColor.WHITE + rawBase));
        }
    }

    private void showAvailableTitles(Player player) {
        int playerPower = getPlayerPower(player);
        List<String> prefixKeys = new ArrayList<>(getConfig().getStringList("prefix-list"));

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

        List<String> available = new ArrayList<>();
        for (String k : prefixKeys) {
            int titleWeight = getConfig().getInt("prefixes." + k + ".weight", 0);
            String perm = getConfig().getString("prefixes." + k + ".permission", "prefixes.default");
            if (playerPower >= titleWeight && player.hasPermission(perm)) {
                String raw = getConfig().getString("prefixes." + k + ".prefix", k);
                available.add(ChatColor.GRAY + "[" + ChatColor.translateAlternateColorCodes('&', raw) + ChatColor.GRAY + "]");
            }
        }
        player.sendMessage(ChatColor.GOLD + "Your available prefixes:");
        if (!available.isEmpty()) player.sendMessage(String.join(ChatColor.GOLD + ", ", available));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6Your current prefix: &7[" + getActivePrefix(player) + "&7]"));
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
        String configFormat = getConfig().getString("format");
        if (configFormat == null) configFormat = "&7[%chat_prefix%&7] %player_name%: %msg%";

        String format = configFormat.replace("%msg%", event.getMessage());
        format = PlaceholderAPI.setPlaceholders(event.getPlayer(), format);
        event.setFormat(ChatColor.translateAlternateColorCodes('&', format.replace("%", "%%")));
    }
}