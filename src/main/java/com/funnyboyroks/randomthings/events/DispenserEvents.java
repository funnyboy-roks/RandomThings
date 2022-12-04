package com.funnyboyroks.randomthings.events;

import com.funnyboyroks.randomthings.RandomThings;
import com.funnyboyroks.randomthings.Util;
import io.papermc.paper.event.block.BlockBreakBlockEvent;
import io.papermc.paper.event.block.BlockPreDispenseEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;

public class DispenserEvents implements Listener {

    @EventHandler
    public void onPreDispense(BlockPreDispenseEvent event) {
        if (event.isCancelled()) return;
        ItemStack item = event.getItemStack(); // The ItemStack that gets used
        BlockFace facingDirection = ((Directional) event.getBlock().getBlockData()).getFacing(); // The direction in which the dispenser is facing
        Block facing = event.getBlock().getRelative(facingDirection); // The block that the dispenser is facing
        if ( // Dispenser Block Breaker
            item.getType().name().endsWith("_PICKAXE")
            || item.getType().name().endsWith("_SHOVEL")
            || item.getType().name().endsWith("_AXE")
            || item.getType() == Material.SHEARS
        ) {
            if (!RandomThings.config.dispenserBreakBlocks) return;
            event.setCancelled(true);
            if (facing.getType().getHardness() == -1) return;

            BlockBreakBlockEvent bbbEvent = new BlockBreakBlockEvent(facing, event.getBlock(), List.copyOf(facing.getDrops(item)));
            if (!bbbEvent.callEvent()) return;
            boolean broken = facing.breakNaturally(event.getItemStack(), true);
            if (broken) Util.applyDamage(item);

        } else if (item.getType().name().endsWith("_HOE")) { // Dispenser Till Block
            if (!RandomThings.config.dispenserTillBlocks) return;
            event.setCancelled(true);
            Material newType = switch (facing.getType()) {
                case DIRT, GRASS_BLOCK -> Material.FARMLAND;
                case COARSE_DIRT -> Material.DIRT;
                default -> null;
            };

            if (newType == null) return;

            facing.setType(newType);
            Util.applyDamage(item);


        } else if (item.getType().name().endsWith("BUCKET")) { // Dispenser Cauldrons
            if (!RandomThings.config.dispenserCauldrons) return;

            if (!facing.getType().name().endsWith("CAULDRON")) return;

            switch (item.getType()) {
                case WATER_BUCKET, LAVA_BUCKET, POWDER_SNOW_BUCKET, BUCKET -> event.setCancelled(true);
                default -> {
                    return;
                }
            }

            Container c = (Container) event.getBlock().getState();
            if (item.getType() == Material.BUCKET && facing.getType() != Material.CAULDRON) { // Bucket from cauldron
                if (facing.getBlockData() instanceof Levelled l && l.getLevel() != l.getMaximumLevel()) {
                    return;
                }

                item.setAmount(item.getAmount() - 1);
                HashMap<Integer, ItemStack> overflow = c.getInventory().addItem(new ItemStack(switch (facing.getType()) {
                    case WATER_CAULDRON -> Material.WATER_BUCKET;
                    case LAVA_CAULDRON -> Material.LAVA_BUCKET;
                    case POWDER_SNOW_CAULDRON -> Material.POWDER_SNOW_BUCKET;
                    default -> Material.AIR;
                }));
                facing.setType(Material.CAULDRON);

                if (!overflow.isEmpty()) {
                    event.getBlock().getWorld().dropItemNaturally(
                        event.getBlock().getLocation().clone().add(facingDirection.getDirection()),
                        new ItemStack(overflow.get(0).getType())
                    );

                }

            } else if (item.getType() != Material.BUCKET) { // Bucket into cauldron
                // Always replaces, no matter content (just like when right-clicking with a bucket)
                facing.setType(switch (item.getType()) {
                    case WATER_BUCKET -> Material.WATER_CAULDRON;
                    case LAVA_BUCKET -> Material.LAVA_CAULDRON;
                    case POWDER_SNOW_BUCKET -> Material.POWDER_SNOW_CAULDRON;
                    default -> Material.AIR;
                });


                if (facing.getBlockData() instanceof Levelled l) {
                    l.setLevel(l.getMaximumLevel());
                    facing.setBlockData(l);
                }

                item.setAmount(item.getAmount() - 1);
                HashMap<Integer, ItemStack> overflow = c.getInventory().addItem(new ItemStack(Material.BUCKET));

                if (!overflow.isEmpty()) { // If the item can't fit into the inventory, drop it in world
                    event.getBlock().getWorld().dropItemNaturally(
                        event.getBlock().getLocation().clone().add(facingDirection.getDirection()),
                        new ItemStack(overflow.get(0).getType())
                    );
                }


            }


        }
    }

}
