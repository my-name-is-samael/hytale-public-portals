package com.tontonsamael.publicportals.events;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.builtin.mounts.MountedByComponent;
import com.hypixel.hytale.builtin.mounts.NPCMountComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.logger.HytaleLogger;
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
import org.joml.Vector3i;

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

        Ref<EntityStore> entity = ctxt.getEntity();
        Store<EntityStore> store = entity.getStore();
        Player player = store.getComponent(entity, Player.getComponentType());
        if (player != null) {
            PortalService.get().onPlayerEnter(player, world, position);
            return;
        }

        // Riders. What the game exposes for a rideable NPC (found by testing, 0.7.7):
        // - the entity colliding with the portal is the mount (an NPC), never the rider;
        // - the mount has no MountedByComponent and the rider no MountedComponent (those are for minecarts and seats);
        // - the link is NPCMountComponent.getOwnerPlayerRef() on the mount (and Player.getMountEntityId() on the rider);
        // - teleporting a ridden NPC or its rider does nothing: the rider's client drives the mount and overrides it.
        //   So PortalService dismounts the rider with MountPlugin.checkDismountNpc, teleports both, then queues the rider
        //   on the NPC again through StateSupport.addInteraction (what a "use" on the NPC does).
        // MountedByComponent is still checked for other mount kinds.
        Player rider = null;
        MountedByComponent mountedBy = store.getComponent(entity, MountedByComponent.getComponentType());
        if (mountedBy != null) {
            for (Ref<EntityStore> passenger : mountedBy.getPassengers()) {
                rider = passenger.isValid() ? store.getComponent(passenger, Player.getComponentType()) : null;
                if (rider != null) break;
            }
        }
        NPCMountComponent npcMount = store.getComponent(entity, NPCMountComponent.getComponentType());
        if (rider == null && npcMount != null && npcMount.getOwnerPlayerRef() != null) {
            Ref<EntityStore> ownerRef = npcMount.getOwnerPlayerRef().getReference();
            if (ownerRef != null && ownerRef.isValid()) {
                rider = store.getComponent(ownerRef, Player.getComponentType());
            }
        }
        if (rider != null) {
            PortalService.get().onMountEnter(entity, rider, world, position);
            return;
        }

        if (store.getComponent(entity, NPCEntity.getComponentType()) != null) {
            PortalService.get().onMobEnter(entity, world, position);
        }
    }

    @Override
    protected void simulateInteractWithBlock(@NonNullDecl InteractionType interaction, @NonNullDecl InteractionContext ctxt, @NullableDecl ItemStack item, @NonNullDecl World world, @NonNullDecl Vector3i target) {
    }
}
