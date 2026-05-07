package com.tontonsamael.publicportals;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.blocktrack.BlockCounter;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class PortalRemoveLimit extends SimpleInstantInteraction {
    public static final BuilderCodec<PortalRemoveLimit> CODEC =
            BuilderCodec.builder(PortalRemoveLimit.class, PortalRemoveLimit::new, SimpleInstantInteraction.CODEC)
                    .appendInherited(new KeyedCodec<>("Block", Codec.STRING),
                            (o, v) -> o.blockType = v,
                            (o) -> o.blockType,
                            (o, p) -> o.blockType = p.blockType).addValidator(Validators.nonNull())
                    .add()
                    .appendInherited(new KeyedCodec<>("Value", Codec.INTEGER),
                            (o, v) -> o.value = v,
                            (o) -> o.value,
                            (o, p) -> o.value = p.value)
                    .add()
                    .appendInherited(new KeyedCodec<>("LessThan", Codec.BOOLEAN),
                            (o, v) -> o.lessThan = v,
                            (o) -> o.lessThan,
                            (o, p) -> o.lessThan = p.lessThan)
                    .add()
                    .build();
    private String blockType;
    private int value = 0;
    private boolean lessThan = true;;

    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        if (this.blockType.equals("Teleporter")) {
            context.getState().state = InteractionState.Finished;
            return;
        }

        CommandBuffer<EntityStore> cmd = context.getCommandBuffer();
        if (cmd == null) return;
        BlockCounter counter = cmd.getExternalData().getWorld().getChunkStore().getStore()
                .getResource(BlockCounter.getResourceType());
        int blockCount = counter.getBlockPlacementCount(this.blockType);
        if (this.lessThan) {
            context.getState().state = blockCount < this.value ? InteractionState.Finished : InteractionState.Failed;
        } else {
            context.getState().state = blockCount > this.value ? InteractionState.Finished : InteractionState.Failed;
        }
    }
}
