package com.tontonsamael.publicportals.events;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tontonsamael.publicportals.PortalService;
import com.tontonsamael.publicportals.PortalUtils;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nullable;

public class PortalUseEvent implements OpenCustomUIInteraction.CustomPageSupplier {
    public static final BuilderCodec<PortalUseEvent> CODEC = BuilderCodec.builder(PortalUseEvent.class, PortalUseEvent::new)
            .appendInherited(new KeyedCodec<>("ActiveState", Codec.STRING),
                    (i, o) -> i.activeState = o,
                    (i) -> i.activeState,
                    (i, parent) -> i.activeState = parent.activeState)
            .add()
            .build();
    @Nullable
    private String activeState;

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @NullableDecl
    @Override
    public CustomUIPage tryCreate(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl ComponentAccessor<EntityStore> componentAccessor, @NonNullDecl PlayerRef playerRef, @NonNullDecl InteractionContext context) {
        Player player = context.getEntity().getStore().getComponent(context.getEntity(), Player.getComponentType());
        if (player == null) return null;

        BlockPosition targetBlock = context.getTargetBlock();
        if (targetBlock == null) return null;

        Teleporter teleporter = PortalUtils.retrieveTeleporter((ref.getStore().getExternalData()).getWorld(), targetBlock);
        if (teleporter == null) return null;

        return PortalService.get().onPlayerUse(player, player.getWorld(), targetBlock);
    }
}
