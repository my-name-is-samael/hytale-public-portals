package com.tontonsamael.publicportals;

import com.hypixel.hytale.builtin.adventure.memories.MemoriesGameplayConfig;
import com.hypixel.hytale.builtin.adventure.memories.MemoriesPlugin;
import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.builtin.teleport.TeleportPlugin;
import com.hypixel.hytale.builtin.teleport.Warp;
import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.gameplay.GameplayConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import com.tontonsamael.PublicPortals;
import com.tontonsamael.publicportals.config.PortalConfig;
import com.tontonsamael.publicportals.ui.PortalEdit;
import com.tontonsamael.publicportals.ui.PortalSelectDestination;
import com.tontonsamael.publicportals.ui.PortalWarpList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

public class PortalService {
    public static class PortalData {
        // CONFIG
        public String id;
        public String name;
        @Nullable
        public String owner;
        @Nullable
        public String ownerName;
        public boolean allowTeleport;
        public String world;
        public int x;
        public int y;
        public int z;
        @Nullable
        public String destination;
        public boolean queueDestinationChange;

        // RUNTIME
        @Nullable
        Teleporter teleporter;

        public PortalData() {
        }

        public PortalData(String id, String name, @Nullable String owner, @Nullable String ownerName, boolean allowTeleport, String world, int x, int y, int z) {
            this.id = id;
            this.name = name;
            this.owner = owner;
            this.ownerName = ownerName;
            this.allowTeleport = allowTeleport;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public PortalData(String id, String name, @Nullable String owner, @Nullable String ownerName, boolean allowTeleport, String world, int x, int y, int z, @Nullable String destination, boolean queueDestinationChange) {
            this(id, name, owner, ownerName, allowTeleport, world, x, y, z);
            this.destination = destination;
            this.queueDestinationChange = queueDestinationChange;
        }

        public PortalData sanitize() {
            return new PortalData(id, name, owner, ownerName, allowTeleport, world, x, y, z, destination, queueDestinationChange);
        }
    }

    public static class PortalView {
        public String id;
        public String name;
        @Nullable
        public String destination;
        @Nullable
        public String ownerName;
        public boolean allowTeleport;
        public boolean wasPublic;

        public PortalView(String id, String name, @Nullable String destination, @Nullable String ownerName, boolean allowTeleport, boolean wasPublic) {
            this.id = id;
            this.name = name;
            this.destination = destination;
            this.ownerName = ownerName;
            this.allowTeleport = allowTeleport;
            this.wasPublic = wasPublic;
        }
    }

    private static PortalService instance;

    public static PortalService get() {
        if (instance == null) instance = new PortalService();
        return instance;
    }

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Map<String, PortalData> portals = new HashMap<>();

    private final Map<String, PortalView> playersEditPortal = new HashMap<>();
    private final Map<String, Integer> playersWarpSkipDelay = new HashMap<>();

    private static final Color chatErrorMsg = new Color(255, 55, 47);

    private PortalService() {
    }

    public int getPersonalLimit() {
        int recorded = MemoriesPlugin.get().getRecordedMemories().size();
        int amount = 2;
        int[] levelsAmount = new int[]{1, 1, 1, 2, 2};
        GameplayConfig conf = GameplayConfig.getAssetStore().getAssetMap().getAsset("Default");
        if (conf == null) return amount;
        Object memoriesConfObj = conf.getPluginConfig().values().stream()
                .filter(v -> v instanceof MemoriesGameplayConfig)
                .findAny().orElse(null);
        if (memoriesConfObj == null) return amount;
        int[] levels = ((MemoriesGameplayConfig) memoriesConfObj).getMemoriesAmountPerLevel();
        for (int i = 0; i < levels.length; i++)
            amount += recorded >= levels[i] ? levelsAmount[i] : 0;
        return amount;
    }

    private boolean isLimitMaxed() {
        int recorded = MemoriesPlugin.get().getRecordedMemories().size();
        GameplayConfig conf = GameplayConfig.getAssetStore().getAssetMap().getAsset("Default");
        if (conf == null) return false;
        Object memoriesConfObj = conf.getPluginConfig().values().stream()
                .filter(v -> v instanceof MemoriesGameplayConfig)
                .findAny().orElse(null);
        if (memoriesConfObj == null) return false;
        int[] levels = ((MemoriesGameplayConfig) memoriesConfObj).getMemoriesAmountPerLevel();
        return levels[levels.length - 1] <= recorded;
    }

