package cn.apisium.nekoparty;

import cn.apisium.nekoparty.games.*;
import com.destroystokyo.paper.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

@SuppressWarnings("deprecation")
public final class Knockout implements Listener {
    private final Location center, centerTemp;
    private final Set<Player> players;
    private final Game[] games;
    public final Set<Player> remains;
    private final LinkedList<String> ranking = new LinkedList<>();
    private int knockout, stage;
    public final int count;
    private boolean started, shouldNotMove;
    private BukkitTask task;

    private static final Title TITLE = new Title("§a恭喜您晋级了!", "", 10, 40, 10);

    @SafeVarargs
    public Knockout(final Block center, final Set<Player> players, Class<? extends Game> ...games) {
        this.players = players;
        this.center = center.getLocation();
        centerTemp = this.center.clone().add(18, -55, 18);
        knockout = count = players.size();
        remains = new HashSet<>(players);
        this.games = Arrays.stream(games).map(it -> {
            try {
                return (Game) it.getConstructor(Block.class, Knockout.class).newInstance(center, this);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }).toArray(Game[]::new);
    }

    public void start() {
        if (players.size() < 4) throw new RuntimeException("人数过少!");
        for (Game it : games) it.clear();
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
        for (Game it : games) it.clear();
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
        for (Game it : games) it.stop();
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
        if (++stage > games.length) return;
        getCurrentGame().sendIntroduction();
        Utils.fillNeko(center);
        updateChunksLight();
        Utils.timer(20, 20 * 45, 5, it -> title("§e" + (5 - it), "§b准备时间即将结束!"), () -> {
            Utils.clearNeko(center);
            started = true;
            sound();
            shouldNotMove = true;
            setGameMode(GameMode.ADVENTURE);
            getCurrentGame().init();
            getCurrentGame().teleport();
            Utils.timer(20, 5 * 20, 10, it -> title("§e" + (10 - it), "§b游戏即将开始!"), () -> {
                shouldNotMove = false;
                title("§a游戏开始!", "");
                sound();
                games[stage - 1].start();
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

    public Game getCurrentGame() { return games[stage - 1]; }

    public void knockout(final Player player) {
        if (!remains.remove(player)) return;
        player.setGameMode(GameMode.SPECTATOR);
        player.getInventory().clear();
        player.updateInventory();
        knockout--;
        ranking.addFirst(player.getName());
        if (knockout > 1) {
            title(player, "§c您已经被淘汰了!", "§b您可以继续观战");
            player.sendMessage("§c您已经被淘汰了! §e排名为: §b" + knockout);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1f, 1f);
            getCurrentGame().onKnockout(player);
        }
        if (!started) return;
        int remainsCount = getCurrentGame().getRemainCount();
        if (stage > games.length) return;
        if ((remainsCount != -1 && knockout <= remainsCount) || knockout < 1 ||
                knockout <= count * (stage / games.length)) {
            started = false;
            getCurrentGame().stop();
            setGameMode(GameMode.SPECTATOR);
            getCurrentGame().clear();
            if (knockout < 1) {
                Utils.later(60, () -> title("§a游戏已结束!", "", 40));
                Iterator<String> iterator = ranking.iterator();
                Bukkit.broadcastMessage("§b§m          §r §a[§e最终排名§a] §b§m          ");
                for (int i = 1; i <= 16 && iterator.hasNext(); i++) {
                    Bukkit.broadcastMessage("  §e" + i + ". §a" + iterator.next());
                }
                Bukkit.broadcastMessage("§b§m                                                          ");
            } else {
                congratulate();
                Utils.later(60, () -> title("§a本轮游戏结束!", "§e当前剩余" + knockout + "名玩家!", 40));
                nextGame();
            }
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

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent e) {
        if (!e.getPlayer().isOp()) e.setCancelled(true);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @EventHandler(ignoreCancelled = true)
    public void onEntityRegainHealth(final EntityRegainHealthEvent e) {
        if (remains.contains(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSwapHandItems(final PlayerSwapHandItemsEvent e) {
        if (remains.contains(e.getPlayer())) e.setCancelled(true);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent e) {
        if (remains.contains(e.getWhoClicked())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityRegainHealth(final PlayerItemHeldEvent e) {
        if (remains.contains(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawn(final EntitySpawnEvent e) {
        if (e.getEntity() instanceof Monster && centerTemp.distanceSquared(e.getLocation()) <= 4900) e.setCancelled(true);
    }

    public void updateChunksLight() {
        int x = center.getChunk().getX(), z = center.getChunk().getZ();
        for (int i = 0; i < 6; i++) for (int j = 0; j < 6; j++) Utils.updateLight(center.getWorld().getChunkAt(x + i, z + j));
    }
}
