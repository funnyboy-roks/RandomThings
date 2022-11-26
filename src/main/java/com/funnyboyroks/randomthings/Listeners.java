package com.funnyboyroks.randomthings;

import io.papermc.paper.event.block.BlockBreakBlockEvent;
import io.papermc.paper.event.block.BlockPreDispenseEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.*;

public class Listeners implements Listener {

    private static final Random        RNG            = new Random();
    private static final BlockFace[]   ADJACENT_FACES = {
        BlockFace.DOWN, BlockFace.UP,
        BlockFace.EAST, BlockFace.WEST,
        BlockFace.NORTH, BlockFace.SOUTH,
        };
    private static final Set<Material> PLANTS         = Set.of(
        Material.POTATOES,
        Material.CARROTS,
        Material.WHEAT,
        Material.BEETROOTS
    );

    @EventHandler
    public void onLightningStrike(LightningStrikeEvent event) {
        if (!RandomThings.config.renewableSponges) return;
        event.getLightning()
            .getNearbyEntities(4, 4, 4)
            .stream()
            .filter((e) -> e instanceof Guardian)
            .map(e -> (Guardian) e)
            .forEach(g -> {
                ElderGuardian elder = g.getWorld().spawn(g.getLocation(), ElderGuardian.class, CreatureSpawnEvent.SpawnReason.LIGHTNING);
                elder.customName(g.customName());
                elder.addPotionEffects(g.getActivePotionEffects());
                g.remove();
            });
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!RandomThings.config.twerkBonemeal) return;
        if (event.isSneaking()) {
            doBonemeal(event.getPlayer());
        }
    }

    public void doBonemeal(Player player) {
        for (int attempt = 0; attempt < 10; ++attempt) {
            for (int i = 0; i < 10; ++i) {
                Location loc = player.getLocation().clone();
                double radius = 2.5;
                loc.add(
                    Math.random() * radius * 2 - radius,
                    Math.random() * radius * 2 - radius,
                    Math.random() * radius * 2 - radius
                );
                Block block = player.getWorld().getBlockAt(loc);

                if (block.getType() != Material.GRASS_BLOCK) {
                    if (block.applyBoneMeal(BlockFace.UP)) i++;
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!RandomThings.config.rightClickHarvestCrops) return;
        if (event.getAction().isRightClick() && event.hasBlock()) {
            Block clicked = event.getClickedBlock();

            Material type = clicked.getType();

            if (PLANTS.contains(type)) {

                Ageable data = (Ageable) clicked.getBlockData();
                if (data.getAge() != data.getMaximumAge()) return;

                event.setUseInteractedBlock(Event.Result.DENY);
                event.setUseItemInHand(Event.Result.DENY);

                clicked.breakNaturally(true); // Break it to have the particles and drop the items


                clicked.getWorld().getBlockAt(clicked.getLocation()).setType(type);
                event.getPlayer().swingMainHand(); // Swing the hand to make it look intended
                event.setCancelled(true);
            }


        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!RandomThings.config.silverFishDropGravel) return;
        Material from = event.getBlock().getType();
        Material to = event.getTo();

        if (from.name().startsWith("INFESTED") && to.isAir()) {
            ItemStack stack = new ItemStack(Material.GRAVEL);
            event.getBlock().getWorld().dropItem(event.getBlock().getLocation().clone().add(.5, .5, .5), stack);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!RandomThings.config.vacuumHoppers) return;
        Location l = event.getBlock().getLocation().add(0, -1, 0);
        Block hopperBlock = l.getWorld().getBlockAt(l);
        if (hopperBlock.getType() != Material.HOPPER || event.getBlockPlaced().getType() != Material.PURPLE_CARPET) {
            return;
        }

        Marker e = l.getWorld().spawn(l.add(.5, 1, .5), Marker.class);
        e.setGravity(false);
        RandomThings.getDataHandler().data.vacuumHoppers.add(e.getUniqueId());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!RandomThings.config.vacuumHoppers) return;
        RandomThings.getDataHandler().data.vacuumHoppers.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!RandomThings.config.vacuumHoppers) return;
        if (event.getBlock().getType() == Material.HOPPER || event.getBlock().getType() == Material.PURPLE_CARPET) {
            Location asLoc = event.getBlock().getLocation().add(.5, 0, .5);
            if (event.getBlock().getType() == Material.HOPPER) asLoc.add(0, 1, 0);

            UUID standId = RandomThings.getDataHandler().data.vacuumHoppers
                .stream()
                .filter(s -> Bukkit.getEntity(s) != null && Bukkit.getEntity(s).getLocation().distanceSquared(asLoc) < .2)
                .findFirst()
                .orElse(null);

            if (standId == null) return;

            Entity stand = Bukkit.getEntity(standId);

            if (stand == null) return;

            stand.remove();
            RandomThings.getDataHandler().data.vacuumHoppers.remove(standId);


        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!RandomThings.config.renewableBlackstone) return;
        if (event.getBlock().getType() == Material.LAVA) {
            boolean hasBlueIce = false;
            Block toBlock = event.getToBlock();
            for (BlockFace face : ADJACENT_FACES) {
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


            // Give the particle and sound
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
        if (!RandomThings.config.renewableDeepslate) return;
        if (event.getBlock().getLocation().getBlockY() < 0) {
            Material newMaterial = switch (event.getNewState().getType()) {
                case COBBLESTONE -> Material.COBBLED_DEEPSLATE;
                case STONE -> Material.DEEPSLATE;
                default -> null;
            };

            if (newMaterial == null) return;
            event.getNewState().setType(newMaterial);

        }
    }

    public void applyDamage(ItemStack item) {
        if (!(item.getItemMeta() instanceof Damageable dmg)) return;
        double chance = 1 / (double) (item.getEnchantmentLevel(Enchantment.DURABILITY) + 1);
        if (RNG.nextDouble() <= chance) dmg.setDamage(dmg.getDamage() + 1);
        item.setItemMeta(dmg);
        if (dmg.getDamage() >= item.getType().getMaxDurability()) item.setAmount(0);
    }

    @EventHandler
    public void onPreDispense(BlockPreDispenseEvent event) {
        if (event.isCancelled()) return;
        ItemStack item = event.getItemStack();
        BlockFace facingDirection = ((Directional) event.getBlock().getBlockData()).getFacing();
        Block facing = event.getBlock().getRelative(facingDirection);
        if (
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
            if (broken) applyDamage(item);

        } else if (item.getType().name().endsWith("_HOE")) {
            if (!RandomThings.config.dispenserTillBlocks) return;
            event.setCancelled(true);
            Material newType = switch (facing.getType()) {
                case DIRT, GRASS_BLOCK -> Material.FARMLAND;
                case COARSE_DIRT -> Material.DIRT;
                default -> null;
            };

            if (newType == null) return;

            facing.setType(newType);
            applyDamage(item);


        } else if (item.getType().name().endsWith("BUCKET")) {
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

            } else if (item.getType() != Material.BUCKET) {
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

                if (!overflow.isEmpty()) {
                    event.getBlock().getWorld().dropItemNaturally(
                        event.getBlock().getLocation().clone().add(facingDirection.getDirection()),
                        new ItemStack(overflow.get(0).getType())
                    );
                }


            }


        }
    }

    @EventHandler
    public void onPistonPush(BlockPistonExtendEvent event) {
        if (event.isCancelled()) return;

        if (!RandomThings.config.movableAmethyst) return;

        var blocksSet = Set.copyOf(event.getBlocks());
        var dir = event.getDirection();

        long pushSize = blocksSet.stream().filter(f -> f.getPistonMoveReaction() != PistonMoveReaction.BREAK || f.getType() == Material.BUDDING_AMETHYST).count();
        if (pushSize > 12) {
            event.setCancelled(true);
            return;
        }

        for (var block : event.getBlocks()) {
            var dest = block.getRelative(dir);
            if (!blocksSet.contains(dest) && dest.getPistonMoveReaction() != PistonMoveReaction.BREAK && !dest.getType().isAir()) {
                return;
            }
        }
        var blocks = event.getBlocks().stream().map(b -> Map.entry(b.getType(), b)).toList();
        Bukkit.getScheduler().runTaskLater(RandomThings.INSTANCE, () -> {
            for (var block : blocks) {
                var dest = block.getValue().getRelative(dir);
                if (block.getKey() == Material.BUDDING_AMETHYST) {
                    dest.setType(Material.BUDDING_AMETHYST);
                }
            }
        }, 1);
    }

}
