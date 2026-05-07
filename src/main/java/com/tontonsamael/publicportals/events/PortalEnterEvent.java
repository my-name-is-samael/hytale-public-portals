package com.tontonsamael.publicportals.events;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.tontonsamael.publicportals.PortalService;
import com.tontonsamael.publicportals.PortalUtils;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PortalEnterEvent extends SimpleBlockInteraction {
    @Nonnull
    public static final BuilderCodec<PortalEnterEvent> CODEC = BuilderCodec.builder(PortalEnterEvent.class, PortalEnterEvent::new, SimpleBlockInteraction.CODEC)
            .appendInherited(new KeyedCodec<>("Particle", Codec.STRING),
                    (i, s) -> i.particle = s,
                    (i) -> i.particle,
                    (i, parent) -> i.particle = parent.particle)
            .documentation("Particle effect for portal usage")
            .add()
            .build();
    @Nullable
    private String particle;

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    protected void interactWithBlock(@NonNullDecl World world, @NonNullDecl CommandBuffer<EntityStore> cmd, @NonNullDecl InteractionType interaction, @NonNullDecl InteractionContext ctxt, @NullableDecl ItemStack itemStack, @NonNullDecl Vector3i target, @NonNullDecl CooldownHandler cooldown) {
        if (interaction != InteractionType.CollisionEnter) return;

        // teleporter check
        BlockPosition position = new BlockPosition(target.x, target.y, target.z);
        Teleporter teleporter = PortalUtils.retrieveTeleporter(world, position);
        if (teleporter == null) return;

        // player check
        Player player = ctxt.getEntity().getStore().getComponent(ctxt.getEntity(), Player.getComponentType());
        if(player == null) {
            /*
            // MountedComponent nor MountedByComponent are detected well for now (for rideable animals like horses, rams, etc.)
            MountedComponent mount = ctxt.getEntity().getStore().getComponent(ctxt.getEntity(), MountedComponent.getComponentType());
            LOGGER.atInfo().log("mount = %b", mount != null);
            if (mount != null) {
                Ref<EntityStore> refPlayerPassenger = mount.getMountedToEntity();//getPassengers().stream().filter(p -> p.getStore().getComponent(p, Player.getComponentType()) != null).findFirst().orElse(null)
                if (refPlayerPassenger == null) return;
                player = refPlayerPassenger.getStore().getComponent(refPlayerPassenger, Player.getComponentType());
            }
            */
            NPCEntity mob = ctxt.getEntity().getStore().getComponent(ctxt.getEntity(), NPCEntity.getComponentType());
            if(mob != null) {
                PortalService.get().onMobEnter(ctxt.getEntity(), world, position);
            }
            return;
        }

        PortalService.get().onPlayerEnter(player, world, position);

    }

    @Override
    protected void simulateInteractWithBlock(@NonNullDecl InteractionType interaction, @NonNullDecl InteractionContext ctxt, @NullableDecl ItemStack item, @NonNullDecl World world, @NonNullDecl Vector3i target) {
    }
}
