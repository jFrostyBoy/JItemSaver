package jfbdev.jitemsaver;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class JItemSaver extends JavaPlugin implements Listener {

    private final Set<SavedItemMatcher> savedItems = new HashSet<>();
    private String msgNoPerm;
    private String msgReload;
    private String msgSaved;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("JItemSaver включён");
    }

    private void loadConfigValues() {
        FileConfiguration cfg = getConfig();

        msgNoPerm = cfg.getString("messages.no-permission", "&4✗ &cНедостаточно прав");
        msgReload = cfg.getString("messages.reload", "&2✔ &fПлагин перезагружен");
        msgSaved = cfg.getString("messages.itemsaver-message", "&2✔ &fПредметы сохранены &7({count} шт.)");

        savedItems.clear();

        List<String> list = cfg.getStringList("itemsaver-list");
        for (String line : list) {
            if (line == null || line.trim().isEmpty()) continue;

            SavedItemMatcher matcher = SavedItemMatcher.fromString(line.trim());
            if (matcher != null) {
                savedItems.add(matcher);
            } else {
                getLogger().warning("Неверный формат предмета в конфиге: " + line);
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, @NotNull String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("jisreload")) {
            if (!sender.hasPermission("jitemsaver.reload")) {
                sender.sendMessage(color(msgNoPerm));
                return true;
            }
            reloadConfig();
            loadConfigValues();
            sender.sendMessage(color(msgReload));
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();

        if (e.getKeepInventory()) return;
        if (e.isCancelled()) return;
        if (p.getHealth() > 0) return;

        List<ItemStack> toKeep = new ArrayList<>();

        for (ItemStack item : p.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (shouldKeep(item)) {
                toKeep.add(item.clone());
            }
        }

        if (toKeep.isEmpty()) return;

        e.getDrops().removeIf(drop -> {
            for (ItemStack keep : toKeep) {
                if (drop.isSimilar(keep)) return true;
            }
            return false;
        });

        e.getItemsToKeep().addAll(toKeep);

        int count = toKeep.size();
        String message = color(msgSaved.replace("{count}", String.valueOf(count)));
        p.sendMessage(message);
    }

    private boolean shouldKeep(ItemStack item) {
        for (SavedItemMatcher matcher : savedItems) {
            if (matcher.matches(item)) return true;
        }
        return false;
    }

    private String color(String s) {
        return s.replace("&", "§");
    }

    private static class SavedItemMatcher {
        private final Material material;
        private final Map<Enchantment, Integer> requiredEnchants;

        private SavedItemMatcher(Material material, Map<Enchantment, Integer> enchants) {
            this.material = material;
            this.requiredEnchants = enchants;
        }

        static SavedItemMatcher fromString(String s) {
            String[] parts = s.split(";", 2);
            String matName = parts[0].trim().toUpperCase();

            Material mat;
            try {
                mat = Material.valueOf(matName);
            } catch (IllegalArgumentException e) {
                return null;
            }

            Map<Enchantment, Integer> enchants = new HashMap<>();

            if (parts.length > 1) {
                String enchPart = parts[1].trim();
                if (!enchPart.isEmpty()) {
                    for (String pair : enchPart.split(",")) {
                        String[] kv = pair.trim().split(":", 2);
                        if (kv.length != 2) continue;

                        String enchKey = kv[0].trim().toLowerCase();
                        Enchantment ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchKey));
                        if (ench == null) continue;

                        int level;
                        try {
                            level = Integer.parseInt(kv[1].trim());
                        } catch (NumberFormatException ex) {
                            continue;
                        }

                        enchants.put(ench, level);
                    }
                }
            }

            return new SavedItemMatcher(mat, enchants);
        }

        boolean matches(ItemStack item) {
            if (item == null || item.getType() != material) return false;

            if (requiredEnchants.isEmpty()) return true;

            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasEnchants()) return false;

            for (Map.Entry<Enchantment, Integer> entry : requiredEnchants.entrySet()) {
                Enchantment e = entry.getKey();
                int reqLevel = entry.getValue();

                if (!meta.hasEnchant(e) || meta.getEnchantLevel(e) < reqLevel) return false;
            }

            return true;
        }
    }
}