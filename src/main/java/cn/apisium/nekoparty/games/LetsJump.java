package cn.apisium.nekoparty.games;

import cn.apisium.nekoparty.Knockout;
import cn.apisium.nekoparty.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Random;

public final class LetsJump extends Game {
    public int remainsCount;
    private final static Random random = new Random();
    private boolean[][] grids = new boolean[12][12];
    private final int minX, minZ, maxX, maxZ, maxY, minY, rightX;
    private final Location leftSide;
    private final HashSet<Block> removedBlocks = new HashSet<>();
    private BukkitTask task;
    private final HashSet<Player> promotions = new HashSet<>();
    public LetsJump(Block block, Knockout knockout) {
        super(block, knockout);
        leftSide = block.getLocation().add(-4, 1, 12);
        int tmp = block.getX();
        minX = tmp - 6;
        minZ = block.getZ();
        maxX = tmp + 30;
        maxZ = minZ + 24;
        maxY = block.getY() + 20;
        minY = block.getY() - 20;
        rightX = tmp + 25;
    }

    @Override
    public void onKnockout(Player player) {
        checkPlayerCount();
    }

    private void calcPromotionCount() {
        remainsCount = knockout.remains.size() / 4 * 3;
    }
    private void checkPlayerCount() {
        calcPromotionCount();
        if (remainsCount > promotions.size()) return;
        knockout.remains.forEach(it -> {
            if (!promotions.contains(it)) knockout.knockout(it);
        });
    }

    private void teleportPlayerToLeftSide(final Player player) {
        player.teleport(leftSide.clone().add(Math.random() * 2 - 1, 0, Math.random() * 10 - 5));
    }

    @Override
    public void init() {
        promotions.clear();
        calcPromotionCount();
        final Location loc = center.getLocation();
        grids = new boolean[12][12];
        boolean left = false, right = false;
        int prev = 4 + random.nextInt(6);
        for (int i = 0; i < 12;) {
            grids[i][prev] = true;
            boolean canLeft = !right && prev > 0, canRight = !left && prev < 11;
            random.nextBoolean();
            if ((canLeft || canRight) && random.nextBoolean()) {
                if (canLeft && canRight) {
                    if (random.nextBoolean()) {
                        left = true;
                        prev -= 1;
                    } else {
                        right = true;
                        prev += 1;
                    }
                } else prev += canLeft ? -1 : 1;
                grids[i][prev] = true;
            } else {
                left = right = false;
                i++;
            }
        }
        for (int i = 0; i < 12; i++) for (int j = 0; j < 12; j++) loc.clone().add(i * 2, 0, j * 2)
                .getBlock().setType(Material.IRON_BLOCK);

        Location loc2 = loc.clone().add(-2, 0, 0);
        for (int i = 0; i < 5; i++) for (int j = 0; j < 24; j++) loc2.clone().add(-i, 0, j).getBlock().setType(Material.LIGHT_BLUE_CONCRETE);
        loc2 = loc.clone().add(24, 0, 0);
        for (int i = 0; i < 5; i++) for (int j = 0; j < 24; j++) loc2.clone().add(i, 0, j).getBlock().setType(Material.PINK_CONCRETE);
    }

    @Override
    public void clear() {
        final Location loc = center.getLocation();
        for (int i = 0; i < 12; i++) for (int j = 0; j < 12; j++) loc.clone().add(i * 2, 0, j * 2)
                .getBlock().setType(Material.AIR);
        Location loc2 = loc.clone().add(-2, 0, 0);
        for (int i = 0; i < 5; i++) for (int j = 0; j < 24; j++) loc2.clone().add(-i, 0, j).getBlock().setType(Material.AIR);
        loc2 = loc.clone().add(24, 0, 0);
        for (int i = 0; i < 5; i++) for (int j = 0; j < 24; j++) loc2.clone().add(i, 0, j).getBlock().setType(Material.AIR);
    }

    @Override
    public void start() {
        super.start();
        task = Utils.timer(20 * 20, $ -> {
            removedBlocks.forEach(it -> it.setType(Material.IRON_BLOCK));
            removedBlocks.clear();
        });
    }

    @Override
    public void stop() {
        super.stop();
        task.cancel();
    }

    @Override
    public void sendIntroduction() {
        knockout.title("§e跳跃吧", "§b第一轮");
        knockout.messages("§b§m          §r §a[§e游戏介绍§a] §b§m          \n§a  玩家需要在前方的方块中找到正确的路最终到达终点.\n§b  你的目标是尽快到达终点, 否则将会被淘汰!\n§b§m                                                          §r\n");
    }

    @Override
    public void teleport() {
        knockout.remains.forEach(this::teleportPlayerToLeftSide);
    }

    @EventHandler
    public void onPlayerToggleSprint(final PlayerToggleSprintEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent e) {
        final Player player = e.getPlayer();
        if (!knockout.remains.contains(player) || promotions.contains(player)) return;
        final Location loc = e.getFrom().add(0, -1, 0);
        if (world != loc.getWorld() || loc.getY() < minY || loc.getY() > maxY || loc.getX() < minX ||
                loc.getX() > maxX || loc.getZ() < minZ || loc.getZ() > maxZ) {
            teleportPlayerToLeftSide(player);
            return;
        }
        if (loc.getX() > rightX) {
            promotions.add(player);
            checkPlayerCount();
            return;
        }
        final Block block = loc.getBlock();
        if (block.getType() != Material.IRON_BLOCK || removedBlocks.contains(block)) return;
        final Location loc2 = loc.subtract(center.getLocation());
        if (!grids[loc2.getBlockX() / 2][loc2.getBlockZ() / 2]) {
            removedBlocks.add(block);
            block.setType(Material.AIR);
        }
    }
}