    @Nullable
    private PortalData getSingleClosePortal(World world, BlockPosition position) {
        List<PortalData> closePortals = portals.values().stream()
                .filter(p -> p.world.equals(world.getName()) &&
                        PortalUtils.distance3d(position.x, position.y, position.z, p.x, p.y, p.z) <= 2f)
                .toList();
        return closePortals.size() == 1 ? closePortals.getFirst() : null;
    }

    @Nullable
    private PortalData getByWorldAndPositionOrClose(World world, BlockPosition position) {
        return portals.values().stream()
                .filter(p -> p.world.equals(world.getName()))
                .filter(p -> p.x == position.x && p.y == position.y && p.z == position.z)
                .findFirst()
                .orElseGet(() -> {
                    // check for close distance portals
                    PortalData closePortal = getSingleClosePortal(world, position);
                    if (closePortal != null) {
                        // fix portal data position
                        LOGGER.atInfo().log("Close portal adjust for new position");
                        closePortal.x = position.x;
                        closePortal.y = position.y;
                        closePortal.z = position.z;
                        PublicPortals.get().getConfigPortal().get()
                                .setPortal(closePortal);
                    }
                    return closePortal;
                });
    }

    public void onServerBoot() {
        Config<PortalConfig> config = PublicPortals.get().getConfigPortal();
        portals.clear();
        if (!config.get().isConfigInit()) {
            TeleportPlugin.get().getWarps().forEach((_, w) -> {
                String newId = PortalUtils.generateNewPortalId(portals.keySet());
                String finalName = !w.getId().isEmpty() ? w.getId() : newId;
                assert w.getTransform() != null;
                BlockPosition position = new BlockPosition(
                        (int) w.getTransform().getPosition().x,
                        (int) w.getTransform().getPosition().y,
                        (int) w.getTransform().getPosition().z
                );
                PortalData portal = new PortalData(newId, finalName,
                        null, null, false, w.getWorld(),
                        position.x, position.y, position.z);
                portals.put(newId, portal);
            });
            config.get().setPortals(portals);
            LOGGER.atInfo().log("Portals first launch configuration written (%d portals) !", portals.size());
        } else {
            config.get().getPortals().forEach((id, p) -> portals.put(id, p.sanitize()));
            LOGGER.atInfo().log("Portals configuration loaded (%d portals)", portals.size());
        }

        updateWarps();
    }

    private void updateWarps() {
        // update needed to sync map/radar markers
        Map<String, Warp> warps = TeleportPlugin.get().getWarps();
        warps.forEach((id, _) -> TeleportPlugin.get().getWarps().remove(id));
        Set<String> insertedIds = new HashSet<>();
        Map<String, World> worlds = new HashMap<>();
        portals.forEach((_, p) -> {
            String id = p.name.isEmpty() ? p.id : p.name;
            if (p.owner != null) id += String.format(" (%s)", p.ownerName);
            while (insertedIds.contains(id)) id += " ";
            if (!worlds.containsKey(p.world)) {
                World w = Universe.get().getWorlds().get(p.world);
                if (w == null) return;
                worlds.put(p.world, w);
            }
            Transform transform = new Transform(new Vector3i(p.x, p.y, p.z));
            warps.put(id, new Warp(transform,
                    id, worlds.get(p.world), p.ownerName != null ? p.ownerName : "*Teleporter", Instant.now()));
            insertedIds.add(id);
        });
        TeleportPlugin.get().saveWarps();
    }

    public int getPlayerPortalsCount(@Nonnull String uuid) {
        return portals.values().stream().map(p -> uuid.equals(p.owner) ? 1 : 0)
                .reduce(0, Integer::sum);
    }

