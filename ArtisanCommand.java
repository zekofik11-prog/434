package dev.artisan;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ArtisanCommand implements CommandExecutor, TabCompleter {

    private final ArtisanPlugin plugin;

    public ArtisanCommand(ArtisanPlugin plugin) {
        this.plugin = plugin;
    }

    private static void msg(CommandSender s, String text, NamedTextColor color) {
        s.sendMessage(Component.text(text, color));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            msg(sender, "/artisan give <игрок> <id> [кол-во] | list | reload | pack", NamedTextColor.YELLOW);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> {
                if (args.length < 3) {
                    msg(sender, "Использование: /artisan give <игрок> <id> [кол-во]", NamedTextColor.RED);
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    msg(sender, "Игрок не найден", NamedTextColor.RED);
                    return true;
                }
                int amount = 1;
                if (args.length >= 4) {
                    try {
                        amount = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        msg(sender, "Кол-во должно быть числом", NamedTextColor.RED);
                        return true;
                    }
                }
                ItemStack item = plugin.items().create(args[2], amount);
                if (item == null) {
                    msg(sender, "Предмет не найден: " + args[2], NamedTextColor.RED);
                    return true;
                }
                target.getInventory().addItem(item).values()
                        .forEach(rest -> target.getWorld().dropItemNaturally(target.getLocation(), rest));
                msg(sender, "Выдано " + amount + "x " + args[2] + " игроку " + target.getName(), NamedTextColor.GREEN);
            }
            case "list" -> msg(sender, "Предметы: " + String.join(", ", plugin.items().ids()), NamedTextColor.AQUA);
            case "reload" -> {
                plugin.reloadAll();
                msg(sender, "Конфиг и предметы перезагружены, пак пересобирается", NamedTextColor.GREEN);
            }
            case "pack" -> {
                plugin.pack().rebuildAsync(true);
                msg(sender, "Пересборка ресурспака запущена, игрокам он отправится заново", NamedTextColor.GREEN);
            }
            default -> msg(sender, "Неизвестная подкоманда", NamedTextColor.RED);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(List.of("give", "list", "reload", "pack"));
        } else if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            else if (args.length == 3) out.addAll(plugin.items().ids());
        }
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
