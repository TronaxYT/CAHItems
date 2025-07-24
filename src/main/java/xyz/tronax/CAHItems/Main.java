package xyz.tronax.CAHItems;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Main extends JavaPlugin implements Listener {

    public static String prefix = "§cCAH §eItems §8| ";

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Prevent moving items in the hotbar
        if (event.getClickedInventory() != null && event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (event.getSlot() < 9 && hotbarItems.containsKey(event.getSlot())) {
                event.setCancelled(true);
            }
        }
    }

    private Map<Integer, ItemStack> hotbarItems = new HashMap<>();
    private Map<Integer, String> itemCommands = new HashMap<>();
    private Map<Integer, Inventory> itemInventories = new HashMap<>();
    private Map<Integer, String> itemServers = new HashMap<>();
    private Map<Integer, String> itemPermissions = new HashMap<>();
    private Map<Integer, Integer> itemSlots = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        if (getResource("config.yml") == null) {
            createDefaultConfig();
            saveConfig();
        }
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        print(prefix + " §aPlugin successfully enabled!");
    }

    public void print(String msg) {
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        print(prefix + " §cPlugin successfully disabled!");
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        if (!config.contains("items")) {
            createDefaultConfig();
            saveConfig();
        }
        hotbarItems.clear();
        itemCommands.clear();
        itemInventories.clear();
        itemServers.clear();
        itemPermissions.clear();
        itemSlots.clear();

        for (String key : config.getConfigurationSection("items").getKeys(false)) {
            int slot = config.getInt("items." + key + ".slot");
            String materialName = config.getString("items." + key + ".material");
            String displayName = config.getString("items." + key + ".displayName");
            List<String> lore = config.getStringList("items." + key + ".lore");
            String command = config.getString("items." + key + ".command");
            String server = config.getString("items." + key + ".server");
            String permission = config.getString("items." + key + ".permission", "");
            boolean opensInventory = config.getBoolean("items." + key + ".opensInventory", false);

            Material material = Material.getMaterial(materialName.toUpperCase());
            if (material != null) {
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(displayName);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                hotbarItems.put(slot, item);
                if (opensInventory) {
                    Inventory inventory = Bukkit.createInventory(null, 9, displayName);
                    // Example to add items to the inventory, this can be customized
                    inventory.addItem(new ItemStack(Material.DIAMOND, 1));
                    itemInventories.put(slot, inventory);
                } else if (server != null && !server.isEmpty()) {
                    itemServers.put(slot, server);
                } else {
                    itemCommands.put(slot, command);
                }
                itemPermissions.put(slot, permission);
                itemSlots.put(slot, slot);
            } else {
                getLogger().warning("Invalid material: " + materialName);
            }
        }
    }

    private void createDefaultConfig() {
        FileConfiguration config = getConfig();
        config.set("items.0.slot", 0);
        config.set("items.0.material", "COMPASS");
        config.set("items.0.displayName", "§aNavigator");
        config.set("items.0.lore", List.of("§7Wähle dein Spiel"));
        config.set("items.0.command", "warp lobby");
        config.set("items.0.opensInventory", false);
        config.set("items.0.permission", "cahitems.navigator");
        config.set("items.1.slot", 1);
        config.set("items.1.material", "CLOCK");
        config.set("items.1.displayName", "§bSurvival Server");
        config.set("items.1.lore", List.of("§7Betrete den Survival Server"));
        config.set("items.1.server", "survival");
        config.set("items.1.permission", "cahitems.survival");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        for (Map.Entry<Integer, ItemStack> entry : hotbarItems.entrySet()) {
            int slot = entry.getKey();
            ItemStack configuredItem = entry.getValue();

            if (player.getInventory().getHeldItemSlot() == slot && item.isSimilar(configuredItem)) {
                String permission = itemPermissions.get(slot);
                if (permission.isEmpty() || player.hasPermission(permission)) {
                    if (itemInventories.containsKey(slot)) {
                        player.openInventory(itemInventories.get(slot));
                    } else if (itemServers.containsKey(slot)) {
                        String server = itemServers.get(slot);
                        sendPlayerToServer(player, server);
                    } else {
                        String command = itemCommands.get(slot);
                        if (command != null && !command.isEmpty()) {
                            player.performCommand(command);
                        }
                    }
                    event.setCancelled(true);
                } else {
                    player.sendMessage("§cYou do not have permission to use this item.");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        giveHotbarItems(player);
    }

    public void giveHotbarItems(Player player) {
        // Clear inventory before giving new items if they have changed
        player.getInventory().clear();
        for (Map.Entry<Integer, ItemStack> entry : hotbarItems.entrySet()) {
            String permission = itemPermissions.get(entry.getKey());
            if (permission.isEmpty() || player.hasPermission(permission)) {
                ItemStack item = entry.getValue();
                item.setAmount(1); // Ensure the item stack has only one item
                player.getInventory().setItem(entry.getKey(), item);
                player.getInventory().getItem(entry.getKey()).setAmount(1);
            }
        }
    }

    public void reloadConfig() {
        super.reloadConfig();
        loadConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("cahitems")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("cahitems.reload")) {
                    reloadConfig();
                    sender.sendMessage("§aConfig reloaded successfully!");
                } else {
                    sender.sendMessage("§cYou do not have permission to use this command.");
                }
                return true;
            } else {
                sender.sendMessage("§cUsage: /cahitems reload");
                return true;
            }
        }
        return false;
    }

    private void sendPlayerToServer(Player player, String serverName) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(this, "BungeeCord", b.toByteArray());
        } catch (Exception e) {
            player.sendMessage("§cFailed to connect to server: " + serverName);
            e.printStackTrace();
        }
    }

    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        // Handle incoming plugin messages if needed
    }

}
