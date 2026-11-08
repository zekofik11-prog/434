package dev.artisan;

import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Собирает ресурспак из plugins/Artisan/pack и раздаёт его по встроенному HTTP. */
public final class PackManager {

    private static final long FIXED_TIME = 315532800000L; // 1980-01-01, чтобы хеш был стабильным

    private final ArtisanPlugin plugin;
    private HttpServer server;
    private volatile byte[] data;
    private volatile byte[] hash;
    private volatile String hex = "";
    private volatile UUID packId = UUID.randomUUID();

    public PackManager(ArtisanPlugin plugin) {
        this.plugin = plugin;
    }

    public void rebuildAsync(boolean resend) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                build();
            } catch (Exception e) {
                plugin.getLogger().severe("Не удалось собрать ресурспак: " + e.getMessage());
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                startServer();
                if (resend) {
                    for (Player p : Bukkit.getOnlinePlayers()) send(p);
                }
            });
        });
    }

    private void build() throws Exception {
        Path root = plugin.getDataFolder().toPath().resolve("pack");
        Files.createDirectories(root);

        List<Path> files;
        try (Stream<Path> s = Files.walk(root)) {
            files = s.filter(Files::isRegularFile).sorted().toList();
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(bos)) {
            if (!Files.exists(root.resolve("pack.mcmeta"))) {
                put(z, "pack.mcmeta", mcmeta().getBytes(StandardCharsets.UTF_8));
            }
            for (Path f : files) {
                String name = root.relativize(f).toString().replace('\\', '/');
                put(z, name, Files.readAllBytes(f));
            }
        }
        byte[] bytes = bos.toByteArray();
        byte[] sha1 = MessageDigest.getInstance("SHA-1").digest(bytes);

        this.data = bytes;
        this.hash = sha1;
        this.hex = HexFormat.of().formatHex(sha1);
        this.packId = UUID.nameUUIDFromBytes(sha1);

        Path out = plugin.getDataFolder().toPath().resolve("build/pack.zip");
        Files.createDirectories(out.getParent());
        Files.write(out, bytes);
        plugin.getLogger().info("Ресурспак собран: " + files.size() + " файлов, sha1=" + hex);
    }

    private static void put(ZipOutputStream z, String name, byte[] content) throws IOException {
        ZipEntry e = new ZipEntry(name);
        e.setTime(FIXED_TIME);
        z.putNextEntry(e);
        z.write(content);
        z.closeEntry();
    }

    private String mcmeta() {
        FileConfiguration c = plugin.getConfig();
        int format = c.getInt("pack.pack-format", 75);
        String desc = c.getString("pack.description", "Artisan resource pack").replace("\"", "\\\"");
        return "{\"pack\":{\"description\":\"" + desc + "\",\"pack_format\":" + format + "}}";
    }

    private void startServer() {
        if (server != null) return;
        int port = plugin.getConfig().getInt("pack.port", 8085);
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/pack.zip", ex -> {
                try (ex) {
                    byte[] d = data;
                    if (d == null) {
                        ex.sendResponseHeaders(404, -1);
                        return;
                    }
                    ex.getResponseHeaders().add("Content-Type", "application/zip");
                    ex.sendResponseHeaders(200, d.length);
                    try (OutputStream os = ex.getResponseBody()) {
                        os.write(d);
                    }
                }
            });
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            plugin.getLogger().info("HTTP-раздача ресурспака запущена на порту " + port);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось запустить HTTP на порту " + port + ": " + e.getMessage());
            server = null;
        }
    }

    public void send(Player player) {
        if (data == null || server == null) return;
        FileConfiguration c = plugin.getConfig();
        String url = "http://" + c.getString("pack.host", "127.0.0.1") + ":" + c.getInt("pack.port", 8085)
                + "/pack.zip?v=" + hex;
        player.setResourcePack(packId, url, hash,
                Component.text(c.getString("pack.prompt", "Ресурспак сервера")),
                c.getBoolean("pack.force", false));
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }
}
