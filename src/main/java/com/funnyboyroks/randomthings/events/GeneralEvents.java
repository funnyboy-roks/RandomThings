package com.funnyboyroks.randomthings.events;

import com.funnyboyroks.randomthings.RandomThings;
import com.funnyboyroks.randomthings.Util;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class GeneralEvents implements Listener {

    public static final Map<UUID, LongAdder> TO_BE_PLANTED = new ConcurrentHashMap<>(); // UUID : TicksLeft

    @EventHandler
    public void onLightningStrike(LightningStrikeEvent event) {
        if (!RandomThings.config.renewableSponges) return;
        event.getLightning()
            .getNearbyEntities(4, 4, 4) // Radius of 4 pulled from wiki for pigs, villagers, and mooshrooms
            .stream()
            .filter((e) -> e instanceof Guardian)
            .map(e -> (Guardian) e)
            .forEach(g -> {
                // Unfortunately no easy way to convert guardian into elder anymore, so have to manually copy data
                ElderGuardian elder = g.getWorld().spawn(g.getLocation(), ElderGuardian.class, CreatureSpawnEvent.SpawnReason.LIGHTNING);
                // Main data that is noticeable.
                elder.customName(g.customName());
                elder.addPotionEffects(g.getActivePotionEffects());
                g.remove();
            });
    }


    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!RandomThings.config.silverFishDropGravel) return;
        Material from = event.getBlock().getType();
        Material to = event.getTo();

        if (from.name().startsWith("INFESTED") && to.isAir()) { // Silverfish drop gravel
            ItemStack stack = new ItemStack(Material.GRAVEL);
            event.getBlock().getWorld().dropItem(event.getBlock().getLocation().clone().add(.5, .5, .5), stack);
        }
    }


    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!RandomThings.config.vacuumHoppers) return;
        // Remove vacuum hopper upon death
        RandomThings.getDataHandler().data.vacuumHoppers.remove(event.getEntity().getUniqueId());
    }


    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!RandomThings.config.renewableBlackstone) return;
        if (event.getBlock().getType() == Material.LAVA) { // Renewable Blackstone
            boolean hasBlueIce = false;
            Block toBlock = event.getToBlock();

            // Check that an adjacent face has blue ice
            for (BlockFace face : Util.ADJACENT_FACES) {
                if (toBlock.getRelative(face).getType() == Material.BLUE_ICE) {
                    hasBlueIce = true;
                    break;
                }
            }
            if (!hasBlueIce) return;

            if (toBlock.getRelative(BlockFace.DOWN).getType() != Material.SOUL_SAND) return;

            event.setCancelled(true);

            // Set the block
            toBlock.getWorld().getBlockAt(toBlock.getLocation()).setType(Material.BLACKSTONE);


            // Give the particle and sound -- similar to cobblestone/stone/deepslate
            Location loc = toBlock.getLocation().clone().add(.5, .5, .5);
            event.getBlock().getWorld().spawnParticle(
                Particle.SMOKE_LARGE,
                loc,
                2,
                .25,
                .5,
                .25,
                0
            );
            toBlock.getWorld().playSound(
                Sound.sound(
                    Key.key("block.lava.extinguish"),
                    Sound.Source.BLOCK,
                    5,
                    2
                ),
                loc.getX(), loc.getY(), loc.getZ()
            );
        }
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        if (
            event.getNewState().getType() == Material.STONE
            && event.getBlock().getRelative(BlockFace.DOWN).getType() == Material.ANDESITE
        ) { // Renewable Deepslate
            if(!RandomThings.config.renewableAndesite) return;
            event.getNewState().setType(Material.ANDESITE);
            return;
        }
        if (event.getBlock().getLocation().getBlockY() < 0) { // Renewable Deepslate
            if (!RandomThings.config.renewableDeepslate) return;
            Material newMaterial = switch (event.getNewState().getType()) {
                case COBBLESTONE -> Material.COBBLED_DEEPSLATE;
                case STONE -> Material.DEEPSLATE;
                default -> null;
            };

            if (newMaterial == null) return;
            event.getNewState().setType(newMaterial);
        }
    }

    @EventHandler
    public void onPistonPush(BlockPistonExtendEvent event) {
        if (event.isCancelled()) return;

        if (!RandomThings.config.movableAmethyst) return;

        Set<Block> blocksSet = Set.copyOf(event.getBlocks());
        BlockFace dir = event.getDirection();

        /*/ Handle Movable Amethyst /*/

        // Re-calculate push limit to account for amethyst
        long pushSize = blocksSet.stream().filter(f -> f.getPistonMoveReaction() != PistonMoveReaction.BREAK || f.getType() == Material.BUDDING_AMETHYST).count();
        if (pushSize > 12) {
            event.setCancelled(true);
            return;
        }

        // Check if the amethyst can be moved -- if not, let the event happen and "crush" the amethyst.
        for (Block block : event.getBlocks()) {
            Block dest = block.getRelative(dir);
            if (!blocksSet.contains(dest) && dest.getPistonMoveReaction() != PistonMoveReaction.BREAK && !dest.getType().isAir()) {
                return;
            }
        }

        // Move the amethyst
        List<Map.Entry<Material, Block>> blocks = event.getBlocks().stream().map(b -> Map.entry(b.getType(), b)).toList();
        Bukkit.getScheduler().runTaskLater(RandomThings.INSTANCE, () -> {
            for (Map.Entry<Material, Block> block : blocks) {
                Block dest = block.getValue().getRelative(dir);
                if (block.getKey() == Material.BUDDING_AMETHYST) {
                    dest.setType(Material.BUDDING_AMETHYST);
                }
            }
        }, 1);
    }

    @EventHandler
    public void onItemDrop(ItemSpawnEvent event) {
        if (event.isCancelled() || !RandomThings.config.autoSaplings) return;

        Item item = event.getEntity();
        if (!Util.isSapling(item.getItemStack().getType())) return;
        int[] timer = { 0 }; // God damn, Java is annoying
        timer[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(RandomThings.INSTANCE, () -> {
            if (Util.isOnSaplingBlock(item)) {
                LongAdder la = new LongAdder();
                la.add(Util.randInt(5 * 20, 15 * 20));
                TO_BE_PLANTED.put(item.getUniqueId(), la);
                Bukkit.getScheduler().cancelTask(timer[0]);
            }
        }, 0, 10);
        Bukkit.getScheduler().runTaskLater(RandomThings.INSTANCE, () -> Bukkit.getScheduler().cancelTask(timer[0]), 20 * 10);
    }

    @EventHandler
    public void onItemPickup(ItemDespawnEvent event) {
        TO_BE_PLANTED.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        TO_BE_PLANTED.remove(event.getItem().getUniqueId());
    }

    @EventHandler
    public void onItemPickup(InventoryPickupItemEvent event) {
        TO_BE_PLANTED.remove(event.getItem().getUniqueId());
    }

}
