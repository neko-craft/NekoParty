package cn.apisium.nekoparty;

import cn.apisium.nekoparty.games.DangerousFlowers;
import cn.apisium.nekoparty.games.LastOfUS;
import cn.apisium.nekoparty.games.LetsJump;
import cn.apisium.nekoparty.games.RememberTheBlock;
import com.destroystokyo.paper.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class Knockout implements Listener {
    private final Location center;
    private final LetsJump letsJump;
    private final LastOfUS lastOfUS;
    private final RememberTheBlock rememberTheBlock;
    private final DangerousFlowers dangerousFlowers;
    private final Set<Player> players;
    public final Set<Player> remains;
    private final LinkedList<String> ranking = new LinkedList<>();
    private int knockout, stage;
    public final int count;
    private boolean started, shouldNotMove;
    private BukkitTask task;

    private static final Title TITLE = new Title("§a恭喜您晋级了!", "", 10, 40, 10);

    public Knockout(final Block center, final Set<Player> players) {
        this.players = players;
        this.center = center.getLocation();
        knockout = count = players.size();
        remains = new HashSet<>(players);
        letsJump = new LetsJump(center, this);
        lastOfUS = new LastOfUS(center, this);
        dangerousFlowers = new DangerousFlowers(center, this);
        rememberTheBlock = new RememberTheBlock(center, this);
    }

    public void start() {
        if (players.size() < 4) throw new RuntimeException("人数过少!");
        letsJump.clear();
        lastOfUS.clear();
        dangerousFlowers.clear();
        rememberTheBlock.clear();
        Bukkit.getPluginManager().registerEvents(this, Main.INSTANCE);
        setGameMode(GameMode.SPECTATOR);
        teleport(center);
        title("§e比赛即将开始!", "");
        sound();
        Utils.later(20 * 3, this::nextGame);
        if (task != null && !task.isCancelled()) task.cancel();
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(Main.INSTANCE, this::updateChunksLight, 60, 60);
        Bukkit.getOnlinePlayers().forEach(it -> {
            it.stopSound("com.neko-craft.game", SoundCategory.RECORDS);
            it.playSound(it.getLocation(), "com.neko-craft.game", SoundCategory.RECORDS, 1f, 1f);
        });
    }

    public void clear() {
        letsJump.clear();
        lastOfUS.clear();
        rememberTheBlock.clear();
        dangerousFlowers.clear();
        players.clear();
        remains.clear();
        Utils.clearNeko(center);
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void particle(final Location location) {
        location.getWorld().spawnParticle(Particle.TOTEM, location.clone().add(0, 1, 0), 300, 0.8, 1, 0.8, 0.1F);
    }

    public void stop() {
        letsJump.stop();
        rememberTheBlock.stop();
        dangerousFlowers.stop();
        lastOfUS.stop();
        clear();
    }

    private void nextGame() {
        remains.forEach(it -> {
            it.setFoodLevel(20);
            it.setHealth(20);
            PlayerInventory inv = it.getInventory();
            inv.setItemInMainHand(null);
            inv.setItemInOffHand(null);
            inv.setBoots(null);
            inv.setHelmet(null);
            inv.setChestplate(null);
            inv.setLeggings(null);
        });
        switch (++stage) {
            case 1:
                letsJump.sendIntroduction();
                break;
            case 2:
                dangerousFlowers.sendIntroduction();
                break;
            case 3:
                rememberTheBlock.sendIntroduction();
                break;
            case 4:
                lastOfUS.sendIntroduction();
                break;
            default: return;
        }
        Utils.fillNeko(center);
        updateChunksLight();
        Utils.timer(20, 20 * 45, 5, it -> title("§e" + (5 - it), "§b准备时间即将结束!"), () -> {
            Utils.clearNeko(center);
            started = true;
            sound();
            shouldNotMove = true;
            setGameMode(GameMode.ADVENTURE);
            switch (stage) {
                case 1:
                    letsJump.init();
                    letsJump.teleport();
                    break;
                case 2:
                    dangerousFlowers.init();
                    dangerousFlowers.teleport();
                    break;
                case 3:
                    rememberTheBlock.init();
                    rememberTheBlock.teleport();
                    break;
                case 4:
                    lastOfUS.init();
                    lastOfUS.teleport();
            }
            Utils.timer(20, 5 * 20, 10, it -> title("§e" + (10 - it), "§b游戏即将开始!"), () -> {
                shouldNotMove = false;
                title("§a游戏开始!", "");
                sound();
                switch (stage) {
                    case 1:
                        letsJump.start();
                        break;
                    case 2:
                        dangerousFlowers.start();
                        break;
                    case 3:
                        rememberTheBlock.start();
                        break;
                    case 4:
                        lastOfUS.start();
                }
            });
        });
    }

    public int getRemainsCount() {
        return knockout;
    }

    public void setGameMode(final GameMode gameMode) {
        remains.forEach(it -> it.setGameMode(gameMode));
    }

    public void teleport(final Location location) {
        remains.forEach(it -> it.teleport(location));
    }

    public void knockout(final Player player) {
        if (!remains.remove(player)) return;
        player.setGameMode(GameMode.SPECTATOR);
        knockout--;
        ranking.addFirst(player.getName());
        if (knockout > 1) {
            title(player, "§c您已经被淘汰了!", "§b您可以继续观战");
            player.sendMessage("§c您已经被淘汰了! §e排名为: §b" + knockout);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1f, 1f);
            if (stage == 1) {
                letsJump.onKnockout(player);
            }
        }
        if (!started) return;
        switch (stage) {
            case 1:
                if (knockout <= letsJump.remainsCount) {
                    started = false;
                    letsJump.stop();
                    setGameMode(GameMode.SPECTATOR);
                    letsJump.clear();
                    Utils.later(60, () -> title("§a本轮游戏结束!", "§e当前剩余" + knockout + "名玩家!", 40));
                    nextGame();
                }
                break;
            case 2:
                if (knockout <= count / 5 * 3) {
                    started = false;
                    letsJump.stop();
                    dangerousFlowers.stop();
                    congratulate();
                    setGameMode(GameMode.SPECTATOR);
                    dangerousFlowers.clear();
                    Utils.later(60, () -> title("§a本轮游戏结束!", "§e当前剩余" + knockout + "名玩家!", 40));
                    nextGame();
                }
            case 3:
                if (knockout <= count / 4) {
                    started = false;
                    dangerousFlowers.stop();
                    rememberTheBlock.stop();
                    congratulate();
                    setGameMode(GameMode.SPECTATOR);
                    rememberTheBlock.clear();
                    Utils.later(60, () -> title("§a本轮游戏结束!", "§e当前剩余" + knockout + "名玩家!", 40));
                    nextGame();
                }
                break;
            case 4:
                if (knockout < 1) {
                    started = false;
                    lastOfUS.stop();
                    rememberTheBlock.stop();
                    congratulate();
                    setGameMode(GameMode.SPECTATOR);
                    lastOfUS.clear();
                    Utils.later(60, () -> title("§a游戏已结束!", "", 40));
                    Iterator<String> iterator = ranking.iterator();
                    Bukkit.broadcastMessage("§b§m          §r §a[§e最终排名§a] §b§m          ");
                    for (int i = 1; i <= 10 && iterator.hasNext(); i++) {
                        Bukkit.broadcastMessage("  §e" + i + ". §a" + iterator.next());
                    }
                    Bukkit.broadcastMessage("§b§m                                                          ");
                }
                break;
            default: return;
        }
        sound();
    }

    public void congratulate() {
        remains.forEach(this::congratulate);
    }

    public void congratulate(final Player it) {
        particle(it.getLocation());
        it.sendMessage("§a恭喜您晋级了!");
        it.sendTitle(TITLE);
    }

    public void title(final String title, final String sub) {
        final Title tmp = new Title(title, sub, 10, 40, 10);
        players.forEach(it -> {
            if (it.isOnline()) it.sendTitle(tmp);
        });
    }

    public void sound() {
        Bukkit.getOnlinePlayers().forEach(it -> {
            if (it.isOnline()) it.playSound(it.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        });
    }

    public void title(final String title, final String sub, final int time) {
        final Title tmp = new Title(title, sub, 10, 40, 10);
        Utils.later(time, () -> Bukkit.getOnlinePlayers().forEach(it -> {
            if (it.isOnline()) it.sendTitle(tmp);
        }));
    }

    public static void title(final Player player, final String title, final String sub) {
        if (player.isOnline()) player.sendTitle(new Title(title, sub, 10, 40, 10));
    }

    @EventHandler
    public void onEntityToggleGlide(final EntityToggleGlideEvent e) {
        if (e.getEntityType() != EntityType.PLAYER || !e.isGliding()) return;
        final Player player = (Player) e.getEntity();
        if (players.contains(player)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent e) {
        if (shouldNotMove && remains.contains(e.getPlayer())) e.setCancelled(true);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(final FoodLevelChangeEvent e) {
        if (remains.contains(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent e) {
        if (remains.contains(e.getPlayer())) knockout(e.getPlayer());
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @EventHandler
    public void onPlayerQuit(final EntityDamageEvent e) {
        if (!remains.contains(e.getEntity()) || (stage == 2 && e.getCause() == EntityDamageEvent.DamageCause.WITHER)) return;
        e.setDamage(0);
    }

    @EventHandler
    public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent e) {
        if (remains.contains(e.getPlayer())) e.setCancelled(true);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @EventHandler
    public void onEntityRegainHealth(final EntityRegainHealthEvent e) {
        if (remains.contains(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler
    public void onEntityRegainHealth(final PlayerSwapHandItemsEvent e) {
        if (remains.contains(e.getPlayer())) e.setCancelled(true);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @EventHandler
    public void onEntityRegainHealth(final InventoryClickEvent e) {
        if (remains.contains(e.getWhoClicked())) e.setCancelled(true);
    }

    @EventHandler
    public void onEntityRegainHealth(final PlayerItemHeldEvent e) {
        if (remains.contains(e.getPlayer())) e.setCancelled(true);
    }

    public void updateChunksLight() {
        int x = center.getChunk().getX(), z = center.getChunk().getZ();
        for (int i = 0; i < 6; i++) for (int j = 0; j < 6; j++) Utils.updateLight(center.getWorld().getChunkAt(x + i, z + j));
    }
}
