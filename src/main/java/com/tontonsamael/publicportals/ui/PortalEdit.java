package com.tontonsamael.publicportals.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tontonsamael.publicportals.PortalService;
import com.tontonsamael.publicportals.PortalUtils;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.List;

public class PortalEdit extends InteractiveCustomUIPage<PortalEdit.UIData> {
    private static final String ACTION = "Action";
    private static final String VALUE_STRING = "@ValueString";

    private enum Actions {
        CLOSE, UPDATE_DESTINATION, UPDATE_NAME, UPDATE_PUBLIC, UPDATE_PERSONAL, UPDATE_ALLOW_TELEPORT, SAVE
    }

    public static class UIData {
        public static final BuilderCodec<PortalEdit.UIData> CODEC = BuilderCodec.builder(PortalEdit.UIData.class, PortalEdit.UIData::new)
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

    public PortalEdit(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, UIData.CODEC);
    }

    private void createBindings(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#CloseButton", new EventData().append(ACTION, Actions.CLOSE.name()));

        events.addEventBinding(CustomUIEventBindingType.Activating, "#Visibility #Public",
                new EventData().append(ACTION, Actions.UPDATE_PUBLIC.name()), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#Visibility #Personal",
                new EventData().append(ACTION, Actions.UPDATE_PERSONAL.name()), false);

        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#Destination #Dropdown",
                new EventData()
                        .append(ACTION, Actions.UPDATE_DESTINATION.name())
                        .append(VALUE_STRING, "#Destination #Dropdown.Value"),
                false
        );

        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#Name #Input",
                new EventData()
                        .append(ACTION, Actions.UPDATE_NAME.name())
                        .append(VALUE_STRING, "#Name #Input.Value"),
                false
        );

        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#AllowTeleport #Checkbox",
                new EventData().append(ACTION, Actions.UPDATE_ALLOW_TELEPORT.name()), false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton",
                new EventData().append(ACTION, Actions.SAVE.name()));
    }

    private void enableButton(UICommandBuilder cmd, String selector) {
        cmd.set(selector + ".Style.Default.Background", "#4cb736");
        cmd.set(selector + ".Style.Hovered.Background", "#2fb023");
        cmd.set(selector + ".Style.Pressed.Background", "#3acb1c");
    }

    private void updateData(Player player, UICommandBuilder cmd, PortalService.PortalView view) {
        boolean isPublic = view.ownerName == null;
        if (player.getGameMode() == GameMode.Creative) {
            cmd.set("#Visibility #Personal.Disabled", true);
            enableButton(cmd, "#Visibility #Public");
        } else if (!PortalUtils.checkOP(player)) {
            cmd.set("#Visibility #Public.Disabled", true);
            enableButton(cmd, "#Visibility #Personal");
        } else if (view.wasPublic && isPublic && PortalService.get().isPersonalCapReached(player))
            cmd.set("#Visibility #Personal.Disabled", true);
        else enableButton(cmd, isPublic ? "#Visibility #Public" : "#Visibility #Personal");

        if (player.getGameMode() == GameMode.Creative) {
            cmd.set("#PersonalAmount.Visible", false);
        } else {
            String uuid = PortalUtils.getPlayerUUID(player);
            if (uuid != null) {
                int amount = PortalService.get().getPlayerPortalsCount(uuid);
                if (isPublic != view.wasPublic) {
                    amount += isPublic ? -1 : 1;
                    cmd.set("#PersonalAmount #Label.Style.TextColor", "#e6e64b");
                }
                cmd.set("#PersonalAmount #Label.Text", String.format("%d/%d",
                        amount, PortalService.get().getPersonalLimit()));
            }
        }

        List<PortalUtils.KeyValueData<String, DropdownEntryInfo>> destinations = PortalService.get().getDestinationsList(player);
        cmd.set("#Destination #Dropdown.Entries", destinations.stream().map(kv -> kv.value).toList());
        String finalDestination = destinations.stream().anyMatch(kv -> kv.key.equals(view.destination)) ?
                view.destination : "";
        cmd.set("#Destination #Dropdown.Value", finalDestination);

        cmd.set("#Name #Input.PlaceholderText", Message.translation(PortalUtils.capitalize(view.id)));
        cmd.set("#Name #Input.Value", view.name);

        if (isPublic)
            cmd.set("#AllowTeleport.Visible", false);
        else
            cmd.set("#AllowTeleport #Checkbox.Value", view.allowTeleport);
    }

    private void updateNameError(UICommandBuilder cmd, @Nullable String nameError) {
        cmd.set("#Name #Error.Visible", nameError != null);
        if (nameError != null)
            cmd.set("#Name #Error.Text", Message.translation(nameError));
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder cmd, @NonNullDecl UIEventBuilder events, @NonNullDecl Store<EntityStore> store) {
        PortalService.PortalView view = PortalService.get().getView(this.playerRef);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (view == null || player == null) {
            PortalUtils.closePage(ref, store);
            return;
        }

        cmd.append("PublicPortals/Edit.ui");
        createBindings(events);
        updateData(player, cmd, view);
        updateNameError(cmd, PortalService.get().getPortalNameError(player, view.name));
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl UIData data) {
        PortalService.PortalView view = PortalService.get().getView(this.playerRef);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (view == null || player == null) {
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

        boolean updated = false;
        switch (action) {
            case Actions.CLOSE -> PortalUtils.closePage(ref, store);
            case Actions.UPDATE_PUBLIC -> {
                view.ownerName = null;
                updated = true;
            }
            case Actions.UPDATE_PERSONAL -> {
                view.ownerName = player.getDisplayName();
                updated = true;
            }
            case Actions.UPDATE_DESTINATION -> view.destination = data.valueString.isEmpty() ?
                    null : data.valueString;
            case Actions.UPDATE_NAME -> view.name = data.valueString;
            case Actions.UPDATE_ALLOW_TELEPORT -> view.allowTeleport = !view.allowTeleport;
            case Actions.SAVE -> {
                String nameError = PortalService.get().getPortalNameError(player, view.name);
                if (nameError != null) {
                    UICommandBuilder cmd = new UICommandBuilder();
                    updateNameError(cmd, nameError);
                    this.sendUpdate(cmd);
                } else {
                    PortalService.get().savePortal(this.playerRef);
                    PortalUtils.closePage(ref, store);
                    return;
                }
            }
        }

        if (updated) {
            player.getPageManager().openCustomPage(ref, store, new PortalEdit(playerRef));
        }
    }
}