    public void tickPlayer(Player player) {
        if (playersWarpSkipDelay.containsKey(player.getDisplayName())) {
            int val = playersWarpSkipDelay.get(player.getDisplayName()) - 1;
            if (val <= 0) playersWarpSkipDelay.remove(player.getDisplayName());
            else playersWarpSkipDelay.put(player.getDisplayName(), val);
        }

        // enable close portals
        if (player.getReference() == null) return;
        PlayerRef pref = player.getReference().getStore()
                .getComponent(player.getReference(), PlayerRef.getComponentType());
        if (pref == null) return;
        Vector3d playerPos = pref.getTransform().getPosition();
        Set<String> deletionQueue = new HashSet<>();
        portals.values().stream()
                .filter(p -> player.getWorld() != null &&
                        p.world.equals(player.getWorld().getName()))
                .forEach(p -> {
                    BlockPosition pos = new BlockPosition(p.x, p.y, p.z);
                    if (PortalUtils.distance3d(playerPos.x, playerPos.y, playerPos.z,
                            pos.x, pos.y, pos.z) > 20f) return;
                    PortalUtils.activatePortal(player.getWorld(), pos);
                    if (p.teleporter == null)
                        p.teleporter = PortalUtils.retrieveTeleporter(player.getWorld(), pos);
                    if(p.teleporter == null){
                        // invalid portal
                        deletionQueue.add(p.id);
                        return;
                    }
                    if(p.queueDestinationChange) {
                        p.teleporter.setWarp(p.destination);
                        p.queueDestinationChange = false;
                        PublicPortals.get().getConfigPortal().get().setPortal(p);
                    }
                });
        if(!deletionQueue.isEmpty()) {
            deletionQueue.forEach(this::onBreak);
        }
    }

    public void setWarpSkip(Player player, int duration) {
        playersWarpSkipDelay.put(player.getDisplayName(), duration);
    }

    public void onPlayerJoinWorld(Player player) {
        setWarpSkip(player, 5);
        if (player.getWorld() == null) return;

        String uuid = PortalUtils.getPlayerUUID(player);
        if (uuid != null) {
            // check and managed player name changes
            List<PortalData> ownedPortalsWithWrongOwnerName = portals.values().stream()
                    .filter(p -> uuid.equals(p.owner) && p.ownerName != null &&
                            !p.ownerName.equals(player.getDisplayName()))
                    .toList();
            if (!ownedPortalsWithWrongOwnerName.isEmpty()) {
                PortalConfig config = PublicPortals.get().getConfigPortal().get();
                ownedPortalsWithWrongOwnerName.forEach(p -> {
                    p.ownerName = player.getDisplayName();
                    config.setPortal(p);
                });

            }
        }
    }

    public void triggerCommand(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || pRef == null) return;
        player.getPageManager().openCustomPage(ref, store, new PortalWarpList(pRef));
    }

    public boolean onPlayerPlace(Player player, World world, BlockPosition position) {
        // personal limit reached
        if (!PortalUtils.checkOP(player) && player.getGameMode() != GameMode.Creative &&
                isPersonalCapReached(player)) {
            player.sendMessage(Message.translation(isLimitMaxed() ?
                            "server.interactions.teleporter.failed" :
                            "server.interactions.teleporter.failedCollectMore")
                    .color(chatErrorMsg));
            return false;
        }

        world.execute(() -> onPortalPlaced(player, world, position));
        return true;
    }

    public void onPortalPlaced(Player player, World world, BlockPosition position) {
        String uuid = PortalUtils.getPlayerUUID(player);
        if (uuid == null) return;
        Teleporter teleporter = PortalUtils.retrieveTeleporter(world, position);
        if (teleporter == null) {
            LOGGER.atSevere().log("PortalPlaced invalid teleporter target");
            return;
        }

        Config<PortalConfig> config = PublicPortals.get().getConfigPortal();
        // public if creative or op and personal limit reached
        boolean isPublic = player.getGameMode() == GameMode.Creative || (PortalUtils.checkOP(player) && isPersonalCapReached(player));

        String newPortalId = PortalUtils.generateNewPortalId(portals.keySet());
        PortalData portal = new PortalData(newPortalId, "", isPublic ? null : uuid,
                isPublic ? null : player.getDisplayName(), false,
                world.getName(), position.x, position.y, position.z);
        portals.put(portal.id, portal);

        config.get().setPortal(portal);

        PortalUtils.activatePortal(world, position);
        updateWarps();
    }

