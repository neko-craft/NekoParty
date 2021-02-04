package cn.apisium.nekoparty;

import cn.apisium.nekoparty.games.LastOfUS;
import cn.apisium.nekoparty.games.LetsJump;
import cn.apisium.nekoparty.games.RememberTheBlock;
import com.destroystokyo.paper.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public final class Knockout implements Listener {
    private final Location center;
    private final LetsJump letsJump;
    private final LastOfUS lastOfUS;
    private final RememberTheBlock rememberTheBlock;
    private final Set<Player> players;
    public final Set<Player> remains;
    private final LinkedList<String> ranking = new LinkedList<>();
    private int knockout, stage;
    private final int count;
    private boolean started = false;
    public Knockout(final Block center, final Set<Player> players) {
        this.players = players;
        this.center = center.getLocation();
        knockout = count = players.size();
        remains = new HashSet<>(players);
        letsJump = new LetsJump(center, this);
        lastOfUS = new LastOfUS(center, this);
        rememberTheBlock = new RememberTheBlock(center, this);
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, Main.INSTANCE);
        setGameMode(GameMode.SPECTATOR);
        teleport(center);
    }

    public void clear() {
        letsJump.clear();
        lastOfUS.clear();
        rememberTheBlock.clear();
    }

    private void nextGame() {
        switch (++stage) {
            case 1:
                letsJump.sendIntroduction();
                break;
            case 2:
                rememberTheBlock.sendIntroduction();
                break;
            case 3:
                lastOfUS.sendIntroduction();
                break;
            default: return;
        }
        Utils.timer(20, 20 * 55, 5, it -> title("§e" + (5 - it) + "!", "§b准备时间即将结束!"), () -> {
            started = true;
            sound();
            switch (stage) {
                case 1:
                    letsJump.init();
                    letsJump.teleport();
                    break;
                case 2:
                    rememberTheBlock.init();
                    rememberTheBlock.teleport();
                case 3:
                    lastOfUS.init();
                    lastOfUS.teleport();
            }
            setGameMode(GameMode.ADVENTURE);
            Utils.timer(20, 5 * 20, 10, it -> title("§e" + (10 - it) + "!", "§b游戏即将开始!"), () -> {
                title("§a游戏开始!", "");
                sound();
                switch (stage) {
                    case 1:
                        letsJump.start();
                        break;
                    case 2:
                        rememberTheBlock.start();
                        break;
                    case 3:
                        lastOfUS.start();
                }
            });
        });
    }

    public void setGameMode(final GameMode gameMode) {
        remains.forEach(it -> it.setGameMode(gameMode));
    }

    public void teleport(final Location location) {
        remains.forEach(it -> it.teleport(location));
    }

    public void knockout(final Player player) {
        remains.remove(player);
        player.setGameMode(GameMode.SPECTATOR);
        knockout--;
        ranking.addFirst(player.getName());
        if (knockout != 0) {
            title(player, "§c您已经被淘汰了!", "§b您可以继续观战");
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
                    title("§a本轮游戏结束!", "§e当前剩余" + knockout + "名玩家!", 40);
                    nextGame();
                }
                break;
            case 2:
                if (knockout < count / 4) {
                    started = false;
                    rememberTheBlock.stop();
                    setGameMode(GameMode.SPECTATOR);
                    rememberTheBlock.clear();
                    title("§a本轮游戏结束!", "§e当前剩余" + knockout + "名玩家!", 40);
                    nextGame();
                }
                break;
            case 3:
                if (knockout == 0) {
                    started = false;
                    lastOfUS.stop();
                    setGameMode(GameMode.SPECTATOR);
                    lastOfUS.clear();
                    title("§a游戏已结束!", "", 40);
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

    public void title(final String title, final String sub) {
        final Title tmp = new Title(title, sub, 10, 40, 10);
        players.forEach(it -> {
            if (it.isOnline()) it.sendTitle(tmp);
        });
    }

    public void sound() {
        players.forEach(it -> {
            if (it.isOnline()) it.playSound(it.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        });
    }

    public void title(final String title, final String sub, final int time) {
        final Title tmp = new Title(title, sub, 10, 40, 10);
        Utils.later(time, () -> players.forEach(it -> {
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

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent e) {
        if (remains.contains(e.getPlayer())) knockout(e.getPlayer());
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @EventHandler
    public void onPlayerQuit(final EntityDamageEvent e) {
        if (remains.contains(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent e) {
        if (started && remains.contains(e.getPlayer())) e.setCancelled(true);
    }
}
