package com.tontonsamael.publicportals;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;

import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldProvider;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;


import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

public final class PortalUtils {
    public static int SORT_A_BEFORE_B = -1;
    public static int SORT_B_BEFORE_A = 1;

    public static boolean checkOP(Player player) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null) return false;
        PlayerRef playerRef = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return false;

        return PermissionsModule.get().getGroupsForUser(playerRef.getUuid()).contains("hytale:Admin");
    }

    @Nullable
    public static Teleporter retrieveTeleporter(@Nullable World world, BlockPosition position) {
        if (world == null) return null;
        Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(world, position.x, position.y, position.z);
        if (blockRef == null || !blockRef.isValid()) return null;

        return world.getChunkStore().getStore().getComponent(blockRef, Teleporter.getComponentType());
    }

    /**
     * True only when the chunk is loaded AND the block there is certainly not a teleporter any more.
     * Right after a boot the chunk or its block entities may not be ready: that is not a broken portal.
     */
    public static boolean isTeleporterBlockGone(World world, BlockPosition pos) {
        ChunkStore chunkStore = world.getChunkStore();
        Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReferenceAtBlock(pos.x, pos.y, pos.z);
        if (sectionRef == null || !sectionRef.isValid()) return false;
        BlockSection section = chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType());
        if (section == null) return false;
        BlockType blockType = BlockType.getAssetMap().getAsset(section.get(pos.x, pos.y, pos.z));
        if (blockType == null) return false;
        return !blockType.getId().toLowerCase().contains("teleporter");
    }

    private static final java.util.Map<String, Long> DEBUG_LAST = new java.util.HashMap<>();

    /** Temporary diagnostic, at most once every 15 s per message and position. */
    static void debug(BlockPosition pos, String msg) {
        String key = pos.x + "," + pos.y + "," + pos.z + msg;
        long now = System.currentTimeMillis();
        Long last = DEBUG_LAST.get(key);
        if (last != null && now - last < 15000) return;
        DEBUG_LAST.put(key, now);
        HytaleLogger.forEnclosingClass().atInfo().log("[activate %d %d %d] %s", pos.x, pos.y, pos.z, msg);
    }

    public static void activatePortal(World world, BlockPosition pos) {
        ChunkStore chunkStore = world.getChunkStore();
        Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReferenceAtBlock(pos.x, pos.y, pos.z);
        if (sectionRef == null || !sectionRef.isValid()) {
            debug(pos, "no chunk section here (chunk not loaded?)");
            return;
        }
        Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(chunkStore.getStore(), sectionRef, pos.x, pos.y, pos.z);
        if (blockRef == null || !blockRef.isValid()) {
            debug(pos, "no block entity at this position");
            return;
        }
        if (chunkStore.getStore().getComponent(blockRef, Teleporter.getComponentType()) == null) {
            debug(pos, "block entity has no Teleporter component");
            return;
        }

        BlockSection section = chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType());
        if (section == null) {
            debug(pos, "no BlockSection");
            return;
        }
        BlockType blockType = BlockType.getAssetMap().getAsset(section.get(pos.x, pos.y, pos.z));
        if (blockType == null) {
            debug(pos, "no block type");
            return;
        }
        String state = blockType.getStateForBlock(blockType);
        if (!"Active".equals(state)) {
            BlockType active = blockType.getBlockForState("Active");
            debug(pos, "block " + blockType.getId() + " state=" + state + ", Active variant=" + (active != null ? active.getId() : "null"));
            if (active == null) return;
            // the block passed is the current one: the method resolves the target variant itself from the state name
            BlockOperations.setBlockInteractionState(chunkStore, sectionRef, pos.x, pos.y, pos.z, blockType, "Active", false);
        }
    }

    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    @Nullable
    public static Player refToPlayer(@Nullable Ref<EntityStore> ref) {
        if (ref == null) return null;
        return ref.getStore().getComponent(ref, Player.getComponentType());
    }

    @Nullable
    public static Player playerRefToPlayer(@Nullable PlayerRef pref) {
        if (pref == null) return null;
        Ref<EntityStore> ref = pref.getReference();
        if (ref == null) return null;
        return refToPlayer(ref);
    }

    static List<String> syllables = List.of(
            "ka", "ra", "lo", "mi", "sa", "ne", "ti", "va", "do", "xi", "ze", "ul", "or", "an", "il", "en",
            "tor", "zen", "kai", "dra", "vel", "nor", "lum", "tek", "syl", "vor", "mar", "kel", "rin", "dar",
            "qua", "zyn", "ael", "yss", "kor", "vyn", "thal", "jor", "nyx", "pyr", "zed", "ryn", "vox", "tyr"
    );

    public static String generateNewPortalId(Set<String> existingIds) {
        final Random rand = new Random();
        Supplier<String> generate = () -> {
            StringBuilder res = new StringBuilder();
            int syllablesCount = rand.nextInt(3, 4);
            for (int i = 0; i < syllablesCount; i++) {
                res.append(syllables.get(rand.nextInt(syllables.size())));
            }
            return res.toString();
        };
        String res;
        do {
            res = generate.get();
        } while (existingIds.contains(res));
        return res;
    }

    public static void closePage(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) return;
        player.getPageManager().setPage(playerRef, store, Page.None);
    }

    @Nullable
    public static String getPlayerUUID(Player player) {
        if (player.getReference() == null) return null;
        UUIDComponent uuid = player.getReference().getStore().getComponent(player.getReference(), UUIDComponent.getComponentType());
        if (uuid != null)
            return uuid.getUuid().toString();
        return null;
    }

    public static double distanceSquared(double x, double y, double z, double x1, double y1, double z1) {
        double dx = x - x1, dy = y - y1, dz = z - z1;
        return dx * dx + dy * dy + dz * dz;
    }

    public static double distance3d(double x, double y, double z, double x1, double y1, double z1) {
        return (float) Math.sqrt(Math.pow(x - x1, 2) + Math.pow(y - y1, 2) + Math.pow(z - z1, 2));
    }

    public static Set<String> listComponents(Ref<? extends WorldProvider> ref) {
        Set<String> components = new HashSet<>();
        List.of(ref.getStore().collectArchetypeChunkData()).forEach((acd) ->
                components.addAll(List.of(acd.getComponentTypes()))
        );
        HytaleLogger.forEnclosingClass().atInfo().log("Block components :");
        components.forEach(ct -> HytaleLogger.forEnclosingClass().atInfo().log("  - %s", ct));
        return components;
    }

    public static class KeyValueData<T, U> {
        public T key;
        public U value;

        public KeyValueData(T key, U value) {
            this.key = key;
            this.value = value;
        }
    }

    public static boolean isPositionSimilar(BlockPosition bpos, Vector3d vpos) {
        return (bpos.x == vpos.x && bpos.y == vpos.y && bpos.z == vpos.z) || distance3d(bpos.x, bpos.y, bpos.z, vpos.x, vpos.y, vpos.z) < 1f;
    }

    public static boolean isPositionSimilar(Vector3d vpos, BlockPosition bpos) {
        return isPositionSimilar(bpos, vpos);
    }

    public static boolean approximate(double a, double b) {
        return Math.abs(a - b) < 0.0001;
    }

    public static Vector3d getFinalTeleporterDestination(BlockPosition basePosition, @Nullable Float yaw) {
        Vector3d off = new Vector3d();
        if (yaw != null) {
            if (approximate(Math.PI, yaw)) // FACING SOUTH
                off.z = 1;
            else if (approximate(Math.PI * 3 / 2, yaw)) //  FACING EAST
                off.x = 1;
            else if (approximate(Math.PI * 2, yaw)) // FACING NORTH
                off.z = -1;
            else if (approximate(Math.PI * 5 / 2, yaw)) // FACING WEST
                off.x = -1;
        }
        return new Vector3d(basePosition.x + off.x + .5f, basePosition.y, basePosition.z + off.z + .5f);
    }

    /** The account name (what the game itself uses), falling back to "INVALID" for a player not fully in the world. */
    public static String getPlayerName(Player player) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null) return "INVALID";
        PlayerRef pref = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
        if (pref == null) return "INVALID";
        String name = pref.getUsername();
        return name == null || name.isEmpty() ? "INVALID" : name;
    }

    public static void sendMessage(Player player, Message msg) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null) return;
        PlayerRef pref = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
        if (pref == null) return;
        pref.sendMessage(msg);
    }
}