    @Nullable
    public CustomUIPage onPlayerUse(Player player, World world, BlockPosition position) {
        if (player.getReference() == null) return null;
        PlayerRef playerRef = player.getReference().getStore().getComponent(player.getReference(), PlayerRef.getComponentType());
        if (playerRef == null) return null;
        String uuid = PortalUtils.getPlayerUUID(player);
        if (uuid == null) return null;

        PortalData portal = getByWorldAndPositionOrClose(world, position);
        if (portal == null) {
            // lonely out-of-sync portal
            portal = new PortalData(PortalUtils.generateNewPortalId(portals.keySet()), "", null, null, false, world.getName(), position.x, position.y, position.z);
            portals.put(portal.id, portal);
            PublicPortals.get().getConfigPortal().get().setPortal(portal);
            updateWarps();
        }

        boolean isPublic = portal.owner == null;

        // public portal edit check
        if (!PortalUtils.checkOP(player) && player.getGameMode() != GameMode.Creative && isPublic) {
            player.sendMessage(Message.translation("portals.chat.publicEditForbidden")
                    .color(chatErrorMsg));
            return null;
        } else if (!isPublic && player.getGameMode() == GameMode.Creative) {
            player.sendMessage(Message.translation("portals.chat.creativeEditPersonalForbidden").color(chatErrorMsg));
            return null;
        } else if (!isPublic && !uuid.equals(portal.owner)) {
            player.sendMessage(Message.translation("portals.chat.belongsAnotherPlayer")
                    .color(chatErrorMsg));
            return null;
        }

        playersEditPortal.put(player.getDisplayName(), new PortalView(portal.id, portal.name,
                portal.destination, portal.ownerName, portal.allowTeleport,
                portal.ownerName == null));
        return new PortalEdit(playerRef);
    }

    public void onPlayerEnter(Player player, World world, BlockPosition position) {
        if (playersWarpSkipDelay.containsKey(player.getDisplayName())) return;
        if (player.getReference() == null) return;
        PlayerRef playerRef = player.getReference().getStore().getComponent(player.getReference(), PlayerRef.getComponentType());
        if (playerRef == null) return;
        PortalData portal = getByWorldAndPositionOrClose(world, position);
        if (portal == null) return;
        if(portal.destination == null || !portals.containsKey(portal.destination))
            player.getPageManager().openCustomPage(player.getReference(), player.getReference().getStore(), new PortalSelectDestination(playerRef, portal.id));
        else
            world.execute(() -> teleportPlayer(playerRef, portal.destination));
    }

    public void onMobEnter(Ref<EntityStore> entity, World world, BlockPosition position) {
        PortalData portal = getByWorldAndPositionOrClose(world, position);
        if (portal == null) return;
        if(portal.destination != null && portals.containsKey(portal.destination))
            world.execute(() -> teleportEntity(entity, portal.destination));
    }

    public boolean onPlayerBreak(Player player, World world, BlockPosition position) {
        String uuid = PortalUtils.getPlayerUUID(player);
        if (uuid == null) return false;

        PortalData portal = getByWorldAndPositionOrClose(world, position);
        if (portal == null) {
            LOGGER.atInfo().log("PortalBreak invalid target");
            player.sendMessage(Message.translation("portals.chat.invalid")
                    .color(chatErrorMsg));
            return player.getGameMode() == GameMode.Creative || PortalUtils.checkOP(player);
        }

        boolean isPublic = portal.owner == null;

        if (!PortalUtils.checkOP(player) && player.getGameMode() != GameMode.Creative &&
                (isPublic || !uuid.equals(portal.owner))) {
            player.sendMessage(Message.translation("portals.chat.belongsAnotherPlayer")
                    .color(chatErrorMsg));
            return false;
        }

        onBreak(portal.id);
        world.execute(() -> {
            this.activateAllPortals();
            player.sendMessage(Message.translation("server.commands.teleport.warp.removedWarp")
                    .param("name", portal.name.isEmpty() ? portal.id : portal.name));
        });
        return true;
    }

    private void onBreak(String portalId) {
        portals.values().forEach(p -> {
            // remove destinations
            if(portalId.equals(p.destination)) {
                p.destination = null;
                p.queueDestinationChange = true;
            }
        });
        portals.remove(portalId);
        PublicPortals.get().getConfigPortal().get().removePortal(portalId);
        updateWarps();
    }

