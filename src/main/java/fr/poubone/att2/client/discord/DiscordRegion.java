package fr.poubone.att2.client.discord;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class DiscordRegion {
    public static final String FALLBACK_ASSET = "logo-brillant";
    private static final DiscordRegion.Anchor[] OVERWORLD = new DiscordRegion.Anchor[]{
        new DiscordRegion.Anchor("hill-valley", "discord.region.hill_valley", 788.0, 908.0),
        new DiscordRegion.Anchor("ouranos", "discord.region.ouranos", 7046.0, 6972.0),
        new DiscordRegion.Anchor("nojelanth", "discord.region.nojelanth", -7371.0, -4531.0),
        new DiscordRegion.Anchor("ryliath", "discord.region.ryliath", -5005.0, -5077.0),
        new DiscordRegion.Anchor("meleim", "discord.region.meleim", -3845.0, -5808.0),
        new DiscordRegion.Anchor("eolorion", "discord.region.eolorion", -5248.0, -6202.0),
        new DiscordRegion.Anchor("elcheol", "discord.region.elcheol", -5607.0, -6382.0),
        new DiscordRegion.Anchor("kortaek", "discord.region.kortaek", -5533.0, -4678.0),
        new DiscordRegion.Anchor("kert", "discord.region.kert", -5523.0, -5033.0),
        new DiscordRegion.Anchor("asunark", "discord.region.asunark", -3661.0, -4977.0),
        new DiscordRegion.Anchor("owsastr", "discord.region.owsastr", -4661.0, -4546.0),
        new DiscordRegion.Anchor("soquai", "discord.region.soquai", -4790.0, -5686.0)
    };
    private static final DiscordRegion.Info SYLBERLAND = new DiscordRegion.Info("sylberland", "discord.region.sylberland");
    private static final DiscordRegion.Info ANGBAND = new DiscordRegion.Info("angband", "discord.region.angband");
    private static final DiscordRegion.Info BILLGART = new DiscordRegion.Info("billgart", "discord.region.billgart");
    private static final double MAX_TOWN_DISTANCE_SQ = 3240000.0;

    private DiscordRegion() {
    }

    public static DiscordRegion.Info of(LocalPlayer player) {
        if (player != null && player.level() != null) {
            String id = String.valueOf(player.level().dimension());
            if (!id.contains("the_nether") && !id.endsWith(":nether")) {
                if (!id.contains("the_end") && !id.endsWith(":end")) {
                    Vec3 pos = player.position();
                    DiscordRegion.Anchor nearest = null;
                    double best = Double.MAX_VALUE;

                    for (DiscordRegion.Anchor anchor : OVERWORLD) {
                        double dx = pos.x - anchor.x;
                        double dz = pos.z - anchor.z;
                        double d = dx * dx + dz * dz;
                        if (d < best) {
                            best = d;
                            nearest = anchor;
                        }
                    }

                    return nearest != null && !(best > 3240000.0) ? new DiscordRegion.Info(nearest.assetKey, nearest.nameKey) : SYLBERLAND;
                } else {
                    return BILLGART;
                }
            } else {
                return ANGBAND;
            }
        } else {
            return SYLBERLAND;
        }
    }

    private record Anchor(String assetKey, String nameKey, double x, double z) {
    }

    public record Info(String assetKey, String nameKey) {
    }
}
