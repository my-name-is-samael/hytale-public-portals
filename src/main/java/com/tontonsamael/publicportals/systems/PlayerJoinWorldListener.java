package com.tontonsamael.publicportals.systems;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tontonsamael.publicportals.PortalService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class PlayerJoinWorldListener extends HolderSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void onEntityAdd(@NonNullDecl Holder<EntityStore> holder, @NonNullDecl AddReason addReason, @NonNullDecl Store<EntityStore> store) {
        PortalService.get().onPlayerJoinWorld(holder.getComponent(Player.getComponentType()));
    }

    @Override
    public void onEntityRemoved(@NonNullDecl Holder<EntityStore> holder, @NonNullDecl RemoveReason removeReason, @NonNullDecl Store<EntityStore> store) {
    }
}