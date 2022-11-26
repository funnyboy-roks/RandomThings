package com.funnyboyroks.randomthings;

import com.tchristofferson.configupdater.ConfigUpdater;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;

public class PluginConfig {

    private final File configFile;

    public boolean vacuumHoppers;
    public boolean renewableBlackstone;
    public boolean renewableDeepslate;
    public boolean renewableSponges;
    public boolean silverFishDropGravel;
    public boolean rightClickHarvestCrops;
    public boolean twerkBonemeal;
    public boolean dispenserBreakBlocks;
    public boolean dispenserTillBlocks;
    public boolean dispenserCauldrons;
    public boolean movableAmethyst;

    public PluginConfig(RandomThings plugin) throws IOException {
        plugin.saveDefaultConfig();
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.update();

        FileConfiguration config = plugin.getConfig();

        this.vacuumHoppers = config.getBoolean("vacuum-hoppers");
        this.renewableBlackstone = config.getBoolean("renewable-blackstone");
        this.renewableDeepslate = config.getBoolean("renewable-deepslate");
        this.renewableSponges = config.getBoolean("renewable-sponges");
        this.silverFishDropGravel = config.getBoolean("silver-fish-drop-gravel");
        this.rightClickHarvestCrops = config.getBoolean("right-click-harvest-crops");
        this.twerkBonemeal = config.getBoolean("twerk-bonemeal");
        this.dispenserBreakBlocks = config.getBoolean("dispenser-break-blocks");
        this.dispenserTillBlocks = config.getBoolean("dispenser-till-blocks");
        this.dispenserCauldrons = config.getBoolean("dispenser-cauldrons");
        this.movableAmethyst = config.getBoolean("movable-amethyst");
    }

    public void update() throws IOException {
        ConfigUpdater.update(RandomThings.INSTANCE, "config.yml", this.configFile);
    }

}
