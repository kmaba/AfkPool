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

    String regionName;
    long crateInterval;
    long moneyInterval;
    String crateName;
    String Subtitle;
    String moneyTitle;
    String crateTitle;
    String enteringTitle;
    String exitingTitle;
    String Command1;
    String Command2;
    Boolean Command1E;
    Boolean Command2E;
    int min;
    int max;

    String VersionNumber;

    JavaPlugin plugin;
    int resourceId;

    int moneyVal;
    int testMoneyVal;

    @Override
    public void onEnable() {
        getLogger().info("AfkPool Version 1.2.7 enabled.");
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();

        reload();

        new UpdateChecker(this, 108746).getVersion(version -> {
            if (this.getDescription().getVersion().equals(version)) {
                getLogger().info("There is not a new AfkPool update available, you are on the latest version ()"
                        + version + ").");
            } else {
                getLogger().severe("There is a new AfkPool update available. Please update to the latest version ("
                        + this.getDescription().getVersion() + " --> " + version + ").");
            }
            VersionNumber = version;
        });

        int pluginId = 18474;
        new Metrics(this, pluginId);

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
                                    if (moneyVal == 0) {
                                        moneyVal = min + (int) (Math.random() * ((max - min) + 1));
                                        Command1 = Command1.replace("%p", player.getName());
                                        Command1 = Command1.replace("%m", String.valueOf(moneyVal));
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                                Command1);
                                        moneyTitle = moneyTitle.replace("%m", String.valueOf(moneyVal));
                                        moneyTitle = format(moneyTitle);
                                        player.sendTitle(
                                                moneyTitle,
                                                Subtitle, 10, 70, 20);
                                        moneyVal = 0;
                                        reload();
                                    }
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
                                    Command2 = Command2.replace("%p", player.getName());
                                    Command2 = Command2.replace("%c", crateName);
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                            Command2);
                                    crateTitle = crateTitle.replace("%c", crateName);
                                    crateTitle = format(crateTitle);
                                    player.sendTitle(crateTitle, Subtitle, 10, 70, 20);
                                    reload();
                                }
                            }
                        }
                    }
                }
        };

        if (Command1E) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, tasks[0], 0L, moneyInterval);
        }
        if (Command2E) {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, tasks[1], 0L, crateInterval);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("AfkPool Disabled");
    }

    public static String format(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
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

    public void reload() {
        regionName = getConfig().getString("region-name");
        crateInterval = getConfig().getLong("command2-interval");
        moneyInterval = getConfig().getLong("command1-interval");
        crateName = getConfig().getString("crate-name");
        Subtitle = getConfig().getString("subtitle");
        moneyTitle = getConfig().getString("command1-title");
        crateTitle = getConfig().getString("command2-title");
        enteringTitle = getConfig().getString("entering-title");
        exitingTitle = getConfig().getString("exiting-title");
        Command1 = getConfig().getString("command-1");
        Command2 = getConfig().getString("command-2");
        Command1E = getConfig().getBoolean("command-1-enabled");
        Command2E = getConfig().getBoolean("command-2-enabled");
        min = getConfig().getInt("min");
        max = getConfig().getInt("max");

        reloadConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("AfkPool")) {
            if (args.length == 0) {
                return false;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.GREEN + "/AfkPool reload - Reloads config, also restarts the plugin.");
                sender.sendMessage(ChatColor.GREEN + "/AfkPool test - Give tester things set in the config");
                sender.sendMessage(ChatColor.GREEN + "/AfkPool values - Shows values of the config");
                sender.sendMessage(ChatColor.GREEN + "/AfkPool version - Shows the version number");
                sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "TIP: " + ChatColor.RESET + ChatColor.BLUE + "Use '/AfkPool' shortcut '/ap' for easier access to the plugins commands");
                sender.sendMessage("--------------------------------");
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("version")) {
                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.GOLD + "The version you have installed is " + ChatColor.YELLOW + this.getDescription().getVersion());
                sender.sendMessage(ChatColor.GOLD + "The version on Spigot is " + ChatColor.YELLOW + VersionNumber);
                if (this.getDescription().getVersion().equals(VersionNumber)) {
                    sender.sendMessage(ChatColor.GREEN + "You are on the latest version.");
                } else {
                    sender.sendMessage(ChatColor.RED + "You are not on the latest version!");
                }
                sender.sendMessage("--------------------------------");
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reload();
                saveDefaultConfig();
                reloadConfig();
                disablePlugin();
                enablePlugin();

                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.DARK_GREEN + "Config reloaded!");
                sender.sendMessage("--------------------------------");
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("test")) {
                Player player = Bukkit.getPlayer(sender.getName());
                if (testMoneyVal == 0) {
                    testMoneyVal = min + (int) (Math.random() * ((max - min) + 1));
                    Command1 = Command1.replace("%p", player.getName());
                    Command1 = Command1.replace("%m", String.valueOf(testMoneyVal));
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            Command1);
                    if (Command1E) {        
                    sender.sendMessage("--------------------------------");
                    sender.sendMessage(ChatColor.YELLOW + "Executed Command: " + ChatColor.GREEN + Command1);
                    sender.sendMessage("--------------------------------");
                    } else {
                        sender.sendMessage("--------------------------------");
                        sender.sendMessage(ChatColor.RED + "Command-1 is disabled in the config!");
                        sender.sendMessage(ChatColor.YELLOW + "Executed Command: " + ChatColor.GREEN + Command1);
                        sender.sendMessage("--------------------------------");
                    }
                    moneyTitle = moneyTitle.replace("%m", String.valueOf(testMoneyVal));
                    moneyTitle = format(moneyTitle);
                    player.sendTitle(
                            moneyTitle,
                            Subtitle, 10, 70, 20);

                    testMoneyVal = 0;
                }
                reload();
                wait(500);

                Command2 = Command2.replace("%p", player.getName());
                Command2 = Command2.replace("%c", crateName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        Command2);
                if (Command2E) {
                sender.sendMessage(ChatColor.YELLOW + "Executed Command: " + ChatColor.GREEN + Command2);
                sender.sendMessage("--------------------------------");
                } else {
                    sender.sendMessage(ChatColor.RED + "Command-2 is disabled in the config!");
                    sender.sendMessage(ChatColor.YELLOW + "Executed Command: " + ChatColor.GREEN + Command2);
                    sender.sendMessage("--------------------------------");
                }
                crateTitle = crateTitle.replace("%c", crateName);
                crateTitle = format(crateTitle);
                player.sendTitle(crateTitle, Subtitle, 10, 70, 20);
                Command2 = getConfig().getString(Command2);
                crateTitle = getConfig().getString(crateTitle);
                reload();
                return true;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("values")) {
                sender.sendMessage("--------------------------------");
                sender.sendMessage(
                        ChatColor.BLUE + "region-name: " + ChatColor.GOLD + getConfig().getString("region-name"));
                sender.sendMessage(
                        ChatColor.BLUE + "crate-name: " + ChatColor.GOLD + getConfig().getString("crate-name"));
                sender.sendMessage("--------------------------------");
                sender.sendMessage(
                        ChatColor.BLUE + "command-1: " + ChatColor.GOLD + getConfig().getString("command-1"));
                sender.sendMessage(
                        ChatColor.BLUE + "command-2: " + ChatColor.GOLD + getConfig().getString("command-2"));
                sender.sendMessage("--------------------------------");
                sender.sendMessage(
                        ChatColor.BLUE + "command-1-enabled: " + getConfig().getBoolean("command-1-enabled"));
                sender.sendMessage(
                        ChatColor.BLUE + "command-2-enabled: " + getConfig().getBoolean("command-2-enabled"));
                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.BLUE + "subtitle: " + ChatColor.GOLD + getConfig().getString("subtitle"));
                sender.sendMessage(
                        ChatColor.BLUE + "command1-title: " + ChatColor.GOLD + getConfig().getString("command1-title"));
                sender.sendMessage(
                        ChatColor.BLUE + "command2-title: " + ChatColor.GOLD + getConfig().getString("command2-title"));
                sender.sendMessage(
                        ChatColor.BLUE + "entering-title: " + ChatColor.GOLD + getConfig().getString("entering-title"));
                sender.sendMessage(
                        ChatColor.BLUE + "exiting-title: " + ChatColor.GOLD + getConfig().getString("exiting-title"));
                sender.sendMessage("--------------------------------");
                sender.sendMessage(
                        ChatColor.BLUE + "command1-interval: " + ChatColor.GREEN
                                + getConfig().getLong("command1-interval"));
                sender.sendMessage(
                        ChatColor.BLUE + "command2-interval: " + ChatColor.GREEN
                                + getConfig().getLong("command2-interval"));
                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.BLUE + "min: " + ChatColor.WHITE + getConfig().getInt("min"));
                sender.sendMessage(ChatColor.BLUE + "max: " + ChatColor.WHITE + getConfig().getInt("max"));
                sender.sendMessage("--------------------------------");
                return true;
            }
        }
        if (cmd.getName().equalsIgnoreCase("ap")) {
            if (args.length == 0) {
                return false;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.GREEN + "/ap reload - Reloads config, also restarts the plugin.");
                sender.sendMessage(ChatColor.GREEN + "/ap test - Give tester things set in the config");
                sender.sendMessage(ChatColor.GREEN + "/ap values - Shows values of the config");
                sender.sendMessage(ChatColor.GREEN + "/ap version - Shows the version number");
                sender.sendMessage("--------------------------------");
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("version")) {
                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.GOLD + "The version you have installed is " + ChatColor.YELLOW + this.getDescription().getVersion());
                sender.sendMessage(ChatColor.GOLD + "The version on Spigot is " + ChatColor.YELLOW + VersionNumber);
                if (this.getDescription().getVersion().equals(VersionNumber)) {
                    sender.sendMessage(ChatColor.GREEN + "You are on the latest version.");
                } else {
                    sender.sendMessage(ChatColor.RED + "You are not on the latest version!");
                }
                sender.sendMessage("--------------------------------");
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reload();
                saveDefaultConfig();
                reloadConfig();
                disablePlugin();
                enablePlugin();

                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.DARK_GREEN + "Config reloaded!");
                sender.sendMessage("--------------------------------");
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("test")) {
                Player player = Bukkit.getPlayer(sender.getName());
                if (testMoneyVal == 0) {
                    testMoneyVal = min + (int) (Math.random() * ((max - min) + 1));
                    Command1 = Command1.replace("%p", player.getName());
                    Command1 = Command1.replace("%m", String.valueOf(testMoneyVal));
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            Command1);
                    if (Command1E) {        
                    sender.sendMessage("--------------------------------");
                    sender.sendMessage(ChatColor.YELLOW + "Executed Command: " + ChatColor.GREEN + Command1);
                    sender.sendMessage("--------------------------------");
                    } else {
                        sender.sendMessage("--------------------------------");
                        sender.sendMessage(ChatColor.RED + "Command-1 is disabled in the config!");
                        sender.sendMessage(ChatColor.YELLOW + "Executed Command: " + ChatColor.GREEN + Command1);
                        sender.sendMessage("--------------------------------");
                    }
                    moneyTitle = moneyTitle.replace("%m", String.valueOf(testMoneyVal));
                    moneyTitle = format(moneyTitle);
                    player.sendTitle(
                            moneyTitle,
                            Subtitle, 10, 70, 20);

                    testMoneyVal = 0;
                }
                reload();
                wait(500);

                Command2 = Command2.replace("%p", player.getName());
                Command2 = Command2.replace("%c", crateName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        Command2);
                if (Command2E) {
                sender.sendMessage(ChatColor.YELLOW + "Executed Command: " + ChatColor.GREEN + Command2);
                sender.sendMessage("--------------------------------");
                } else {
                    sender.sendMessage(ChatColor.RED + "Command-2 is disabled in the config!");
                    sender.sendMessage(ChatColor.YELLOW + "Executed Command: " + ChatColor.GREEN + Command2);
                    sender.sendMessage("--------------------------------");
                }
                crateTitle = crateTitle.replace("%c", crateName);
                crateTitle = format(crateTitle);
                player.sendTitle(crateTitle, Subtitle, 10, 70, 20);
                Command2 = getConfig().getString(Command2);
                crateTitle = getConfig().getString(crateTitle);
                reload();
                return true;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("values")) {
                sender.sendMessage("--------------------------------");
                sender.sendMessage(
                        ChatColor.BLUE + "region-name: " + ChatColor.GOLD + getConfig().getString("region-name"));
                sender.sendMessage(
                        ChatColor.BLUE + "crate-name: " + ChatColor.GOLD + getConfig().getString("crate-name"));
                sender.sendMessage("--------------------------------");
                sender.sendMessage(
                        ChatColor.BLUE + "command-1: " + ChatColor.GOLD + getConfig().getString("command-1"));
                sender.sendMessage(
                        ChatColor.BLUE + "command-2: " + ChatColor.GOLD + getConfig().getString("command-2"));
                sender.sendMessage("--------------------------------");
                sender.sendMessage(
                        ChatColor.BLUE + "command-1-enabled: " + getConfig().getBoolean("command-1-enabled"));
                sender.sendMessage(
                        ChatColor.BLUE + "command-2-enabled: " + getConfig().getBoolean("command-2-enabled"));
                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.BLUE + "subtitle: " + ChatColor.GOLD + getConfig().getString("subtitle"));
                sender.sendMessage(
                        ChatColor.BLUE + "command1-title: " + ChatColor.GOLD + getConfig().getString("command1-title"));
                sender.sendMessage(
                        ChatColor.BLUE + "command2-title: " + ChatColor.GOLD + getConfig().getString("command2-title"));
                sender.sendMessage(
                        ChatColor.BLUE + "entering-title: " + ChatColor.GOLD + getConfig().getString("entering-title"));
                sender.sendMessage(
                        ChatColor.BLUE + "exiting-title: " + ChatColor.GOLD + getConfig().getString("exiting-title"));
                sender.sendMessage("--------------------------------");
                sender.sendMessage(
                        ChatColor.BLUE + "command1-interval: " + ChatColor.GREEN
                                + getConfig().getLong("command1-interval"));
                sender.sendMessage(
                        ChatColor.BLUE + "command2-interval: " + ChatColor.GREEN
                                + getConfig().getLong("command2-interval"));
                sender.sendMessage("--------------------------------");
                sender.sendMessage(ChatColor.BLUE + "min: " + ChatColor.WHITE + getConfig().getInt("min"));
                sender.sendMessage(ChatColor.BLUE + "max: " + ChatColor.WHITE + getConfig().getInt("max"));
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
                enteringTitle = format(enteringTitle);
                player.sendTitle(enteringTitle, null, 10, 70, 20);
            }
        } else {
            if (playersInRegion.contains(player)) {
                playersInRegion.remove(player);
                exitingTitle = format(exitingTitle);
                player.sendTitle(exitingTitle, null, 10, 70, 20);
            }
        }
    }
}