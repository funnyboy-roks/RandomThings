package com.funnyboyroks.randomthings;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.IOException;

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
            config = new PluginConfig(INSTANCE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        Bukkit.getPluginManager().registerEvents(new Listeners(), INSTANCE);
        this.dataHandler = new DataHandler();


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

    }

    @Override
    public void onDisable() {
        try {
            this.dataHandler.saveData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static DataHandler getDataHandler() {
        return INSTANCE.dataHandler;
    }
}
