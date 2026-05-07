package com.tontonsamael.publicportals.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tontonsamael.publicportals.PortalService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class PlayersTickSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private float delay = 1f;

    @Override
    public void tick(float dt, int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                     @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {
        delay -= dt;
        if (delay <= 0f) {
            delay += 1f;
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) PortalService.get().tickPlayer(player);
        }
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

}