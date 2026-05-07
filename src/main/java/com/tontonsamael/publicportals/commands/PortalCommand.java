package com.tontonsamael.publicportals.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tontonsamael.publicportals.PortalService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class PortalCommand extends AbstractPlayerCommand {

    public PortalCommand() {
        super("warp", "server.commands.warp.list.desc");
        this.requirePermission(HytalePermissions.fromCommand("warp.list"));
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        PortalService.get().triggerCommand(ref, store);
    }
}
