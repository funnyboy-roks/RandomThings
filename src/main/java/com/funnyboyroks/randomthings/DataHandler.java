package com.funnyboyroks.randomthings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.file.Files;

public class DataHandler {

    public Gson gson = new GsonBuilder().create();
    public File dataFile = null;
    public PluginData data = null;

    public DataHandler() {
        this.dataFile = new File(RandomThings.INSTANCE.getDataFolder(), "data.json");
        try {
            this.loadData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadData() throws IOException {
        if(!this.dataFile.exists()) {
            RandomThings.INSTANCE.getLogger().info("No data file found.  Using blank data...");
            this.data = new PluginData();
//            this.saveData();
        } else {
            RandomThings.INSTANCE.getLogger().info("Loading from data file...");
            String content = Files.readString(this.dataFile.toPath());
            this.data = gson.fromJson(content, PluginData.class);
        }
    }

    public void saveData() throws IOException {
        RandomThings.INSTANCE.getLogger().info("Saving data file...");
        String json = gson.toJson(data);
        if(!this.dataFile.getParentFile().exists()) this.dataFile.getParentFile().mkdir();
        if(!this.dataFile.exists()) this.dataFile.createNewFile();
        FileWriter fw = new FileWriter(this.dataFile);
        fw.write(json);
        fw.close();
        RandomThings.INSTANCE.getLogger().info("Done saving data.");
    }

}
