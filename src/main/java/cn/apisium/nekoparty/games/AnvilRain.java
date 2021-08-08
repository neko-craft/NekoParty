package cn.apisium.nekoparty.games;

import cn.apisium.nekoparty.Knockout;
import cn.apisium.nekoparty.Main;
import cn.apisium.nekoparty.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

public class AnvilRain extends Game {
    private final static Random random = new Random();
    private final static BlockData[] anvils = new BlockData[] {
            Bukkit.createBlockData(Material.ANVIL), Bukkit.createBlockData(Material.CHIPPED_ANVIL),
            Bukkit.createBlockData(Material.DAMAGED_ANVIL)
    };
    private final static Material[] types = new Material[] {
            Material.COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.MOSSY_STONE_BRICKS, Material.STONE_BRICKS,
            Material.CHISELED_STONE_BRICKS, Material.CRACKED_STONE_BRICKS, Material.STONE, Material.DEEPSLATE,
            Material.COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.MOSSY_STONE_BRICKS, Material.STONE_BRICKS,
            Material.CHISELED_STONE_BRICKS, Material.CRACKED_STONE_BRICKS, Material.STONE, Material.DEEPSLATE,
            Material.DEEPSLATE_BRICKS, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, Material.COAL_ORE,
            Material.DEEPSLATE_COAL_ORE, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE
    };
    private int count;
    private BukkitTask timer;
    private final int maxX, minX, maxY, minY, maxZ, minZ;
    public AnvilRain(Block block, Knockout knockout) {
        super(block, knockout);
        minX = block.getX() - 2;
        minZ = block.getZ() - 2;
        maxX = minX + 26;
        maxZ = minZ + 26;
        maxY = block.getY() + 10;
        minY = block.getY() - 15;
    }

    @Override
    public void init() {
        center.getWorld().setStorm(true);
        center.getWorld().setThundering(true);
        final Location loc = center.getLocation();
        for (int i = 0; i < 24; i++) for (int j = 0; j < 24; j++)
            loc.clone().add(i, 0, j).getBlock().setType(types[random.nextInt(types.length)]);
    }

    @Override
    public void start() {
        super.start();
        knockout.remains.forEach(it -> it.setFoodLevel(1));
        setItem(Utils.SWORD_WITH_KNOCKBACK);
        count = 5;
        Location loc = center.getLocation();
        World world = loc.getWorld();
        timer = Bukkit.getScheduler().runTaskTimer(Main.INSTANCE, () -> {
            for (int i = 0; i < count; i++) {
                FallingBlock e = world.spawnFallingBlock(
                        loc.clone().add(random.nextInt(24) + 0.5, 7 + random.nextInt(8), random.nextInt(24) + 0.5),
                        anvils[random.nextInt(3)]
                );
                e.setDropItem(false);
                e.setHurtEntities(true);
            }
            if (count < 120) count += 5;
        }, 100, 60);
    }

    @Override
    public void stop() {
        super.stop();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void clear() {
        center.getWorld().setThundering(false);
        center.getWorld().setStorm(false);
        final Location loc = center.getLocation();
        for (int i = 0; i < 24; i++) for (int j = 0; j < 24; j++) {
            Location l = loc.clone().add(i, 0, j);
            l.getBlock().setType(Material.AIR);
            l.add(0, 1, 0).getBlock().setType(Material.AIR);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendIntroduction() {
        knockout.title("§e铁砧雨!", "§b第三轮", true);
        Bukkit.broadcastMessage(getIntroduction("天空中将会随机生成铁砧, 若被砸到将会直接出局!", "你的目标是尽可能地活下去!"));
    }

    @Override
    public void teleport() {
        knockout.remains.forEach(it -> it.teleport(center.getLocation().clone()
                .add(2 + random.nextInt(20), 1, 2 + random.nextInt(20))));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getEntityType() == EntityType.PLAYER && e.getCause() == EntityDamageEvent.DamageCause.FALLING_BLOCK &&
            knockout.remains.contains((Player) e.getEntity())) {
            e.setDamage(12);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent e) {
        final Location loc = e.getFrom();
        if (world != loc.getWorld() || loc.getX() < minX || loc.getX() > maxX || loc.getY() < minY ||
                loc.getY() > maxY || loc.getZ() < minZ || loc.getZ() > maxZ) {
            knockout.knockout(e.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent e) {
        final Player player = e.getEntity();
        if (!knockout.remains.contains(player)) return;
        knockout.knockout(player);
        player.setHealth(20);
        e.setCancelled(true);
    }
}
