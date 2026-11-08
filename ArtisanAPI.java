package dev.artisan;

import org.bukkit.inventory.ItemStack;

/** API для других плагинов: ArtisanAPI.getItem("ruby", 1), ArtisanAPI.getId(stack). */
public final class ArtisanAPI {

    private static ArtisanPlugin plugin;

    private ArtisanAPI() {}

    static void init(ArtisanPlugin p) { plugin = p; }

    public static ItemStack getItem(String id, int amount) { return plugin.items().create(id, amount); }

    public static String getId(ItemStack item) { return plugin.items().idOf(item); }

    public static boolean exists(String id) { return plugin.items().ids().contains(id); }
}
