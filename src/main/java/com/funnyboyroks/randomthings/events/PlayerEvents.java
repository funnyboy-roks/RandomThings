package com.funnyboyroks.randomthings.events;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.funnyboyroks.randomthings.RandomThings;
import com.funnyboyroks.randomthings.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class PlayerEvents implements Listener {

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) { // Twerk Bonemeal
            if (RandomThings.config.twerkBonemeal) doBonemeal(event.getPlayer());
            if (RandomThings.config.elevators) {
                Block currentBlock = event.getPlayer().getLocation().getWorld().getBlockAt(event.getPlayer().getLocation());
                Block under = currentBlock.getRelative(BlockFace.DOWN);
                if (currentBlock.getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE
                    && under.getType().name().endsWith("_WOOL")) {
                    int maxY = -(Math.max(event.getPlayer().getWorld().getMinHeight(), currentBlock.getY() - 21) - currentBlock.getY());
                    for (int y = 2; y < maxY; ++y) {
                        Block block = currentBlock.getRelative(BlockFace.DOWN, y);

                        if (
                            block.getType() == under.getType()
                            && block.getRelative(BlockFace.UP).getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE
                            && !block.getRelative(BlockFace.UP, 2).getType().isCollidable()
                        ) {
                            Location dest = block.getRelative(BlockFace.UP).getLocation().clone();
                            Location playerLoc = event.getPlayer().getLocation();
                            dest.setYaw(playerLoc.getYaw());
                            dest.setPitch(playerLoc.getPitch());
                            dest.add(.5, .5, .5);

                            event.getPlayer().teleport(dest, PlayerTeleportEvent.TeleportCause.PLUGIN);
                            event.getPlayer().playSound(
                                dest,
                                Sound.ENTITY_ENDER_DRAGON_SHOOT,
                                1,
                                2
                            );
                            Bukkit.getScheduler().runTaskLater(RandomThings.INSTANCE, () -> event.getPlayer().setVelocity(new Vector(0, -.3, 0)), 1);
                            return;
                        }
                    }
                }
            }
        }
    }

    public static void doBonemeal(Player player) {
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
        if (event.getAction().isRightClick() && event.hasBlock()) { // Right-Click Harvest Crops
            Block clicked = event.getClickedBlock();

            Material type = clicked.getType();

            if (Util.PLANTS.contains(type)) {

                Ageable data = (Ageable) clicked.getBlockData();
                if (data.getAge() != data.getMaximumAge()) return;

                event.setUseInteractedBlock(Event.Result.DENY);
                event.setUseItemInHand(Event.Result.DENY);

                clicked.breakNaturally(true); // Break it to have the particles and drop the items


                clicked.getWorld().getBlockAt(clicked.getLocation()).setType(type);
                event.getPlayer().swingMainHand(); // Swing the hand to make it look more natural
                event.setCancelled(true);
            }


        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!RandomThings.config.vacuumHoppers) return;
        Location l = event.getBlock().getLocation().add(0, -1, 0);
        Block hopperBlock = l.getWorld().getBlockAt(l);
        if (hopperBlock.getType() != Material.HOPPER || event.getBlockPlaced().getType() != Material.PURPLE_CARPET) { // Create vacuum hopper
            return;
        }

        Marker e = l.getWorld().spawn(l.add(.5, 1, .5), Marker.class);
        e.setGravity(false);
        RandomThings.getDataHandler().data.vacuumHoppers.add(e.getUniqueId());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!RandomThings.config.vacuumHoppers) return;
        if (event.getBlock().getType() == Material.HOPPER || event.getBlock().getType() == Material.PURPLE_CARPET) { // Remove vacuum hopper
            Location asLoc = event.getBlock().getLocation().add(.5, 0, .5);
            if (event.getBlock().getType() == Material.HOPPER) asLoc.add(0, 1, 0);

            Entity stand = RandomThings.getDataHandler().data.vacuumHoppers
                .stream()
                .map(Bukkit::getEntity)
                .filter(e -> e != null && e.getLocation().distanceSquared(asLoc) < .2)
                .findFirst()
                .orElse(null);

            if (stand == null) return;

            stand.remove();
            RandomThings.getDataHandler().data.vacuumHoppers.remove(stand.getUniqueId());


        }
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        if (!RandomThings.config.magicMirror) return;

        ItemStack left = event.getInventory().getFirstItem();
        ItemStack right = event.getInventory().getSecondItem();

        if (left != null
            && left.getType() == Material.ENDER_PEARL
            && right != null
            && right.getType() == Material.COMPASS
            && left.getAmount() == right.getAmount()
        ) {
            ItemStack newResult = Util.newMagicMirror();
            newResult.setAmount(left.getAmount());
            event.getInventory().setRepairCost(left.getAmount() * 16);
            event.setResult(newResult);
        }
    }

    @EventHandler
    public void onPlayerLaunchProjectile(PlayerLaunchProjectileEvent event) {
        if (event.isCancelled() || !RandomThings.config.magicMirror) return;

        if (Util.isMagicMirror(event.getItemStack())) {
            event.getPlayer().sendActionBar(Component.text("Teleporting...", NamedTextColor.LIGHT_PURPLE));
            Bukkit.getScheduler().runTaskLater(RandomThings.INSTANCE, event.getProjectile()::remove, 1);
            event.setShouldConsume(true);
            Bukkit.getScheduler().runTaskLater(RandomThings.INSTANCE, () -> {
                Location dest = event.getPlayer().getBedSpawnLocation();
                if (dest == null) dest = event.getPlayer().getWorld().getSpawnLocation();
                Util.teleport(event.getPlayer(), dest);
            }, 10);
        }
    }

    @EventHandler
    public void onPlayerJump(PlayerJumpEvent event) {
        if (event.isCancelled() || !RandomThings.config.elevators) return;
        Block currentBlock = event.getFrom().getBlock();
        Block under = currentBlock.getRelative(BlockFace.DOWN);
        if (currentBlock.getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE
            && under.getType().name().endsWith("_WOOL")) {
            int maxY = Math.min(event.getPlayer().getWorld().getMaxHeight(), currentBlock.getY() + 20) - currentBlock.getY();
            for (int y = 0; y < maxY; ++y) {
                Block block = currentBlock.getRelative(BlockFace.UP, y);
                if (
                    block.getType() == under.getType()
                    && block.getRelative(BlockFace.UP).getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE
                    && !block.getRelative(BlockFace.UP, 2).getType().isCollidable()
                ) {
                    Location dest = block.getRelative(BlockFace.UP).getLocation().clone();
                    Location playerLoc = event.getPlayer().getLocation();
                    dest.setYaw(playerLoc.getYaw());
                    dest.setPitch(playerLoc.getPitch());
                    dest.add(.5, 0, .5);

                    event.getPlayer().teleport(dest, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    event.getPlayer().playSound(
                        dest,
                        Sound.ENTITY_ENDER_DRAGON_SHOOT,
                        1,
                        2
                    );
                    Bukkit.getScheduler().runTaskLater(RandomThings.INSTANCE, () -> event.getPlayer().setVelocity(new Vector(0, .3, 0)), 1);
                    return;
                }
            }
        }
    }

}
