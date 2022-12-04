package com.funnyboyroks.randomthings;

import com.funnyboyroks.randomthings.events.DispenserEvents;
import com.funnyboyroks.randomthings.events.GeneralEvents;
import com.funnyboyroks.randomthings.events.PlayerEvents;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public final class RandomThings extends JavaPlugin {

    public static RandomThings INSTANCE;
    public static PluginConfig config;

    public DataHandler dataHandler;

    public RandomThings() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        try {
            Util.isLatestVersion().thenAccept((latest) -> {
                if (!latest) {
                    this.getLogger().warning("RandomThings has an update!");
                    this.getLogger().warning("Get it from https://modrinth.com/plugin/randomthings");
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        try {
            config = new PluginConfig(INSTANCE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.dataHandler = new DataHandler();
        this.registerEvents();


        Bukkit.getScheduler().scheduleSyncRepeatingTask(
            this,
            () -> this.dataHandler.data.vacuumHoppers.forEach(h -> {
                Entity entity = Bukkit.getEntity(h);
                if (entity == null) return;

                entity.getWorld().spawnParticle(
                    Particle.PORTAL,
                    entity.getLocation(),
                    50,
                    .25,
                    .25,
                    .25,
                    0
                );

                entity.getNearbyEntities(5, 5, 5)
                    .stream()
                    .filter(e -> e instanceof Item)
                    .forEach(i -> {
                        i.setVelocity(new Vector(0, 0, 0));
                        i.teleport(entity);
                    });
            }),
            0,
            20L
        );

        Bukkit.getScheduler().scheduleSyncRepeatingTask(
            this,
            () -> GeneralEvents.TO_BE_PLANTED.forEach((uuid, la) -> {
                Item entity = (Item) Bukkit.getEntity(uuid);
                if (la.longValue() <= 0) {
                    if (entity != null) {
                        if (Util.isOnSaplingBlock(entity)) {
                            Util.placeBlock(entity);
                            ItemStack stack = entity.getItemStack();
                            stack.setAmount(stack.getAmount() - 1);
                        }
                    }
                    GeneralEvents.TO_BE_PLANTED.remove(uuid);
                } else {
                    if (entity == null || !Util.isOnSaplingBlock(entity)) {
                        GeneralEvents.TO_BE_PLANTED.remove(uuid);
                    }
                    la.add(-10);
                }
            }),
            0,
            10
        );

    }

    @Override
    public void onDisable() {
        try {
            this.dataHandler.saveData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void registerEvents() {

        Listener[] eventHandlers = {
            new GeneralEvents(),
            new DispenserEvents(),
            new PlayerEvents()
        };

        for (Listener handler : eventHandlers) {
            Bukkit.getPluginManager().registerEvents(handler, INSTANCE);
        }
    }

    public static DataHandler getDataHandler() {
        return INSTANCE.dataHandler;
    }
}
