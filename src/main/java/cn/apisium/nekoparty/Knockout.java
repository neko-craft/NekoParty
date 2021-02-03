package cn.apisium.nekoparty;

import cn.apisium.nekoparty.games.LastOfUS;
import cn.apisium.nekoparty.games.LetsJump;
import cn.apisium.nekoparty.games.RememberTheBlock;
import com.destroystokyo.paper.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Knockout implements Listener {
    private final Location center;
    private final LetsJump letsJump;
    private final LastOfUS lastOfUS;
    private final RememberTheBlock rememberTheBlock;
    private final Set<Player> players;
    public final Set<Player> remains;
    private final HashMap<UUID, Integer> ranking = new HashMap<>();
    private int knockout, count, stage = 0;
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

    private void nextGame() {
        switch (++stage) {
            case 1: break;
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
            switch (stage) {
                case 1:
                    break;
                case 2:
                    rememberTheBlock.init();
                    rememberTheBlock.teleport();
                case 3:
                    lastOfUS.init();
                    lastOfUS.teleport();
                    break;
            }
            setGameMode(GameMode.ADVENTURE);
            Utils.timer(20, 5 * 20, 10, it -> title("§e" + (10 - it) + "!", "§b游戏即将开始!"), () -> {
                title("§a游戏开始!", "");
                switch (stage) {
                    case 1:
                        break;
                    case 2:
                        rememberTheBlock.start();
                    case 3:
                        lastOfUS.start();
                        break;
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
        ranking.put(player.getUniqueId(), knockout--);
        if (knockout != 0) {
            title(player, "§c您已经被淘汰了!", "§b您可以继续观战");
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
                    title("§a游戏已结束!", "§b稍后将会进行排名计算!", 40);
                }
        }
    }

    public void title(final String title, final String sub) {
        final Title tmp = new Title(title, sub, 10, 40, 10);
        players.forEach(it -> {
            if (it.isOnline()) it.sendTitle(tmp);
        });
    }

    public void title(final String title, final String sub, final int time) {
        final Title tmp = new Title(title, sub, 10, 40, 10);
        Utils.later(time, () -> players.forEach(it -> {
            if (it.isOnline()) it.sendTitle(tmp);
        }));
    }

    public void messages(final String msg) {
        players.forEach(it -> {
            if (it.isOnline()) it.sendMessage(msg);
        });
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