    public void activateAllPortals() {
        Map<String, World> worlds = new HashMap<>();
        portals.forEach((_, p) -> {
            World world = worlds.computeIfAbsent(p.world,
                    _ -> Universe.get().getWorld(p.world));
            if (world != null && world.isAlive())
                world.execute(() -> PortalUtils.activatePortal(world,
                        new BlockPosition(p.x, p.y, p.z)));

        });
    }

    @Nullable
    public PortalView getView(PlayerRef playerRef) {
        return playersEditPortal.get(playerRef.getUsername());
    }

    public boolean isPersonalCapReached(Player player) {
        String uuid = PortalUtils.getPlayerUUID(player);
        assert uuid != null;
        return getPlayerPortalsCount(uuid) >= getPersonalLimit();
    }

    @Nullable
    public String getPortalNameError(Player player, String newName) {
        if (newName.isEmpty()) return null;
        if (newName.length() < 3) return "Name too short";
        PortalView view = playersEditPortal.get(player.getDisplayName());
        if (view == null) return "Invalid data";

        return portals.values().stream().anyMatch(p -> {
            if (p.id.equalsIgnoreCase(view.id)) return false;
            if (p.id.equalsIgnoreCase(newName)) return false;
            return p.name.equalsIgnoreCase(newName);
        }) ? "server.customUI.teleporter.errorWarpAlreadyExists" : null;
    }

    public void savePortal(PlayerRef playerRef) {
        Player player = PortalUtils.playerRefToPlayer(playerRef);
        if (player == null) return;
        String uuid = PortalUtils.getPlayerUUID(player);
        if (uuid == null) return;

        PortalView view = PortalService.get().getView(playerRef);
        if (view == null) return;

        boolean isPublic = view.ownerName == null;
        if (isPublic && !(PortalUtils.checkOP(player) || player.getGameMode() == GameMode.Creative)) {
            LOGGER.atSevere().log("Public portal can not be edited without permissions or creative");
            return;
        }
        PortalData portal = portals.get(view.id);
        if (portal == null) {
            LOGGER.atSevere().log("Invalid portal save target");
            return;
        }

        if (portal.owner == null && !isPublic && isPersonalCapReached(player)) {
            LOGGER.atSevere().log("Personal portals limit already reached");
            return;
        }

        if((portal.destination == null && view.destination != null) ||
                (portal.destination != null && view.destination == null) ||
                (portal.destination != null && !portal.destination.equals(view.destination)))
            portal.queueDestinationChange = true;
        portal.destination = view.destination;
        portal.name = view.name;
        portal.owner = isPublic ? null : uuid;
        portal.ownerName = isPublic ? null : player.getDisplayName();
        if(!isPublic && !view.allowTeleport) {
            // remove this newly restricted from destinations
            portals.values().stream()
                    .filter(p -> !p.id.equals(view.id) && view.id.equals(p.destination))
                    .forEach(p -> {
                        p.destination = null;
                        p.queueDestinationChange = true;
                    });
        }
        portal.allowTeleport = !isPublic && view.allowTeleport;
        PublicPortals.get().getConfigPortal().get().setPortal(portal);
        playersEditPortal.remove(player.getDisplayName());
        updateWarps();
    }

    public void teleportPlayer(PlayerRef playerRef, String portalId) {
        if(!portals.containsKey(portalId)){
            LOGGER.atSevere().log("(teleportPlayer) Invalid portal target '%s'", portalId);
            playerRef.sendMessage(Message.translation("server.commands.teleport.warp.unknownWarp")
                    .param("name", portalId)
                    .color(chatErrorMsg));
            return;
        }

        Player player = PortalUtils.playerRefToPlayer(playerRef);
        Ref<EntityStore> ref = playerRef.getReference();
        assert ref != null && player != null;
        setWarpSkip(player, 2);

        teleportEntity(ref, portalId);

        PortalData portal = portals.get(portalId);
        player.sendMessage(Message.translation("server.commands.teleport.warp.warpedTo")
                .param("name", portal.name.isEmpty() ? portal.id : portal.name));
    }

