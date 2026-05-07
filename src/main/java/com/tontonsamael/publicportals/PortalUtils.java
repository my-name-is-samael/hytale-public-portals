package com.tontonsamael.publicportals;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldProvider;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

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

        return PermissionsModule.get().getGroupsForUser(playerRef.getUuid()).contains("OP");
    }

    @Nullable
    public static Teleporter retrieveTeleporter(@Nullable World world, BlockPosition position) {
        HytaleLogger.Api logger = HytaleLogger.forEnclosingClass().atInfo();
        if (world == null) return null;
        ChunkStore chunkStore = world.getChunkStore();
        long chunkIndex = ChunkUtil.indexChunkFromBlock(position.x, position.z);
        BlockComponentChunk blockComponentChunk = chunkStore.getChunkComponent(chunkIndex, BlockComponentChunk.getComponentType());
        if (blockComponentChunk == null) {
            logger.log("chunk is null");
            return null;
        }
        int blockIndex = ChunkUtil.indexBlockInColumn(position.x, position.y, position.z);
        Ref<ChunkStore> blockRef = blockComponentChunk.getEntityReference(blockIndex);
        if (blockRef == null || !blockRef.isValid()) {
            logger.log("ref is null");
            return null;
        }

        return chunkStore.getStore().getComponent(blockRef, Teleporter.getComponentType());
    }

    public static void activatePortal(World world, BlockPosition pos) {
        ChunkStore chunkStore = world.getChunkStore();
        long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
        BlockComponentChunk blockComponentChunk = chunkStore.getChunkComponent(chunkIndex, BlockComponentChunk.getComponentType());
        if (blockComponentChunk == null) return;
        int blockIndex = ChunkUtil.indexBlockInColumn(pos.x, pos.y, pos.z);
        Ref<ChunkStore> blockRef = blockComponentChunk.getEntityReference(blockIndex);
        if (blockRef == null || !blockRef.isValid()) return;

        Teleporter portal = blockRef.getStore().getComponent(blockRef, Teleporter.getComponentType());
        if (portal == null) return;
        BlockModule.BlockStateInfo blockState = blockRef.getStore().getComponent(blockRef, BlockModule.BlockStateInfo.getComponentType());
        if (blockState == null) return;
        WorldChunk worldChunkComponent = chunkStore.getChunkComponent(chunkIndex, WorldChunk.getComponentType());
        if (worldChunkComponent == null) return;
        BlockType blockType = worldChunkComponent.getBlockType(new Vector3i(pos.x, pos.y, pos.z));
        if (blockType == null) return;
        if (!"Active".equals(blockType.getStateForBlock(blockType))) {
            worldChunkComponent.setBlockInteractionState(pos.x, pos.y, pos.z, blockType, "Active", false);
            blockState.markNeedsSaving(blockRef.getStore());
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

    public static class KeyValueData<T,U> {
        public T key;
        public U value;

        public KeyValueData(T key, U value) {
            this.key = key;
            this.value = value;
        }
    }
}
