package com.tontonsamael;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.BootEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.tontonsamael.publicportals.PortalRemoveLimit;
import com.tontonsamael.publicportals.PortalService;
import com.tontonsamael.publicportals.commands.PortalCommand;
import com.tontonsamael.publicportals.config.PortalConfig;
import com.tontonsamael.publicportals.events.PortalEnterEvent;
import com.tontonsamael.publicportals.events.PortalUseEvent;
import com.tontonsamael.publicportals.systems.PlayerJoinWorldListener;
import com.tontonsamael.publicportals.systems.PlayersTickSystem;
import com.tontonsamael.publicportals.systems.PortalBreakListener;
import com.tontonsamael.publicportals.systems.PortalPlaceListener;

import javax.annotation.Nonnull;

public class PublicPortals extends JavaPlugin {
    private static PublicPortals INSTANCE;

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Config<PortalConfig> configPortal = this.withConfig("configPortal", PortalConfig.CODEC);

    public PublicPortals(@Nonnull JavaPluginInit init) {
        super(init);
        INSTANCE = this;
    }

    @Override
    protected void setup() {
        this.getEventRegistry().registerGlobal(BootEvent.class, (_) -> PortalService.get().onServerBoot());
        this.getCommandRegistry().registerCommand(new PortalCommand());
        this.getEntityStoreRegistry().registerSystem(new PortalPlaceListener());
        this.getEntityStoreRegistry().registerSystem(new PortalBreakListener());
        this.getEntityStoreRegistry().registerSystem(new PlayerJoinWorldListener());
        this.getEntityStoreRegistry().registerSystem(new PlayersTickSystem());
        this.getCodecRegistry(Interaction.CODEC).register("Teleporter", PortalEnterEvent.class, PortalEnterEvent.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("PlacementCountCondition", PortalRemoveLimit.class, PortalRemoveLimit.CODEC);
        this.getCodecRegistry(OpenCustomUIInteraction.PAGE_CODEC).register("Teleporter", PortalUseEvent.class, PortalUseEvent.CODEC);

        LOGGER.atInfo().log("PublicPortals is loaded !");
    }

    public static PublicPortals get() {
        return INSTANCE;
    }

    public Config<PortalConfig> getConfigPortal() {
        return configPortal;
    }
}