package com.abdullaharafat.AfkPool;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import net.md_5.bungee.api.ChatColor;

public class App extends JavaPlugin implements Listener {

    public String regionName;
    public long crateInterval;
    public long moneyInterval;
    public String crateName;
    public int int1;
    public int int2;

    @Override
    public void onEnable() {
        getLogger().info("AfkPool Version 1.1.8 enabled.");
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        regionName = getConfig().getString("region-name");
        crateInterval = getConfig().getLong("crate-interval");
        moneyInterval = getConfig().getLong("money-interval");
        crateName = getConfig().getString("crate-name");
        int1 = getConfig().getInt("integer-1");
        int2 = getConfig().getInt("integer-2");

        Runnable[] tasks = new Runnable[] {
                new Runnable() {
                    @Override
                    public void run() {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                                    .get(BukkitAdapter.adapt(player.getWorld()));
                            Location location = player.getLocation();
                            ApplicableRegionSet set = regionManager.getApplicableRegions(
                                    BlockVector3.at(location.getX(), location.getY(), location.getZ()));
                            for (ProtectedRegion region : set) {
                                if (region.getId().equalsIgnoreCase(regionName)) {
                                    int money = (int) (Math.random() * int1) + int2;
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                            "eco give " + player.getName() + " " + money);
                                    player.sendTitle(
                                            ChatColor.WHITE + "You have been given " + ChatColor.GREEN + "$" + money,
                                            "Given by AFK Pool", 10, 70, 20);
                                }
                            }
                        }
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                                    .get(BukkitAdapter.adapt(player.getWorld()));
                            Location location = player.getLocation();
                            ApplicableRegionSet set = regionManager.getApplicableRegions(
                                    BlockVector3.at(location.getX(), location.getY(), location.getZ()));
                            for (ProtectedRegion region : set) {
                                if (region.getId().equalsIgnoreCase(regionName)) {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                            "crate give " + player.getName() + " " + crateName);
                                    player.sendTitle(ChatColor.WHITE + "You have been given an " + ChatColor.YELLOW
                                            + "AFK" + ChatColor.WHITE + " Crate key", "Given by AFK Pool", 10, 70, 20);
                                }
                            }
                        }
                    }
                }
        };
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, tasks[0], 0L, moneyInterval);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, tasks[1], 0L, crateInterval);
    }

    @Override
    public void onDisable() {
        getLogger().info("AfkPool Disabled");
    }

    public static void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public void disablePlugin() {
        Bukkit.getPluginManager().disablePlugin(this);
    }

    public void enablePlugin() {
        Bukkit.getPluginManager().enablePlugin(this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("AfkPool")) {
            if (args.length == 0) {
                return false;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.GREEN + "/AfkPool reload - Reloads config");
                sender.sendMessage(ChatColor.GREEN + "/AfkPool test - Give tester money and crate");
                sender.sendMessage(ChatColor.GREEN + "/AfkPool values - Shows values");
                sender.sendMessage("--------------------------------");
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                disablePlugin();
                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.DARK_GREEN + "Disabled AfkPool");
                sender.sendMessage("--------------------------------");
                enabledPlugin();
                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.DARK_GREEN + "Enabled AfkPool");
                sender.sendMessage("--------------------------------");
                reloadConfig();
                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.DARK_GREEN + "Config reloaded!");
                sender.sendMessage("--------------------------------");
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("test")) {
                Player player = Bukkit.getPlayer(sender.getName());
                int money = (int) (Math.random() * int1) + int2;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "eco give " + player.getName() + " " + money);
                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.YELLOW + "Given tester money");
                sender.sendMessage("--------------------------------");
                player.sendTitle(
                        ChatColor.WHITE + "You have been given " + ChatColor.GREEN + "$" + money,
                        "Given by AFK", 10, 70, 20);

                wait(500);
                
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "crate give " + player.getName() + " " + crateName);
                sender.sendMessage(ChatColor.YELLOW + "Given tester crate");
                sender.sendMessage("--------------------------------");
                player.sendTitle(ChatColor.WHITE + "You have been given an " + ChatColor.YELLOW
                        + "AFK" + ChatColor.WHITE + " Crate key", "Given by AFK Pool", 10, 70, 20);
                return true;

            }

            if (args.length == 1 && args[0].equalsIgnoreCase("values")) {
                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.BLUE + "region-name: " + ChatColor.GOLD + regionName);
                sender.sendMessage(ChatColor.BLUE + "crate-name: " + ChatColor.GOLD + crateName);
                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.BLUE + "money-interval: " + ChatColor.GREEN + moneyInterval);
                sender.sendMessage(ChatColor.BLUE + "crate-interval: " + ChatColor.GREEN + crateInterval);
                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.BLUE + "integer-1: " + ChatColor.WHITE + int1);
                sender.sendMessage(ChatColor.BLUE + "integer-2: " + ChatColor.WHITE + int2);
                sender.sendMessage("--------------------------------");
                return true;
            }
        }
        return false;
    }

    private Set<Player> playersInRegion = new HashSet<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));
        Location locationOfEvent = event.getTo();
        ApplicableRegionSet set = regionManager.getApplicableRegions(
                BlockVector3.at(locationOfEvent.getX(), locationOfEvent.getY(), locationOfEvent.getZ()));
        boolean isInRegion = false;
        for (ProtectedRegion region : set) {
            if (region.getId().equalsIgnoreCase(regionName)) {
                isInRegion = true;
                break;
            }
        }
        if (isInRegion) {
            if (!playersInRegion.contains(player)) {
                playersInRegion.add(player);
                player.sendTitle(ChatColor.WHITE + "You have " + ChatColor.GREEN + "entered" + ChatColor.WHITE + " the "
                        + ChatColor.AQUA + "AFK" + ChatColor.WHITE + " Pool", null, 10, 70, 20);
            }
        } else {
            if (playersInRegion.contains(player)) {
                playersInRegion.remove(player);
                player.sendTitle(ChatColor.WHITE + "You have " + ChatColor.RED + "left" + ChatColor.WHITE + " the "
                        + ChatColor.AQUA + "AFK" + ChatColor.WHITE + " Pool", null, 10, 70, 20);
            }
        }
    }
}