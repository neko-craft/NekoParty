package cn.apisium.nekoparty.games;

import cn.apisium.nekoparty.Knockout;
import cn.apisium.nekoparty.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Random;

public final class RememberTheBlock extends Game {
    private final static Random random = new Random();
    private final Material[][] grids = new Material[5][5];
    private final int maxX, minX, maxY, minY, maxZ, minZ;
    public RememberTheBlock(Block block, final Knockout knockout) {
        super(block, knockout);
        minX = block.getX();
        minZ = block.getZ();
        maxX = minX + 20;
        maxZ = minZ + 20;
        maxY = block.getY() + 20;
        minY = block.getY() - 20;
    }

    @Override
    public void init() {
        fillMaskBlocks();
    }

    @Override
    public void start() {
        if (started) return;
        super.start();
        onceProcess();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void clear() {
        fillAnswerBlocks(Material.AIR);
        final Location loc = center.getLocation();
        for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++) {
            for (int t = 0; t < 4; t++) for (int k = 0; k < 4; k++) {
                loc.clone().add(i * 4 + t, 0, j * 4 + k).getBlock().setType(Material.AIR);
            }
        }
    }

    @Override
    public void sendIntroduction() {
        knockout.title("§e记住方块", "§b第二轮");
        Bukkit.broadcastMessage("§b§m          §r §a[§e游戏介绍§a] §b§m          \n§a  玩家将被传送到平台上, 游戏开始后, 玩家需要记住周围的方块类型.\n§b  之后平台顶部将会出现一种随机类型的方块, 玩家需要站在之前出现的同种方块的平台上, 否则就会掉下去!\n§b§m                                                          §r\n");
    }

    @Override
    public void teleport() {
        knockout.remains.forEach(it -> it.teleport(center.getLocation().clone().add(2 + random.nextInt(16), 1, 2 + random.nextInt(16))));
    }

    private void onceProcess() {
        for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++) grids[i][j] = randomMaterial();
        fillAllBlocks();
        Utils.timer(20, 20 * 20, 5, it -> knockout.title("§e" + (5 - it) + "!", ""), () -> {
            fillMaskBlocks();
            knockout.title("§b正确方块将出现在上方!", "");
            Utils.later(20 * 2, () -> {
                final Material type = randomMaterial();
                fillAnswerBlocks(type);
                knockout.sound();
                Utils.later(20 * 5, () -> clearBlocks(type));
                Utils.later(20 * 12, () -> {
                    fillAnswerBlocks(Material.AIR);
                    fillMaskBlocks();
                    knockout.sound();
                    Utils.later(20 * 3, () -> {
                        knockout.title("§e下一轮即将开始!", "");
                        onceProcess();
                    });
                });
            });
        });
    }

    private void fillAnswerBlocks(final Material type) {
        final Location loc = center.getLocation();
        for (int t = 0; t < 4; t++) for (int k = 0; k < 4; k++) {
            loc.clone().add(8 + t, 10, 8 + k).getBlock().setType(type);
        }
    }

    private void fillAllBlocks() {
        final Location loc = center.getLocation();
        for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++) {
            Material type = grids[i][j];
            for (int t = 0; t < 4; t++) for (int k = 0; k < 4; k++) {
                loc.clone().add(i * 4 + t, 0, j * 4 + k).getBlock().setType(type);
            }
        }
    }

    private void clearBlocks(final Material type) {
        final Location loc = center.getLocation();
        for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++) {
            final Material tmp = grids[i][j] == type ? type : Material.AIR;
            for (int t = 0; t < 4; t++) for (int k = 0; k < 4; k++) {
                loc.clone().add(i * 4 + t, 0, j * 4 + k).getBlock().setType(tmp);
            }
        }
    }

    private void fillMaskBlocks() {
        final Location loc = center.getLocation();
        for (int i = 0, g = 0; i < 5; i++) for (int j = 0; j < 5; j++) {
            Material type = g++ % 2 == 0 ? Material.BLACK_CONCRETE : Material.WHITE_CONCRETE;
            for (int t = 0; t < 4; t++) for (int k = 0; k < 4; k++) {
                loc.clone().add(i * 4 + t, 0, j * 4 + k).getBlock().setType(type);
            }
        }
    }

    private static Material randomMaterial() {
        switch (random.nextInt(5)) {
            case 1: return Material.IRON_BLOCK;
            case 2: return Material.GOLD_BLOCK;
            case 3: return Material.DIAMOND_BLOCK;
            case 4: return Material.NETHERITE_BLOCK;
            default: return Material.EMERALD_BLOCK;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent e) {
        final Location loc = e.getFrom();
        if (world != loc.getWorld() || loc.getX() < minX || loc.getX() > maxX || loc.getY() < minY ||
                loc.getY() > maxY || loc.getZ() < minZ || loc.getZ() > maxZ) {
            if (knockout.remains.contains(e.getPlayer())) knockout.knockout(e.getPlayer());
        }
    }
}
