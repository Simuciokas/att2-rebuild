package fr.poubone.att2.mixin.teleport;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("setPosition")
    void gtalikeTeleport$setPosition(Vec3 var1);

    @Invoker("setRotation")
    void gtalikeTeleport$setRotation(float var1, float var2);
}
