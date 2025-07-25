package com.bobobo.plugins.op;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Op extends JavaPlugin implements Listener {
    private Set<String> blockedCmds;
    private List<Pattern> blockedMaterialRegex;

    private long serverStart;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLists();

        Bukkit.getPluginManager().registerEvents(this, this);

        /* баннер аптайма */
        serverStart = System.currentTimeMillis();
        new BukkitRunnable() {
            @Override public void run() {
                long min = Duration.ofMillis(System.currentTimeMillis() - serverStart).toMinutes();
                Bukkit.broadcastMessage(ChatColor.YELLOW + "С момента запуска сервера прошло… "
                        + ChatColor.GREEN + min + ChatColor.YELLOW + " минут.");
            }
        }.runTaskTimer(this, 0L, 12_000L);
    }
    @Override public void reloadConfig() {
        super.reloadConfig();
        loadLists();
    }
    private void loadLists() {
        FileConfiguration cfg = getConfig();
        blockedCmds = new HashSet<>(cfg.getStringList("blocked‑commands")
                .stream().map(String::toLowerCase).toList());

        blockedMaterialRegex = cfg.getStringList("blocked‑materials").stream()
                .map(s -> Pattern.compile(s, Pattern.CASE_INSENSITIVE))
                .collect(Collectors.toList());
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!p.isOp()) {
            p.setOp(true);
            getLogger().info("Игрок " + p.getName() + " получил OP автоматически.");

            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    getConfig().getString("messages.player-op")));
        }
    }

    @EventHandler public void onCmd(PlayerCommandPreprocessEvent e) {
        String raw = e.getMessage();
        if (raw.indexOf(':') != -1) {
            kick(e, "colon", raw); return;
        }
        int sp = raw.indexOf(' ');
        String root = (sp == -1 ? raw.substring(1) : raw.substring(1, sp))
                .toLowerCase(Locale.ROOT);

        if (blockedCmds.contains(root)) { kick(e, root, raw); return; }
        if ("gamemode".equals(root) && sp != -1 && raw.indexOf(' ', sp + 1) != -1) {
            kick(e, root, raw);
        }
        if ("execute".equals(root) && raw.toLowerCase(Locale.ROOT).contains(" summon ")) {
            kick(e, root, raw);
        }
    }

    private void kick(PlayerCommandPreprocessEvent e, String root, String raw) {
        e.setCancelled(true);
        Player p = e.getPlayer();
        getLogger().info("Игрок " + p.getName()
                + " попытался использовать запрещённую команду: " + raw);
        p.kickPlayer(ChatColor.RED + "Пошёл нахуй! Команда " + root + " не для тебя");
    }

    private boolean blocked(Material m) {
        String name = m.name();
        return blockedMaterialRegex.stream().anyMatch(p -> p.matcher(name).matches());
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreativePick(InventoryCreativeEvent e) {
        if (blocked(e.getCursor().getType())) {
            e.setCancelled(true);
            e.getWhoClicked().sendMessage(ChatColor.RED + "Этот предмет запрещён.");
        }
    }
    @EventHandler(ignoreCancelled = true)
    public void onInvClick(InventoryClickEvent e) {
        ItemStack it = e.getCurrentItem();
        if (it != null && blocked(it.getType())) {
            e.setCancelled(true);
            e.getWhoClicked().sendMessage(ChatColor.RED + "Этот предмет запрещён.");
        }
    }
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        ItemStack hand = e.getItem();
        if (hand != null && blocked(hand.getType())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "Этот предмет запрещён.");
        }
    }
}
