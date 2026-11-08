package dev.artisan;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ArtisanPlugin extends JavaPlugin {

    private ItemRegistry items;
    private PackManager pack;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("items.yml", false);
        extractSamplePack();

        items = new ItemRegistry(this);
        items.load();

        pack = new PackManager(this);
        pack.rebuildAsync(false);

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        ArtisanCommand cmd = new ArtisanCommand(this);
        PluginCommand pc = getCommand("artisan");
        if (pc != null) {
            pc.setExecutor(cmd);
            pc.setTabCompleter(cmd);
        }
        ArtisanAPI.init(this);
    }

    @Override
    public void onDisable() {
        if (pack != null) pack.stop();
    }

    /** Кладёт пример ресурспака, только если папка pack ещё не создана. */
    private void extractSamplePack() {
        if (new File(getDataFolder(), "pack").exists()) return;
        saveResource("pack/assets/artisan/items/ruby.json", false);
        saveResource("pack/assets/artisan/models/item/ruby.json", false);
        saveResource("pack/assets/artisan/textures/item/ruby.png", false);
    }

    public void reloadAll() {
        reloadConfig();
        items.load();
        pack.rebuildAsync(true);
    }

    public ItemRegistry items() { return items; }
    public PackManager pack() { return pack; }
}