    public void teleportEntity(Ref<EntityStore> ref, String portalId) {
        PortalData portal = portals.get(portalId);
        if (portal == null) {
            LOGGER.atSevere().log("(teleportMob) Invalid portal target '%s'", portalId);
            return;
        }

        World world = Universe.get().getWorld(portal.world);
        if (world == null) return;
        Teleport teleporter = Teleport.createForPlayer(new Transform(
                new Vector3d(portal.x + .5f, portal.y + .5f, portal.z + .5f)));

        TransformComponent transformComponent = ref.getStore().getComponent(ref, TransformComponent.getComponentType());
        HeadRotation headRotationComponent = ref.getStore().getComponent(ref, HeadRotation.getComponentType());
        if (transformComponent == null || headRotationComponent == null) return;

        Vector3d playerPosition = transformComponent.getPosition();
        Vector3f playerHeadRotation = headRotationComponent.getRotation();
        ref.getStore().ensureAndGetComponent(ref, TeleportHistory.getComponentType()).append(world, playerPosition.clone(), playerHeadRotation.clone(), "Warp '" + portalId + "'");
        ref.getStore().addComponent(ref, Teleport.getComponentType(), teleporter);
    }

    private List<PortalData> getSortedPortals(Player player, List<PortalData> list) {
        return list.stream().sorted((a, b) -> {
            boolean aPublic = a.ownerName == null;
            boolean bPublic = b.ownerName == null;
            String aName = a.name.isEmpty() ? a.id : a.name;
            String bName = b.name.isEmpty() ? b.id : b.name;
            if (aPublic != bPublic) return aPublic ? PortalUtils.SORT_A_BEFORE_B : PortalUtils.SORT_B_BEFORE_A;
            else if (aPublic) return aName.compareToIgnoreCase(bName);
            if (!a.ownerName.equals(b.ownerName))
                return a.ownerName.equals(player.getDisplayName()) ? PortalUtils.SORT_A_BEFORE_B :
                        b.ownerName.equals(player.getDisplayName()) ? PortalUtils.SORT_B_BEFORE_A :
                                a.ownerName.compareTo(b.ownerName);
            return aName.compareToIgnoreCase(bName);
        }).toList();
    }

    public List<PortalView> getPortalsList(Player player, @Nullable String filteredPortalId, boolean forced) {
        String uuid = PortalUtils.getPlayerUUID(player);
        if (uuid == null) return new ArrayList<>();

        return getSortedPortals(player, portals.values().stream().toList())
                .stream()
                .filter(p -> {
                    // filtered portal
                    if (filteredPortalId != null && filteredPortalId.equalsIgnoreCase(p.id)) return false;
                    // belongs to another player and does not allow teleport
                    if (!forced && p.owner != null && !p.allowTeleport && !uuid.equals(p.owner))
                        return false;
                    return true;
                })
                .map(p ->
                        new PortalView(p.id, p.name, p.destination, p.ownerName,
                                p.allowTeleport, p.ownerName == null))
                .toList();
    }

    public List<PortalUtils.KeyValueData<String, DropdownEntryInfo>> getDestinationsList(Player player) {
        List<PortalUtils.KeyValueData<String, DropdownEntryInfo>> res = List.of(
                new PortalUtils.KeyValueData<>("", new DropdownEntryInfo(LocalizableString.fromMessageId("server.customUI.teleporter.noWarp"), "")
        ));
        String uuid = PortalUtils.getPlayerUUID(player);
        if (uuid == null) return res;

        PortalView view = playersEditPortal.get(player.getDisplayName());
        if (view == null) return res;

        return Stream.of(res, getSortedPortals(player, portals.values().stream().toList())
                .stream().filter(p -> {
                    // current portal
                    if (p.id.equalsIgnoreCase(view.id)) return false;
                    // target is private & restricted
                    if(p.owner != null && !p.allowTeleport) {
                        // current is public or target belongs to someone else
                        if (view.ownerName == null || !uuid.equals(p.owner)) return false;
                    }
                    return true;
                }).map(p -> new PortalUtils.KeyValueData<>(p.id, new DropdownEntryInfo(LocalizableString.fromString(
                        PortalUtils.capitalize(p.name.isEmpty() ? p.id : p.name)
                ), p.id))).toList()).flatMap(List::stream).toList();
    }
}
