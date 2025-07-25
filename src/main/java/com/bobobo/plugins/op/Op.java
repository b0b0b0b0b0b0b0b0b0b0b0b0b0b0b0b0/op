package com.bobobo.plugins.op;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.Set;

public final class Op extends JavaPlugin implements Listener {

    private static final Set<String> BLOCKED = Set.of(
            "stop","reload","end","restart",
            "save","save-all","save-on","save-off",
            "paper","timings","version","ver","plugins","pl",
            "debug","dump","heap","profiler","tps","tpsmap",
            "effect","difficulty","spawnpoint","attribute",
            "ban","ban-ip","banlist","kick","whitelist","op","deop",

            "kill","gamerule","worldborder","scoreboard","fill","fillbiome",
            "setworldspawn","spawn","tp","pos","pos1","summon","give",
            "help","about","day","time","weather","gc","?","execute","function",

            "viaversion","via","viaver","viaback","viabackwards",
            "skinsrestorer","sr","skin","skins","skinupdate","skinset",
            "axiompaper","axiompaperdebug","axiompapershutdown",
            "antipopup","protocol",

            "//pos","//pos1","//pos2","//setblock","//wand"
    );

    private long startMillis;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        startMillis = System.currentTimeMillis();
        new BukkitRunnable() {
            @Override public void run() {
                long min = Duration.ofMillis(System.currentTimeMillis()-startMillis).toMinutes();
                Bukkit.broadcastMessage(ChatColor.YELLOW+"С момента запуска сервера прошло… "
                        +ChatColor.GREEN+min+ChatColor.YELLOW+" минут.");
            }
        }.runTaskTimer(this, 0L, 12_000L);
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!p.isOp()) {
            p.setOp(true);
            getLogger().info("Игрок "+p.getName()+" получил OP автоматически.");
            FileConfiguration cfg = getConfig();
            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    cfg.getString("messages.player-op",
                            "&aВы получили OP автоматически!")));
        }
    }

    @EventHandler
    public void onCmd(PlayerCommandPreprocessEvent e) {
        final String raw = e.getMessage();
        if (raw.length() < 2 || raw.charAt(0) != '/') return;
        if (raw.indexOf(':') != -1) {
            block(e, "colon", raw);
            return;
        }
        final int sp = raw.indexOf(' ');
        final String root = (sp == -1 ? raw.substring(1) : raw.substring(1, sp)).toLowerCase();
        final int colon = root.indexOf(':');
        final String simple = colon == -1 ? root : root.substring(colon + 1);
        if (BLOCKED.contains(root) || BLOCKED.contains(simple)) {
            block(e, simple, raw);
            return;
        }
        if ("gamemode".equals(simple)) {
            if (sp != -1 && raw.indexOf(' ', sp + 1) != -1) {
                block(e, simple, raw);
            }
            return;
        }
        if ("execute".equals(simple) && raw.toLowerCase().contains(" summon ")) {
            block(e, simple, raw);
        }
    }
    private void block(PlayerCommandPreprocessEvent e, String root, String raw) {
        e.setCancelled(true);
        Player p = e.getPlayer();
        getLogger().info("Игрок " + p.getName()
                + " попытался использовать запрещённую команду: " + raw);
        p.kickPlayer(ChatColor.RED + "Пошёл нахуй! Команда " + root + " не для тебя");
    }

    private static boolean isSpawnEgg(Material m){
        return m != null && m.name().endsWith("_SPAWN_EGG");
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreativePick(InventoryCreativeEvent e){
        ItemStack cursor = e.getCursor();
        if (isSpawnEgg(cursor.getType())){
            e.setCancelled(true);
            e.getWhoClicked().sendMessage(ChatColor.RED+"Яйца призыва отключены.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInvClick(InventoryClickEvent e){
        ItemStack current = e.getCurrentItem();
        if (current != null && isSpawnEgg(current.getType())){
            e.setCancelled(true);
            e.getWhoClicked().sendMessage(ChatColor.RED+"Яйца призыва отключены.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e){
        ItemStack hand = e.getItem();
        if (hand != null && isSpawnEgg(hand.getType())){
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED+"Яйца призыва отключены.");
        }
    }
}