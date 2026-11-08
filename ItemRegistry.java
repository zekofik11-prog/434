package dev.artisan;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ItemRegistry {

    private final ArtisanPlugin plugin;
    private final NamespacedKey idKey;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<String, ItemStack> templates = new LinkedHashMap<>();

    public ItemRegistry(ArtisanPlugin plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "id");
    }

    public void load() {
        templates.clear();
        File file = new File(plugin.getDataFolder(), "items.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("items");
        if (root == null) {
            plugin.getLogger().warning("items.yml: нет секции items");
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            try {
                templates.put(id, build(id, s));
            } catch (Exception e) {
                plugin.getLogger().warning("Предмет '" + id + "' пропущен: " + e.getMessage());
            }
        }
        plugin.getLogger().info("Загружено предметов: " + templates.size());
    }

    private ItemStack build(String id, ConfigurationSection s) {
        Material mat = Material.matchMaterial(s.getString("material", "PAPER"));
        if (mat == null || !mat.isItem()) throw new IllegalArgumentException("неизвестный material");

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        String name = s.getString("name");
        if (name != null) {
            meta.displayName(mm.deserialize(name).decoration(TextDecoration.ITALIC, false));
        }

        List<String> loreRaw = s.getStringList("lore");
        if (!loreRaw.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreRaw) {
                lore.add(mm.deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        }

        NamespacedKey model = NamespacedKey.fromString(s.getString("item_model", "artisan:" + id));
        if (model == null) throw new IllegalArgumentException("некорректный item_model");
        meta.setItemModel(model);

        if (s.getBoolean("glow", false)) meta.setEnchantmentGlintOverride(true);
        if (s.getBoolean("unbreakable", false)) meta.setUnbreakable(true);
        if (s.contains("max_stack_size")) meta.setMaxStackSize(Math.max(1, Math.min(99, s.getInt("max_stack_size"))));

        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack create(String id, int amount) {
        ItemStack t = templates.get(id);
        if (t == null) return null;
        ItemStack copy = t.clone();
        copy.setAmount(Math.max(1, amount));
        return copy;
    }

    public String idOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    public Set<String> ids() { return templates.keySet(); }
}
