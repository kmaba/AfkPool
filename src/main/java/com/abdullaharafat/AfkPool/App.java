package com.abdullaharafat.AfkPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class App extends JavaPlugin implements Listener {

    private static PlatformScheduler scheduler;

    private Map<String, CommandConfig> commands;
    private Map<String, Set<Player>> playersInRegions;
    private Map<String, Long> nextRewardTimes;
    private final List<WrappedTask> tasks = new ArrayList<>();

    private String Subtitle;
    private String enteringTitle;
    private String exitingTitle;

    private boolean timerEnabled;
    private String timerFormat;

    private String VersionNumber;

    private int pluginId;

    @Override
    public void onEnable() {
        FoliaLib foliaLib = new FoliaLib(this);
        scheduler = foliaLib.getScheduler();

        getLogger().info("AfkPool Version " + getDescription().getVersion() + " enabled.");
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();

        reload();

        new UpdateChecker(this, 108746).getVersion(version -> {
            if (this.getDescription().getVersion().equals(version)) {
                getLogger().info("You are running the latest AfkPool version (" + version + ").");
            } else {
                getLogger().warning("A different AfkPool version (" + version + ") is available on SpigotMC; you are running "
                        + this.getDescription().getVersion() + ".");
            }
            VersionNumber = version;
        });

        pluginId = 18474;
        new Metrics(this, pluginId);

        startTasks();
    }

    @Override
    public void onDisable() {
        getLogger().info("AfkPool Disabled");
    }

    public static PlatformScheduler scheduler() {
        return scheduler;
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

    public void startTasks() {
        long now = System.currentTimeMillis();
        for (CommandConfig command : commands.values()) {
            if (command.isEnabled()) {
                nextRewardTimes.put(command.getKey(), now + command.getInterval() * 50L);
                tasks.add(scheduler.runTimer(() -> executeCommandForRegion(command), 1L, command.getInterval()));
            }
        }
        if (timerEnabled) {
            tasks.add(scheduler.runTimer(this::updateRewardTimers, 20L, 20L));
        }
        getLogger().info("Scheduled " + commands.values().stream().filter(CommandConfig::isEnabled).count()
                + " reward task(s) and " + (timerEnabled ? "the" : "no")
                + " countdown task (" + tasks.size() + " total).");
    }

    public void stopTasks() {
        for (WrappedTask task : tasks) {
            try {
                task.cancel();
            } catch (Throwable t) {
                getLogger().warning("Failed to cancel scheduled task: " + t);
            }
        }
        tasks.clear();
    }

    public int getTaskCount() {
        return tasks.size();
    }

    public void reload() {
        stopTasks();
        commands = new HashMap<>();
        playersInRegions = new HashMap<>();
        nextRewardTimes = new HashMap<>();

        Subtitle = defaultIfEmpty(getConfig().getString("subtitle"), "&7You are in the AFK Pool!");
        enteringTitle = defaultIfEmpty(getConfig().getString("entering-title"), "&aWelcome to the AFK Pool!");
        exitingTitle = defaultIfEmpty(getConfig().getString("exiting-title"), "&cLeaving the AFK Pool!");

        timerEnabled = getConfig().getBoolean("timer.enabled", true);
        timerFormat = defaultIfEmpty(getConfig().getString("timer.format"), "&7Next reward in &e%time%");

        if (getConfig().isConfigurationSection("commands")) {
            for (String commandKey : getConfig().getConfigurationSection("commands").getKeys(false)) {
                String regionName = getConfig().getString("commands." + commandKey + ".region-name");
                long interval = getConfig().getLong("commands." + commandKey + ".interval");
                if (regionName == null || regionName.isEmpty()) {
                    getLogger().warning("Command '" + commandKey + "' has no region-name set, skipping it. Fix your config.yml.");
                    continue;
                }
                if (interval <= 0) {
                    getLogger().warning("Command '" + commandKey + "' has an invalid interval (" + interval + "), skipping it. Interval must be a positive number of ticks.");
                    continue;
                }
                CommandConfig commandConfig = new CommandConfig(
                    commandKey,
                    regionName,
                    interval,
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

        warnMissingRegions();
        reloadConfig();
    }

    private String defaultIfEmpty(String value, String fallback) {
        return (value == null || value.isEmpty()) ? fallback : value;
    }

    private void warnMissingRegions() {
        try {
            Set<String> knownRegions = new HashSet<>();
            boolean anyWorldLoaded = false;
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                        .get(BukkitAdapter.adapt(world));
                if (manager == null) {
                    continue;
                }
                anyWorldLoaded = true;
                for (ProtectedRegion region : manager.getRegions().values()) {
                    knownRegions.add(region.getId());
                }
            }
            if (!anyWorldLoaded || knownRegions.isEmpty()) {
                return;
            }
            for (CommandConfig command : commands.values()) {
                if (!command.isEnabled()) {
                    continue;
                }
                if (!knownRegions.contains(command.getRegionName())
                        && !knownRegions.contains(command.getRegionName().toLowerCase())) {
                    getLogger().warning("WorldGuard region '" + command.getRegionName()
                            + "' (from '" + command.getKey() + "') was not found in any loaded world."
                            + " Rewards will never trigger until 'region-name' matches an existing WorldGuard region.");
                }
            }
        } catch (Throwable t) {
            getLogger().warning("Could not verify configured regions against WorldGuard: " + t.getMessage());
        }
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
                saveDefaultConfig();
                reloadConfig();
                reload();
                startTasks();

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
                sender.sendMessage(ChatColor.BLUE + "scheduled-tasks: " + ChatColor.WHITE + getTaskCount());
                return true;
            }
        }
        return false;
    }

    private void executeCommandForRegion(CommandConfig commandConfig) {
        nextRewardTimes.put(commandConfig.getKey(),
                System.currentTimeMillis() + commandConfig.getInterval() * 50L);
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean isInRegion = isPlayerInRegion(player, commandConfig.getRegionName());

            if (isInRegion) {
                if (!playersInRegions.get(commandConfig.getKey()).contains(player)) {
                    playersInRegions.get(commandConfig.getKey()).add(player);
                    String formattedTitle = commandConfig.getTitle();
                    formattedTitle = format(formattedTitle);
                    player.sendTitle(formattedTitle, Subtitle, 10, 70, 20);
                }
                executeCommandForPlayer(player, commandConfig);
            } else {
                playersInRegions.get(commandConfig.getKey()).remove(player);
            }
        }
    }

    private boolean isPlayerInRegion(Player player, String regionName) {
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));
        if (regionManager == null) {
            return false;
        }
        Location location = player.getLocation();
        ApplicableRegionSet set = regionManager.getApplicableRegions(
                BlockVector3.at(location.getX(), location.getY(), location.getZ()));
        for (ProtectedRegion region : set) {
            if (region.getId().equalsIgnoreCase(regionName)) {
                return true;
            }
        }
        return false;
    }

    private void updateRewardTimers() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Long soonestReward = null;
            for (CommandConfig commandConfig : commands.values()) {
                if (!commandConfig.isEnabled()) {
                    continue;
                }
                Long nextReward = nextRewardTimes.get(commandConfig.getKey());
                if (nextReward == null || nextReward <= now) {
                    continue;
                }
                if (!isPlayerInRegion(player, commandConfig.getRegionName())) {
                    continue;
                }
                if (soonestReward == null || nextReward < soonestReward) {
                    soonestReward = nextReward;
                }
            }
            if (soonestReward != null) {
                String message = timerFormat.replace("%time%", Countdown.formatDuration(soonestReward - now));
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(format(message)));
            }
        }
    }

    private void executeCommandForPlayer(Player player, CommandConfig commandConfig) {
        if (commandConfig.isEnabled()) {
            int value = commandConfig.getMin() + (int) (Math.random() * ((commandConfig.getMax() - commandConfig.getMin()) + 1));
            if (player.hasPermission("afkpool.bonus")) {
                value *= (int) commandConfig.getMultiplier();
            }
            String command = commandConfig.getCommand().replace("%p", player.getName());
            command = command.replace("%m", String.valueOf(value));
            String finalCommand = command;
            scheduler.runNextTick(task -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));

            String formattedTitle = commandConfig.getTitle().replace("%m", String.valueOf(value));
            formattedTitle = format(formattedTitle);
            player.sendTitle(formattedTitle, Subtitle, 10, 70, 20);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        for (CommandConfig commandConfig : commands.values()) {
            boolean isInRegion = isPlayerInRegion(player, commandConfig.getRegionName());
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        for (Set<Player> set : playersInRegions.values()) {
            set.remove(event.getPlayer());
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
