package com.abdullaharafat.AfkPool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    private Map<String, CommandConfig> commands;
    private Map<String, Set<Player>> playersInRegions;

    private String Subtitle;
    private String enteringTitle;
    private String exitingTitle;

    private String VersionNumber;

    private int pluginId;

    @Override
    public void onEnable() {
        getLogger().info("AfkPool Version 2.0.0 enabled.");
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();

        reload();

        new UpdateChecker(this, 108746).getVersion(version -> {
            if (this.getDescription().getVersion().equals(version)) {
                getLogger().info("There is not a new AfkPool update available, you are on the latest version ("
                        + version + ").");
            } else {
                getLogger().severe("There is a new AfkPool update available. Please update to the latest version ("
                        + this.getDescription().getVersion() + " --> " + version + ").");
            }
            VersionNumber = version;
        });

        pluginId = 18474;
        new Metrics(this, pluginId);

        for (CommandConfig command : commands.values()) {
            if (command.isEnabled()) {
                Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> executeCommandForRegion(command), 0L, command.getInterval());
            }
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
        commands = new HashMap<>();
        playersInRegions = new HashMap<>();

        Subtitle = getConfig().getString("subtitle");
        enteringTitle = getConfig().getString("entering-title");
        exitingTitle = getConfig().getString("exiting-title");

        if (getConfig().isConfigurationSection("commands")) {
            for (String commandKey : getConfig().getConfigurationSection("commands").getKeys(false)) {
                CommandConfig commandConfig = new CommandConfig(
                    commandKey,
                    getConfig().getString("commands." + commandKey + ".region-name"),
                    getConfig().getLong("commands." + commandKey + ".interval"),
                    getConfig().getString("commands." + commandKey + ".command"),
                    getConfig().getString("commands." + commandKey + ".title"),
                    getConfig().getBoolean("commands." + commandKey + ".enabled"),
                    getConfig().getInt("commands." + commandKey + ".min"),
                    getConfig().getInt("commands." + commandKey + ".max"),
                    getConfig().getDouble("commands." + commandKey + ".multiplier", 1.0) // Default multiplier is 1.0
                );
                commands.put(commandKey, commandConfig);
                playersInRegions.put(commandKey, new HashSet<>());
            }
        } else {
            getLogger().severe("No commands found in the config.yml file.");
        }

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
                for (CommandConfig command : commands.values()) {
                    if (command.isEnabled()) {
                        executeCommandForPlayer(player, command);
                        wait(500);
                    }
                }
                return true;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("values")) {
                sender.sendMessage("--------------------------------");
                for (String commandKey : commands.keySet()) {
                    CommandConfig command = commands.get(commandKey);
                    sender.sendMessage(ChatColor.BLUE + "Command: " + ChatColor.GOLD + command.getKey());
                    sender.sendMessage(ChatColor.BLUE + "region-name: " + ChatColor.GOLD + command.getRegionName());
                    sender.sendMessage(ChatColor.BLUE + "command: " + ChatColor.GOLD + command.getCommand());
                    sender.sendMessage(ChatColor.BLUE + "title: " + ChatColor.GOLD + command.getTitle());
                    sender.sendMessage(ChatColor.BLUE + "enabled: " + command.isEnabled());
                    sender.sendMessage(ChatColor.BLUE + "interval: " + ChatColor.GREEN + command.getInterval());
                    sender.sendMessage(ChatColor.BLUE + "min: " + ChatColor.WHITE + command.getMin());
                    sender.sendMessage(ChatColor.BLUE + "max: " + ChatColor.WHITE + command.getMax());
                    sender.sendMessage(ChatColor.BLUE + "multiplier: " + ChatColor.WHITE + command.getMultiplier());
                    sender.sendMessage("--------------------------------");
                }
                return true;
            }
        }
        return false;
    }

    private void executeCommandForRegion(CommandConfig commandConfig) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(player.getWorld()));
            Location location = player.getLocation();
            ApplicableRegionSet set = regionManager.getApplicableRegions(
                    BlockVector3.at(location.getX(), location.getY(), location.getZ()));
            boolean isInRegion = false;
            for (ProtectedRegion region : set) {
                if (region.getId().equalsIgnoreCase(commandConfig.getRegionName())) {
                    isInRegion = true;
                    break;
                }
            }
            if (isInRegion) {
                if (!playersInRegions.get(commandConfig.getKey()).contains(player)) {
                    playersInRegions.get(commandConfig.getKey()).add(player);
                    String formattedTitle = commandConfig.getTitle();
                    formattedTitle = format(formattedTitle);
                    player.sendTitle(formattedTitle, Subtitle, 10, 70, 20);
                }
                executeCommandForPlayer(player, commandConfig);
            } else {
                if (playersInRegions.get(commandConfig.getKey()).contains(player)) {
                    playersInRegions.get(commandConfig.getKey()).remove(player);
                }
            }
        }
    }

    private void executeCommandForPlayer(Player player, CommandConfig commandConfig) {
        if (commandConfig.isEnabled()) {
            int value = commandConfig.getMin() + (int) (Math.random() * ((commandConfig.getMax() - commandConfig.getMin()) + 1));
            if (player.hasPermission("afkpool.bonus")) {
                value *= commandConfig.getMultiplier();
            }
            String command = commandConfig.getCommand().replace("%p", player.getName());
            command = command.replace("%m", String.valueOf(value));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            String formattedTitle = commandConfig.getTitle().replace("%m", String.valueOf(value));
            formattedTitle = format(formattedTitle);
            player.sendTitle(formattedTitle, Subtitle, 10, 70, 20);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));
        Location locationOfEvent = event.getTo();
        ApplicableRegionSet set = regionManager.getApplicableRegions(
                BlockVector3.at(locationOfEvent.getX(), locationOfEvent.getY(), locationOfEvent.getZ()));

        for (CommandConfig commandConfig : commands.values()) {
            boolean isInRegion = false;
            for (ProtectedRegion region : set) {
                if (region.getId().equalsIgnoreCase(commandConfig.getRegionName())) {
                    isInRegion = true;
                    break;
                }
            }
            if (isInRegion) {
                if (!playersInRegions.get(commandConfig.getKey()).contains(player)) {
                    playersInRegions.get(commandConfig.getKey()).add(player);
                    String formattedEnteringTitle = format(enteringTitle);
                    player.sendTitle(formattedEnteringTitle, null, 10, 70, 20);
                }
            } else {
                if (playersInRegions.get(commandConfig.getKey()).contains(player)) {
                    playersInRegions.get(commandConfig.getKey()).remove(player);
                    String formattedExitingTitle = format(exitingTitle);
                    player.sendTitle(formattedExitingTitle, null, 10, 70, 20);
                }
            }
        }
    }

    private static class CommandConfig {
        private String key;
        private String regionName;
        private long interval;
        private String command;
        private String title;
        private boolean enabled;
        private int min;
        private int max;
        private double multiplier;

        public CommandConfig(String key, String regionName, long interval, String command, String title, boolean enabled, int min, int max, double multiplier) {
            this.key = key;
            this.regionName = regionName;
            this.interval = interval;
            this.command = command;
            this.title = title;
            this.enabled = enabled;
            this.min = min;
            this.max = max;
            this.multiplier = multiplier;
        }

        public String getKey() {
            return key;
        }

        public String getRegionName() {
            return regionName;
        }

        public long getInterval() {
            return interval;
        }

        public String getCommand() {
            return command;
        }

        public String getTitle() {
            return title;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        public double getMultiplier() {
            return multiplier;
        }
    }
}
