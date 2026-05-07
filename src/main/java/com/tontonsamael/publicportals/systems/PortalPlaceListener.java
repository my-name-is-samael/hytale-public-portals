package com.tontonsamael.publicportals.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tontonsamael.publicportals.PortalService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Locale;

public class PortalPlaceListener extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public PortalPlaceListener() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl PlaceBlockEvent event) {
        ItemStack handItem = event.getItemInHand();
        if (handItem == null || handItem.isEmpty()) return;
        String itemId = handItem.getItem().getId();
        if (itemId == null || !itemId.toLowerCase(Locale.ROOT).endsWith("teleporter")) return;

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null || player.getWorld() == null) {
            event.setCancelled(true);
            return;
        }

        if (!PortalService.get().onPlayerPlace(player, player.getWorld(), new BlockPosition(event.getTargetBlock().x, event.getTargetBlock().y, event.getTargetBlock().z))) {
            event.setCancelled(true);
        }
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
