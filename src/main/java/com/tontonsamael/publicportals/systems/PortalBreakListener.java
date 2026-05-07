package com.tontonsamael.publicportals.systems;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tontonsamael.publicportals.PortalService;
import com.tontonsamael.publicportals.PortalUtils;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Locale;

public class PortalBreakListener extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    public PortalBreakListener() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl BreakBlockEvent event) {
        String blockId = event.getBlockType().getId();
        if (blockId == null || !blockId.toLowerCase(Locale.ROOT).contains("teleporter")) return;

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null || player.getWorld() == null) {
            event.setCancelled(true);
            return;
        }

        BlockPosition position = new BlockPosition(event.getTargetBlock().x,
                event.getTargetBlock().y, event.getTargetBlock().z);
        Teleporter teleporter = PortalUtils.retrieveTeleporter(player.getWorld(), position);
        if(teleporter == null) return;

        if (!PortalService.get().onPlayerBreak(player, player.getWorld(), position)) {
            event.setCancelled(true);
        }
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
