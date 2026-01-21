package jfbdev.jitemsaver;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.ssomar.score.api.executableitems.ExecutableItemsAPI;
import com.ssomar.score.api.executableitems.config.ExecutableItemsManagerInterface;
import com.ssomar.score.api.executableitems.config.ExecutableItemInterface;
import org.jetbrains.annotations.NotNull;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import java.util.*;

public final class JItemSaver extends JavaPlugin implements Listener {

    private String msgNoPerm;
    private String msgReload;
    private String msgSaved;

    private boolean hasExecutableItems = false;
    private ExecutableItemsManagerInterface eiManager = null;

    private final Map<String, Set<SavedItemMatcher>> groupItems = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        hookExecutableItems();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("JItemSaver включён");
        if (hasExecutableItems) {
            getLogger().info("JItemSaver → ExecutableItems подключён");
        }
    }

    private void hookExecutableItems() {
        Plugin eiPlugin = getServer().getPluginManager().getPlugin("ExecutableItems");
        if (eiPlugin != null && eiPlugin.isEnabled()) {
            try {
                Class.forName("com.ssomar.score.api.executableitems.ExecutableItemsAPI");
                eiManager = ExecutableItemsAPI.getExecutableItemsManager();
                hasExecutableItems = true;
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                getLogger().warning("ExecutableItems найден, но API недоступен");
                hasExecutableItems = false;
            }
        }
    }

    private void loadConfigValues() {
        FileConfiguration cfg = getConfig();

        msgNoPerm = cfg.getString("messages.no-permission", "&4✗ &cНедостаточно прав");
        msgReload = cfg.getString("messages.reload", "&2✔ &fПлагин перезагружен");
        msgSaved = cfg.getString("messages.itemsaver-message", "&2✔ &fПредметы сохранены &7({count} шт.)");

        groupItems.clear();

        ConfigurationSection section = cfg.getConfigurationSection("itemsaver-list");
        if (section != null) {
            for (String group : section.getKeys(false)) {
                List<String> lines = section.getStringList(group);
                Set<SavedItemMatcher> matchers = new HashSet<>();

                for (String line : lines) {
                    if (line == null || line.trim().isEmpty()) continue;
                    SavedItemMatcher matcher = SavedItemMatcher.fromString(line.trim());
                    if (matcher != null) {
                        matchers.add(matcher);
                    } else {
                        getLogger().warning("Неверный формат в группе " + group + ": " + line);
                    }
                }

                groupItems.put(group.toLowerCase(), matchers);
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

        Set<SavedItemMatcher> activeMatchers = getActiveMatchersForPlayer(p);

        if (activeMatchers.isEmpty()) return;

        List<ItemStack> toKeep = new ArrayList<>();

        ItemStack[] contents = p.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;

            String slotType = (i == 39) ? "HEAD" :
                    (i == 38) ? "CHEST" :
                            (i == 37) ? "LEGS" :
                                    (i == 36) ? "FEET" :
                                            (i == 40) ? "OFFHAND" : "INVENTORY";

            for (SavedItemMatcher matcher : activeMatchers) {
                if (matcher.matches(item, slotType, this)) {
                    toKeep.add(item.clone());
                    break;
                }
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

    private Set<SavedItemMatcher> getActiveMatchersForPlayer(Player player) {
        LuckPerms lp = LuckPermsProvider.get();

        User user = lp.getUserManager().getUser(player.getUniqueId());
        if (user == null) return new HashSet<>();

        String primaryGroup = user.getPrimaryGroup().toLowerCase();

        return groupItems.getOrDefault(primaryGroup, new HashSet<>());
    }

    private String color(String s) {
        return s.replace("&", "§");
    }

    private static class SavedItemMatcher {

        private final Material material;
        private final Map<Enchantment, Integer> requiredEnchants;
        private final String eiId;
        private final boolean isEiCustom;
        private final String requiredSlot;

        private SavedItemMatcher(Material mat, Map<Enchantment, Integer> enchants, String eiId, boolean isCustom, String slot) {
            this.material = mat;
            this.requiredEnchants = enchants != null ? enchants : new HashMap<>();
            this.eiId = eiId;
            this.isEiCustom = isCustom;
            this.requiredSlot = slot;
        }

        static SavedItemMatcher fromString(String s) {
            String line = s.trim();
            String slot = null;

            int lastColon = line.lastIndexOf(':');
            if (lastColon > 0) {
                String potentialSlot = line.substring(lastColon + 1).trim().toUpperCase();
                if (potentialSlot.equals("HEAD") || potentialSlot.equals("CHEST") ||
                        potentialSlot.equals("LEGS") || potentialSlot.equals("FEET") ||
                        potentialSlot.equals("OFFHAND")) {
                    slot = potentialSlot;
                    line = line.substring(0, lastColon).trim();
                }
            }

            if (line.startsWith("CUSTOM:")) {
                String id = line.substring("CUSTOM:".length()).trim();
                if (id.isEmpty()) return null;
                return new SavedItemMatcher(null, null, id, true, slot);
            }

            String[] parts = line.split(";", 2);
            String matName = parts[0].trim().toUpperCase();

            Material mat;
            try {
                mat = Material.valueOf(matName);
            } catch (IllegalArgumentException e) {
                return new SavedItemMatcher(null, null, line, true, slot);
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

            return new SavedItemMatcher(mat, enchants, null, false, slot);
        }

        boolean matches(ItemStack item, String currentSlot, JItemSaver plugin) {
            if (item == null) return false;

            if (requiredSlot != null && !requiredSlot.equals(currentSlot)) {
                return false;
            }

            if (isEiCustom) {
                if (!plugin.hasExecutableItems || plugin.eiManager == null) return false;

                Optional<ExecutableItemInterface> eiOpt = plugin.eiManager.getExecutableItem(item);

                if (eiOpt.isPresent()) {
                    String realId = eiOpt.get().getId();
                    return realId != null && realId.equalsIgnoreCase(eiId);
                }
                return false;
            }

            if (item.getType() != material) return false;

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
