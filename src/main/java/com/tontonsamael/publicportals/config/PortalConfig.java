package com.tontonsamael.publicportals.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.tontonsamael.PublicPortals;
import com.tontonsamael.publicportals.PortalService;

import java.util.HashMap;
import java.util.Map;

public class PortalConfig {
    private static final BuilderCodec<PortalService.PortalData> PORTAL_CODEC =
            BuilderCodec.builder(PortalService.PortalData.class, PortalService.PortalData::new)
                    .append(new KeyedCodec<>("Id", Codec.STRING),
                            (p, id) -> p.id = id,
                            (p) -> p.id)
                    .add()
                    .append(new KeyedCodec<>("Name", Codec.STRING),
                            (p, name) -> p.name = name,
                            (p) -> p.name)
                    .add()
                    .append(new KeyedCodec<>("Owner", Codec.STRING),
                            (p, owner) -> p.owner = owner,
                            (p) -> p.owner)
                    .add()
                    .append(new KeyedCodec<>("OwnerName", Codec.STRING),
                            (p, ownerName) -> p.ownerName = ownerName,
                            (p) -> p.ownerName)
                    .add()
                    .append(new KeyedCodec<>("AllowTeleport", Codec.BOOLEAN),
                            (p, allowed) -> p.allowTeleport = allowed,
                            (p) -> p.allowTeleport)
                    .add()
                    .append(new KeyedCodec<>("World", Codec.STRING),
                            (p, name) -> p.world = name,
                            (p) -> p.world)
                    .add()
                    .append(new KeyedCodec<>("X", Codec.INTEGER),
                            (p, x) -> p.x = x,
                            (p) -> p.x)
                    .add()
                    .append(new KeyedCodec<>("Y", Codec.INTEGER),
                            (p, y) -> p.y = y,
                            (p) -> p.y)
                    .add()
                    .append(new KeyedCodec<>("Z", Codec.INTEGER),
                            (p, z) -> p.z = z,
                            (p) -> p.z)
                    .add()
                    .append(new KeyedCodec<>("Yaw", Codec.FLOAT),
                            (p, yaw) -> p.yaw = yaw,
                            (p) -> p.yaw)
                    .add()
                    .append(new KeyedCodec<>("Destination", Codec.STRING),
                            (p, destination) -> p.destination = destination,
                            (p) -> p.destination)
                    .add()
                    .append(new KeyedCodec<>("QueueDestinationChange", Codec.BOOLEAN),
                            (p, queue) -> p.queueDestinationChange = queue,
                            (p) -> p.queueDestinationChange)
                    .add()
                    .build();
    private static final MapCodec<PortalService.PortalData, Map<String, PortalService.PortalData>> PORTALS_CODEC =
            new MapCodec<>(PORTAL_CODEC, HashMap::new, false);
    ;
    public static final BuilderCodec<PortalConfig> CODEC =
            BuilderCodec.builder(PortalConfig.class, PortalConfig::new)
                    .append(new KeyedCodec<>("Portals", PORTALS_CODEC),
                            (c, warps) -> c.portals = warps,
                            (c) -> c.portals
                    ).add()
                    .build();

    private Map<String, PortalService.PortalData> portals;

    public void save() {
        PublicPortals.get().getConfigPortal().save();
    }

    public boolean isConfigInit() {
        return portals != null;
    }

    public Map<String, PortalService.PortalData> getPortals() {
        return portals == null ? new HashMap<>() : new HashMap<>(portals);
    }

    public void setPortal(PortalService.PortalData portal) {
        portals.put(portal.id, portal.sanitize());
        save();
    }

    public void removePortal(String id) {
        portals.remove(id);
        save();
    }

    public void setPortals(Map<String, PortalService.PortalData> newPortals) {
        portals = new HashMap<>();
        newPortals.forEach((id, p) -> portals.put(id, p.sanitize()));
        save();
    }
}
