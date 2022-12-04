package com.funnyboyroks.randomthings;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class Util {

    public static final BlockFace[] ADJACENT_FACES = {
        BlockFace.DOWN, BlockFace.UP,
        BlockFace.EAST, BlockFace.WEST,
        BlockFace.NORTH, BlockFace.SOUTH,
        };

    public static final Set<Material> PLANTS = Set.of(
        Material.POTATOES,
        Material.CARROTS,
        Material.WHEAT,
        Material.BEETROOTS
    );

    public static final Random        RNG                  = new Random();
    public static final Set<Material> VALID_SAPLING_BLOCKS = Set.of(
        Material.GRASS_BLOCK,
        Material.DIRT,
        Material.COARSE_DIRT,
        Material.ROOTED_DIRT,
        Material.FARMLAND
    );

    public static void applyDamage(ItemStack item) {
        applyDamage(item, 1);
    }

    public static void applyDamage(ItemStack item, int amount) {
        if (!(item.getItemMeta() instanceof Damageable dmg)) return;
        for (int i = 0; i < amount; ++i) {
            double chance = 1 / (double) (item.getEnchantmentLevel(Enchantment.DURABILITY) + 1);
            if (RNG.nextDouble() <= chance) dmg.setDamage(dmg.getDamage() + 1);
        }
        item.setItemMeta(dmg);
        if (dmg.getDamage() >= item.getType().getMaxDurability()) item.setAmount(0);
    }

    public static NamespacedKey getKey(String key) {
        return new NamespacedKey(RandomThings.INSTANCE, key);
    }

    public static ItemStack newMagicMirror() {
        ItemStack stack = new ItemStack(Material.ENDER_PEARL);
        var meta = stack.getItemMeta();

        meta.displayName(MiniMessage.miniMessage().deserialize("<bold><rainbow>Magic Mirror</rainbow></bold>"));
        meta.lore(List.of(
            Component.text("Throw this to return to your respawn location.", NamedTextColor.GRAY)
        ));
        meta.getPersistentDataContainer().set(getKey("magic_mirror"), PersistentDataType.BYTE, (byte) 1);

        stack.setItemMeta(meta);

        stack.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 1);
        stack.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        return stack;
    }

    public static boolean isMagicMirror(@NotNull ItemStack stack) {
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        Byte magicMirror = pdc.get(getKey("magic_mirror"), PersistentDataType.BYTE);
        return magicMirror != null && magicMirror == 1;
    }

    public static void teleport(@NotNull Player player, @NotNull Location dest) {
        player.teleport(dest);
        player.setFallDistance(0);
        player.setVelocity(new Vector(0, .3, 0));
    }

    public static void placeBlock(Item entity) {
        if (!entity.getItemStack().getType().isBlock()) return;

        entity.getWorld().getBlockAt(entity.getLocation()).setType(entity.getItemStack().getType());
    }

    public static boolean isSapling(Material type) {
        return type.name().contains("SAPLING") || type == Material.MANGROVE_PROPAGULE;
    }

    public static int randInt(int min, int max) {
        return RNG.nextInt(max - min) + min;
    }

    public static boolean isOnSaplingBlock(Entity entity) {
        Location blockUnder = entity.getLocation().clone().add(0, -1, 0);
        return VALID_SAPLING_BLOCKS.contains(entity.getWorld().getBlockAt(blockUnder).getType());
    }

    public static CompletableFuture<Boolean> isLatestVersion() {

        int serverVersion = Integer.parseInt(
            RandomThings.INSTANCE
                .getDescription()
                .getVersion()
                .replaceAll("\\.|-SNAPSHOT|v", "")
        );

        return CompletableFuture.supplyAsync(() -> {

            try {
                URL url = new URL("https://api.modrinth.com/v2/project/K9JIhdio");
                InputStreamReader reader = new InputStreamReader(url.openStream());
                JsonArray versions = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("versions");
                String version = versions.get(versions.size() - 1).getAsString();

                url = new URL("https://api.modrinth.com/v2/version/" + version);
                reader = new InputStreamReader(url.openStream());
                int latestVersion = Integer.parseInt(
                    JsonParser.parseReader(reader)
                        .getAsJsonObject()
                        .get("version_number")
                        .getAsString()
                        .replaceAll("\\.|-SNAPSHOT|v", "")
                );
                RandomThings.INSTANCE.getLogger().info("Latest Version: " + latestVersion);

                return latestVersion <= serverVersion;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });


    }
}
