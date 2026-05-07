package com.tontonsamael.publicportals.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tontonsamael.publicportals.PortalService;
import com.tontonsamael.publicportals.PortalUtils;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;

public class PortalSelectDestination extends InteractiveCustomUIPage<PortalSelectDestination.UIData> {
    private static final String ACTION = "Action";
    private static final String VALUE_STRING = "ValueString";

    private enum Actions {
        CLOSE, TELEPORT
    }

    public static class UIData {
        public static final BuilderCodec<PortalSelectDestination.UIData> CODEC = BuilderCodec.builder(PortalSelectDestination.UIData.class, PortalSelectDestination.UIData::new)
                .append(new KeyedCodec<>(ACTION, Codec.STRING),
                        (data, value) -> data.action = value,
                        data -> data.action).add()
                .append(new KeyedCodec<>(VALUE_STRING, Codec.STRING),
                        (data, value) -> data.valueString = value,
                        data -> data.valueString).add()
                .build();

        private String action;
        private String valueString;
    }

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final String currentPortalId;

    public PortalSelectDestination(PlayerRef playerRef, String currentPortal) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, UIData.CODEC);
        currentPortalId = currentPortal;
    }

    private void createBindings(UIEventBuilder events, List<PortalService.PortalView> portals) {
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#CloseButton", new EventData().append(ACTION, Actions.CLOSE.name()));

        portals.forEach(portal ->
                events.addEventBinding(CustomUIEventBindingType.Activating,
                        String.format("#%s #Warp", portal.id),
                        new EventData()
                                .append(ACTION, Actions.TELEPORT.name())
                                .append(VALUE_STRING, portal.id),
                        false)
        );
    }

    private void updateData(Player player, UICommandBuilder cmd, List<PortalService.PortalView> portals) {
        portals.forEach(portal -> {
            cmd.appendInline("#" + (portal.ownerName == null ? "Public" : "Personal"),
                    String.format("""
                                Group #%s {
                                    LayoutMode: Left;
                                    Anchor: (Left: 0, Right: 0, Bottom: 5);
                                }
                            """, portal.id));
            cmd.append("#" + portal.id, "PublicPortals/WarpEntry.ui");

            String portalLabel = PortalUtils.capitalize(portal.name.isEmpty() ?
                    portal.id : portal.name);
            cmd.set(String.format("#%s #Label.Text", portal.id), portalLabel);
            if(portal.ownerName == null)
                cmd.set(String.format("#%s #Owner.Visible", portal.id), false);
            else if (player.getDisplayName().equals(portal.ownerName)) {
                cmd.set(String.format("#%s #Owner.Visible", portal.id), false);
                cmd.set(String.format("#%s #Label.Style.TextColor", portal.id), "#e6e64b");
            } else
                cmd.set(String.format("#%s #Owner.Text", portal.id), String.format("(%s)", portal.ownerName));
        });

        if(portals.stream().noneMatch(p -> p.ownerName == null)) {
            cmd.set("#Public #NoWarps.Visible", false);
        }
        if(portals.stream().noneMatch(p -> p.ownerName != null)) {
            cmd.set("#Personal #NoWarps.Visible", false);
        }
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder cmd, @NonNullDecl UIEventBuilder events, @NonNullDecl Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        List<PortalService.PortalView> views = PortalService.get().getPortalsList(player, currentPortalId, false);
        if (player == null || views.isEmpty()) {
            PortalUtils.closePage(ref, store);
            return;
        }

        cmd.append("PublicPortals/WarpDestination.ui");
        updateData(player, cmd, views);
        createBindings(events, views);
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl UIData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            PortalUtils.closePage(ref, store);
            return;
        }

        Actions action;
        try {
            action = Actions.valueOf(data.action);
        } catch (IllegalArgumentException e) {
            LOGGER.atSevere().log("Invalid action : %s", data.action);
            PortalUtils.closePage(ref, store);
            return;
        }

        switch (action) {
            case Actions.CLOSE -> PortalUtils.closePage(ref, store);
            case Actions.TELEPORT -> {
                PortalService.get().teleportPlayer(this.playerRef, data.valueString);
                PortalUtils.closePage(ref, store);
            }
        }
    }
}
