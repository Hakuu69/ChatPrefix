package com.Hakuuu.chatPrefix;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PAPIExpansion extends PlaceholderExpansion {
    private final ChatPrefix plugin;
    public PAPIExpansion(ChatPrefix plugin) { this.plugin = plugin; }
    @Override
    public @NotNull String getIdentifier() { return "chat"; }
    @Override
    public @NotNull String getAuthor() { return "Hakuuu"; }
    @Override
    public @NotNull String getVersion() { return "1.0.0"; }
    @Override
    public boolean persist() { return true; }
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player != null && player.isOnline() && params.equalsIgnoreCase("prefix")) {
            return plugin.getActivePrefix((Player) player);
        }
        return null;
    }
}