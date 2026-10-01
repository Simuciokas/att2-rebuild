package fr.poubone.att2.client.sync;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class PingTargeting {
    private static final double MAX_DISTANCE = 1024.0;

    private PingTargeting() {
    }

    public static PingTargeting.Target lookAt(Minecraft client) {
        if (client.player != null && client.level != null) {
            Vec3 start = client.player.getEyePosition();
            Vec3 direction = client.player.getViewVector(1.0F).normalize();
            Vec3 end = start.add(direction.scale(1024.0));
            BlockHitResult blockHit = client.level.clip(new ClipContext(start, end, Block.OUTLINE, Fluid.NONE, client.player));
            double nearest = blockHit.getType() == Type.MISS ? 1048576.0 : start.distanceToSqr(blockHit.getLocation());
            PingTargeting.Target best = blockHit.getType() == Type.MISS
                ? null
                : new PingTargeting.Target(
                    blockHit.getLocation(), client.level.getBlockState(blockHit.getBlockPos()).getBlock().getName().getString(), null, ItemStack.EMPTY
                );
            AABB scan = client.player.getBoundingBox().expandTowards(direction.scale(1024.0)).inflate(1.0);

            for (Entity entity : client.level
                .getEntities(client.player, scan, candidate -> candidate.isAlive() && (candidate.isPickable() || candidate instanceof ItemEntity))) {
                Optional<Vec3> hit = entity.getBoundingBox().inflate(0.3).clip(start, end);
                if (!hit.isEmpty()) {
                    double distance = start.distanceToSqr(hit.get());
                    if (!(distance >= nearest)) {
                        nearest = distance;
                        String label = entity instanceof ItemEntity item ? item.getItem().getHoverName().getString() : entity.getDisplayName().getString();
                        best = new PingTargeting.Target(
                            entity.position().add(0.0, entity.getBbHeight(), 0.0),
                            label,
                            entity.getUUID(),
                            entity instanceof ItemEntity item ? item.getItem().copy() : ItemStack.EMPTY
                        );
                    }
                }
            }

            return best;
        } else {
            return null;
        }
    }

    public record Target(Vec3 pos, String label, UUID entityId, ItemStack item) {
    }
}
