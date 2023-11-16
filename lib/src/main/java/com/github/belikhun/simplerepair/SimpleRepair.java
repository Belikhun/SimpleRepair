package com.github.belikhun.simplerepair;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.lucko.commodore.Commodore;
import me.lucko.commodore.CommodoreProvider;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleRepair extends JavaPlugin {

    protected Economy economy;
    protected FileConfiguration config;
    protected File configFile;
    protected Commodore commodore;

    protected static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    protected final char COLOR_CHAR = ChatColor.COLOR_CHAR;

    @Override
    public void onEnable() {
        // Load the configuration
        loadConfig();

        getLogger().info(getMessage("messages.simplerepair-info"));

        if (!setupEconomy()) {
            getLogger().warning("Vault or an economy plugin not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register the commands
        getCommand("repair").setExecutor(this);
        getCommand("repairc").setExecutor(this);

        PluginCommand mainCommand = getCommand("simplerepair");
        mainCommand.setExecutor(this);

        // Check if brigadier is supported
        if (CommodoreProvider.isSupported()) {
            commodore = CommodoreProvider.getCommodore(this);
			
            commodore.register(
                mainCommand,
                LiteralArgumentBuilder.literal("simplerepair")
                    .then(LiteralArgumentBuilder.literal("reload"))
            );
        } else {
			getLogger().warning("Commodore is not supported on this server!");
		}
    }

    @Override
    public void onDisable() {
        getLogger().info("SimpleRepair has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("repair")) {
            calculateAndRepair(sender);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("repairc")) {
            calculateCost(sender);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("simplerepair")) {
            if (args.length == 0) {
                sendSimpleRepairInfo(sender);
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                handleReloadCommand(sender);
                return true;
            }
        }

        return false;
    }

    protected void sendSimpleRepairInfo(CommandSender sender) {
        sender.sendMessage(getMessage("messages.simplerepair-info"));

        List<String> commandList = config.getStringList("messages.simplerepair-command-list");
        
        for (String command : commandList)
            sender.sendMessage(colorize(command));
    }

    protected void handleReloadCommand(CommandSender sender) {
        if (sender.hasPermission("simplerepair.reload")) {
            loadConfig();
            sender.sendMessage(getMessage("messages.simplerepair-reload-success"));
        } else {
            sender.sendMessage(getMessage("messages.no-permission"));
        }
    }

    protected void calculateAndRepair(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("messages.no-console"));
            return;
        }

        if (!sender.hasPermission("simplerepair.repair")) {
            sender.sendMessage(getMessage("messages.no-permission"));
            return;
        }

        Player player = (Player) sender;

        // Check if the player has the item in hand
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(getMessage("messages.must-hold-item"));
            return;
        }

        // Check if the item is repairable
        if (!isRepairable(itemInHand.getType())) {
            player.sendMessage(getMessage("messages.not-repairable"));
            return;
        }

        // Check if the tool is damaged
        if (!(itemInHand.getItemMeta() instanceof Damageable)) {
            player.sendMessage(getMessage("messages.not-damaged"));
            return;
        }

        Damageable damageable = (Damageable) itemInHand.getItemMeta();

        // Check if the tool is actually damaged
        if (damageable.getDamage() == 0) {
            player.sendMessage(getMessage("messages.not-damaged"));
            return;
        }

        // Calculate repair cost
        double repairCost = calculateRepairCost(itemInHand);

        // Check if the player has enough money
        if (!withdrawMoney(player, repairCost)) {
            player.sendMessage(getMessage("messages.not-enough-money"));
            return;
        }

        // Repair the item
        damageable.setDamage(0);
        itemInHand.setItemMeta((ItemMeta) damageable);

        player.sendMessage(getMessage("messages.item-repaired").replace("%cost%", String.valueOf(repairCost)));
    }

    protected void calculateCost(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("messages.no-console"));
            return;
        }

        if (!sender.hasPermission("simplerepair.repair")) {
            sender.sendMessage(getMessage("messages.no-permission"));
            return;
        }

        Player player = (Player) sender;

        // Check if the player has the item in hand
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(getMessage("messages.must-hold-item"));
            return;
        }

        // Check if the item is repairable
        if (!isRepairable(itemInHand.getType())) {
            player.sendMessage(getMessage("messages.not-repairable"));
            return;
        }

        // Check if the tool is damaged
        if (!(itemInHand.getItemMeta() instanceof Damageable)) {
            player.sendMessage(getMessage("messages.not-damaged"));
            return;
        }

        // Calculate and display repair cost
        double repairCost = calculateRepairCost(itemInHand);
        player.sendMessage(getMessage("messages.repair-cost").replace("%cost%", String.valueOf(repairCost)));
    }

    protected double calculateRepairCost(ItemStack item) {
        double maxDamage = item.getType().getMaxDurability();
        double percentageDamage = (double) ((Damageable) item.getItemMeta()).getDamage() / maxDamage;
        double cost = getConfig().getDouble("base-cost") + (getConfig().getDouble("cost-percentage") * percentageDamage);
        return Math.round(cost * 100.0) / 100.0;
    }
 
    protected boolean withdrawMoney(Player player, double amount) {
        // Use Vault API to withdraw money
        if (economy != null && economy.getBalance(player) >= amount) {
            economy.withdrawPlayer(player, amount);
            return true;
        }

        return false;
    }

    protected boolean isRepairable(Material material) {
        // Add your logic to determine if the material is repairable
        // For example, you can check if the material is a tool or armor piece
        return material.isItem();
    }

    protected void loadConfig() {
        configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists())
            saveResource("config.yml", false);

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    protected String getMessage(String path) {
        String message = config.getString("messages.prefix") + config.getString(path);
        return colorize(message);
    }

    protected String colorize(String message) {
        message = translateColors(message);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    protected String translateColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);

        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
            );
        }

        return matcher.appendTail(buffer).toString();
    }

    protected boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null)
            return false;

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null)
            return false;

        economy = rsp.getProvider();
        return economy != null;
    }
}
