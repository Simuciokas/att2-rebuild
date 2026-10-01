package fr.poubone.att2.client.teleport;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class TeleportTransitionController {
    private static final int EXIT_BODY_TICKS = 12;
    private static final int EXTERNAL_ARRIVAL_MARGIN_TICKS = 15;
    private static final int ARRIVAL_EFFECT_GRACE_TICKS = 40;
    private static int ticksSinceCleared = Integer.MAX_VALUE;
    private static final int PRE_TRAVEL_WAIT_TICKS = 10;
    private static final int DEFAULT_TRAVEL_TICKS = 40;
    private static final int MIN_TRAVEL_TICKS = 20;
    private static final int MAX_TRAVEL_TICKS = 60;
    private static final int TRAVEL_EASE_EDGE_TICKS = 10;
    private static final double TRAVEL_BLOCKS_PER_TICK = 30.0;
    private static final int PRE_PUSH_WAIT_TICKS = 10;
    private static final int CROSS_DIMENSION_WAIT_TICKS = 20;
    private static final int ENTER_BODY_TICKS = 12;
    private static final int POST_RELEASE_EFFECT_TICKS = 8;
    private static final int HUD_FADE_TICKS = 8;
    private static final int CUSTOM_TRAVEL_SOUND_FADE_TICKS = 30;
    private static final int FALLBACK_TERRAIN_SWEEP_TICKS = 10;
    private static final int FALLBACK_TERRAIN_FADE_OUT_DELAY_TICKS = 4;
    private static final float FALLBACK_TERRAIN_SWEEP_BAND = 0.22F;
    private static final float SHADER_SCREEN_MASK_SWEEP_BAND = 0.22F;
    private static final int TERRAIN_VISIBILITY_REFRESH_MIN_SECTION_STEP = 4;
    private static final boolean SHADER_SCREEN_MASK_ENABLED = true;
    private static final boolean VOXY_LOADED = FabricLoader.getInstance().isModLoaded("voxy");
    private static final boolean DISTANT_HORIZONS_LOADED = FabricLoader.getInstance().isModLoaded("distanthorizons");
    private static final boolean BOBBY_LOADED = FabricLoader.getInstance().isModLoaded("bobby");
    private static final float TRAVEL_EASE_CONTROL_X1 = 0.55F;
    private static final float TRAVEL_EASE_CONTROL_X2 = 0.55F;
    private static final double ZOOM_LOOK_SHAKE_PITCH_AMPLITUDE = 1.05;
    private static final double ZOOM_LOOK_SHAKE_YAW_AMPLITUDE = 0.15;
    private static final float BODY_LOOK_SPEED_MULTIPLIER = 1.5F;
    private static final float TOP_DOWN_PITCH = 90.0F;
    private static final float MINECRAFT_STEP_SOUND_VOLUME = 0.86F;
    private static final float MINECRAFT_STEP_SOUND_PITCH = 1.0F;
    private static final float MINECRAFT_TRAVEL_SOUND_VOLUME = 0.48F;
    private static final float MINECRAFT_TRAVEL_SOUND_PITCH = 1.0F;
    private static final float MINECRAFT_BODY_SOUND_VOLUME = 1.0F;
    private static final float MINECRAFT_BODY_SOUND_PITCH = 0.8F;
    private static final int MINECRAFT_BODY_SOUND_LAYERS = 3;
    private static Runnable pendingAction;
    private static Vec3 startFeet;
    private static Vec3 startEye;
    private static Vec3 plannedTargetFeet;
    private static boolean plannedTargetFeetStable = true;
    private static Vec3 actualTargetFeet;
    private static Vec3 arrivalCameraFeet;
    private static Vec3 lastObservedPlayerFeet;
    private static Vec3 enterBodyTargetEye;
    private static String plannedTargetDimensionId;
    private static ResourceKey<Level> startDimension;
    private static CameraType previousCameraType;
    private static boolean previousHudHidden;
    private static boolean previousSmartCull;
    private static float startYaw;
    private static float startPitch;
    private static int ticks;
    private static int travelTicks = 40;
    private static int totalTicks = getFixedTotalTicks() + 40;
    private static boolean commandSent;
    private static boolean cameraReleased;
    private static boolean hudSuppressed;
    private static boolean previousHardCutCullingActive;
    private static boolean skipTravel;
    private static int skipTravelArrivalDelayTicks;
    private static float enterBodyTargetYaw;
    private static float enterBodyTargetPitch;
    private static int lastVisibilitySectionX = Integer.MIN_VALUE;
    private static int lastVisibilitySectionY = Integer.MIN_VALUE;
    private static int lastVisibilitySectionZ = Integer.MIN_VALUE;
    private static TeleportTransitionController.FadingTravelSound activeTravelSound;

    private TeleportTransitionController() {
    }

    public static void startExternal(Minecraft client, Vec3 plannedTarget, int expectedArrivalTicks) {
        LocalPlayer player = client.player;
        if (player != null) {
            boolean stable = plannedTarget != null;
            start(client, stable ? plannedTarget : getFeetPos(player), null, () -> {}, true, stable);
            int minimumTravelTicks = expectedArrivalTicks + 15 - getTravelStartTick();
            if (travelTicks < minimumTravelTicks) {
                travelTicks = minimumTravelTicks;
                totalTicks = getFixedTotalTicks() + travelTicks;
            }
        }
    }

    public static void start(Minecraft client, Vec3 plannedTarget, Runnable action) {
        start(client, plannedTarget, null, action, true);
    }

    public static void start(Minecraft client, Vec3 plannedTarget, Runnable action, boolean closeOpenScreen) {
        start(client, plannedTarget, null, action, closeOpenScreen);
    }

    public static void start(Minecraft client, Vec3 plannedTarget, String targetDimensionId, Runnable action) {
        start(client, plannedTarget, targetDimensionId, action, true);
    }

    public static void start(Minecraft client, Vec3 plannedTarget, String targetDimensionId, Runnable action, boolean closeOpenScreen) {
        start(client, plannedTarget, targetDimensionId, action, closeOpenScreen, true);
    }

    private static void start(
        Minecraft client, Vec3 plannedTarget, String targetDimensionId, Runnable action, boolean closeOpenScreen, boolean stablePlannedTarget
    ) {
        LocalPlayer player = client.player;
        if (player != null) {
            stopCustomTravelSound(client);
            pendingAction = action;
            startFeet = getFeetPos(player);
            startEye = player.getEyePosition();
            plannedTargetFeet = plannedTarget;
            plannedTargetFeetStable = stablePlannedTarget;
            actualTargetFeet = null;
            arrivalCameraFeet = stablePlannedTarget ? plannedTarget : null;
            lastObservedPlayerFeet = startFeet;
            enterBodyTargetEye = null;
            startDimension = client.level == null ? Level.OVERWORLD : client.level.dimension();
            plannedTargetDimensionId = normalizeTargetDimensionId(targetDimensionId);
            previousCameraType = client.options.getCameraType();
            previousHudHidden = client.options.hideGui;
            previousSmartCull = client.smartCull;
            startYaw = player.getYRot();
            startPitch = player.getXRot();
            ticks = 0;
            skipTravel = shouldSkipTravelForTargetDimension(plannedTargetDimensionId);
            skipTravelArrivalDelayTicks = 0;
            enterBodyTargetYaw = 0.0F;
            enterBodyTargetPitch = 0.0F;
            travelTicks = skipTravel ? 0 : calculateTravelTicks(startFeet, plannedTargetFeet);
            totalTicks = getFixedTotalTicks() + travelTicks;
            commandSent = false;
            cameraReleased = false;
            hudSuppressed = false;
            previousHardCutCullingActive = false;
            resetVisibilityRefreshState();
            if (client.screen != null) {
                if (closeOpenScreen) {
                    client.setScreen(null);
                } else {
                    client.screen = null;
                }
            }

            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            client.smartCull = false;
            client.levelRenderer.needsUpdate();
            SodiumCompat.beginTransition(client, isFallbackTerrainMode());
        }
    }

    public static void tick(Minecraft client) {
        if (!isRunning()) {
            if (ticksSinceCleared < Integer.MAX_VALUE) {
                ticksSinceCleared++;
            }
        } else if (client.player == null || client.level == null || client.getConnection() == null) {
            clear(client);
        } else if (client.player.isDeadOrDying()) {
            clear(client);
        } else {
            ticks++;
            updateHardCutTerrainState(client);
            updateHudVisibility(client);
            if (ticks == 1) {
                playBodyTransitionSound(client, false);
            }

            if (ticks == getEnterStartTick()) {
                playBodyTransitionSound(client, true);
            }

            playStepSound(client, ticks);
            if (!skipTravel && ticks == getTravelStartTick()) {
                playTravelSound(client);
            }

            if (!skipTravel && ticks == getTravelEndTick()) {
                fadeCustomTravelSound();
            }

            if (!commandSent && ticks >= getCommandSendTick()) {
                commandSent = true;
                runPendingAction();
            }

            Vec3 playerFeet = getFeetPos(client.player);
            if (commandSent && ticks > getPullEndTick() + 2 && actualTargetFeet == null && hasArrivedAtTeleportTarget(client, playerFeet)) {
                recordActualTargetFeet(playerFeet);
                if (skipTravel) {
                    skipTravelArrivalDelayTicks = Math.max(0, ticks - getTravelEndTick());
                    totalTicks = totalTicks + skipTravelArrivalDelayTicks;
                }
            }

            lastObservedPlayerFeet = playerFeet;
            if (!cameraReleased && ticks >= totalTicks) {
                if (!commandSent) {
                    commandSent = true;
                    runPendingAction();
                }

                cameraReleased = true;
                restoreCameraType(client);
                restoreSmartCull(client);
                SodiumCompat.endTransition();
            }

            if (ticks >= totalTicks + 8) {
                clear(client);
            }
        }
    }

    public static boolean isRunning() {
        return pendingAction != null;
    }

    public static boolean shouldSuppressArrivalEffects() {
        return isRunning() || ticksSinceCleared <= 40;
    }

    public static float getProgress() {
        return !isRunning() ? 0.0F : Mth.clamp((float)ticks / totalTicks, 0.0F, 1.0F);
    }

    public static int getTicks() {
        return ticks;
    }

    public static boolean shouldBlockPlayerInput() {
        return TeleportAnimationConfig.isPlayerFreezeEnabled() && isRunning() && !cameraReleased;
    }

    public static boolean shouldBlockGameplayInput() {
        if (!shouldBlockPlayerInput()) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        return client == null || client.screen == null;
    }

    public static boolean shouldSuppressGameMenu() {
        return false;
    }

    public static boolean shouldHideLocalPlayerModel() {
        if (isRunning() && !cameraReleased) {
            int hideTicks = TeleportAnimationConfig.getLocalPlayerHideTicks();
            return hideTicks > 0 && (ticks <= hideTicks || ticks >= totalTicks - hideTicks);
        } else {
            return false;
        }
    }

    public static TeleportTransitionController.CameraFrame getCameraFrame(float tickProgress) {
        if (!isRunning()) {
            return null;
        } else {
            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null) {
                return null;
            } else if (cameraReleased) {
                return null;
            } else {
                float frameTick = Math.min(ticks + tickProgress, totalTicks);
                if (frameTick <= 12.0F) {
                    return exitBodyFrame(frameTick / 12.0F);
                } else if (frameTick <= getPullStartTick()) {
                    return startBodyHoldFrame(frameTick);
                } else {
                    int pullMotionEnd = getPullMotionEndTick();
                    if (frameTick <= pullMotionEnd) {
                        return pullFrame((frameTick - getPullStartTick()) / Math.max(1.0F, pullMotionEnd - getPullStartTick()), frameTick);
                    } else if (frameTick <= getTravelStartTick()) {
                        return topDownFrame(startFeet, startYaw, frameTick, getStartTravelAltitude());
                    } else {
                        int travelStart = getTravelStartTick();
                        if (!skipTravel && frameTick <= travelStart + travelTicks) {
                            return travelFrame((frameTick - travelStart) / travelTicks, frameTick);
                        } else if (skipTravel && actualTargetFeet == null) {
                            return topDownFrame(startFeet, startYaw, frameTick, getStartTravelAltitude());
                        } else {
                            int pushMotionStart = getPushMotionStartTick();
                            if (frameTick <= pushMotionStart) {
                                return prePushTopDownFrame(getArrivalCameraFeet(), startYaw, frameTick);
                            } else if (frameTick < getEnterHoldStartTick()) {
                                return pushFrame(player, (frameTick - pushMotionStart) / Math.max(1.0F, getEnterHoldStartTick() - pushMotionStart), frameTick);
                            } else {
                                return frameTick <= getEnterStartTick()
                                    ? enterBodyHoldFrame(frameTick)
                                    : enterBodyFrame(player, (frameTick - getEnterStartTick()) / 12.0F);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void requestTerrainVisibilityUpdate(Vec3 cameraPos) {
        if (isRunning() && !cameraReleased) {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && client.levelRenderer != null) {
                int sectionX = Mth.floor(cameraPos.x / 16.0);
                int sectionZ = Mth.floor(cameraPos.z / 16.0);
                if (lastVisibilitySectionX != Integer.MIN_VALUE && lastVisibilitySectionZ != Integer.MIN_VALUE) {
                    int sectionStep = getTerrainVisibilityRefreshSectionStep(client);
                    if (Math.abs(sectionX - lastVisibilitySectionX) >= sectionStep || Math.abs(sectionZ - lastVisibilitySectionZ) >= sectionStep) {
                        lastVisibilitySectionX = sectionX;
                        lastVisibilitySectionZ = sectionZ;
                        client.levelRenderer.needsUpdate();
                        SodiumCompat.scheduleTerrainUpdate();
                    }
                } else {
                    lastVisibilitySectionX = sectionX;
                    lastVisibilitySectionZ = sectionZ;
                }
            }
        }
    }

    public static boolean shouldForceTerrainFrustumApply() {
        return isRunning() && !cameraReleased;
    }

    public static boolean shouldPreferVoxyOnlyTerrain() {
        return isVoxyTerrainAvailable() && !isDistantHorizonsTerrainAvailable() && !isBobbyTerrainAvailable() && isRunning() && !cameraReleased
            ? ticks >= getPullEndTick() && ticks < getPushStartTick()
            : false;
    }

    public static boolean shouldHideFallbackTerrain() {
        return isFallbackTerrainMode() && !cameraReleased ? ticks >= getFallbackTerrainHideTick() && ticks < getFallbackTerrainShowTick() : false;
    }

    public static boolean shouldApplyFallbackTerrainVisibility() {
        return isFallbackTerrainMode() && !cameraReleased;
    }

    public static boolean shouldHardCutFallbackTerrain() {
        return shouldApplyFallbackTerrainVisibility() && IrisCompat.shouldUseHardTerrainCut();
    }

    public static boolean shouldHideHardCutFallbackTerrain() {
        return shouldHardCutFallbackTerrain() && shouldHideFallbackTerrain();
    }

    public static boolean shouldUseHardCutFallbackTerrainCulling() {
        return shouldHardCutFallbackTerrain() && !shouldUseShaderScreenMaskOnly() && (isFallbackTerrainFadeOutActive() || shouldHideFallbackTerrain());
    }

    public static boolean shouldRecoverHardCutFallbackTerrain() {
        return shouldHardCutFallbackTerrain() && !shouldUseShaderScreenMaskOnly() && ticks >= getFallbackTerrainShowTick();
    }

    public static boolean shouldUseShaderScreenMaskOnly() {
        return shouldHardCutFallbackTerrain();
    }

    public static boolean shouldApplyVanillaFallbackTerrainVisibility() {
        return shouldApplyFallbackTerrainVisibility()
            && !shouldUseShaderScreenMaskOnly()
            && ticks >= getVanillaTerrainFadeOutStartTick()
            && ticks < getVanillaTerrainFadeInEndTick();
    }

    public static boolean shouldHideFallbackSkyCelestials() {
        return isFallbackTerrainMode() && !cameraReleased;
    }

    public static float getFallbackTerrainSectionVisibility(BlockPos renderOrigin) {
        return getFallbackTerrainSectionVisibility(renderOrigin.getX() + 8.0, renderOrigin.getY() + 8.0, renderOrigin.getZ() + 8.0);
    }

    public static float getFallbackTerrainSectionVisibility(double centerX, double centerY, double centerZ) {
        if (!shouldApplyFallbackTerrainVisibility()) {
            return 1.0F;
        } else {
            float frameTick = ticks;
            int fadeOutStart = getVanillaTerrainFadeOutStartTick();
            int fadeOutEnd = getVanillaTerrainFadeOutEndTick();
            int fadeInStart = getVanillaTerrainFadeInStartTick();
            int fadeInEnd = getVanillaTerrainFadeInEndTick();
            if (frameTick >= fadeOutEnd && frameTick < fadeInStart) {
                return 0.0F;
            } else if (frameTick >= fadeOutStart && frameTick < fadeOutEnd) {
                float progress = smoothStep((frameTick - fadeOutStart) / 10.0F);
                float threshold = 1.0F - getDistanceOrderTowardPoint(centerX, centerZ, startFeet, getBestTargetFeet());
                return 1.0F - smoothStep((progress - threshold) / 0.22F);
            } else if (frameTick >= fadeInStart && frameTick < fadeInEnd) {
                float progress = smoothStep((frameTick - fadeInStart) / 10.0F);
                float threshold = getDistanceOrderTowardPoint(centerX, centerZ, getBestTargetFeet(), startFeet);
                return smoothStep((progress - threshold) / 0.22F);
            } else {
                return 1.0F;
            }
        }
    }

    public static float getShaderScreenMaskSectionOpacity(double centerX, double centerY, double centerZ) {
        if (!shouldUseShaderScreenMaskOnly()) {
            return 0.0F;
        } else {
            float frameTick = ticks;
            int fadeOutStart = getFallbackTerrainFadeOutStartTick();
            int fadeOutEnd = getFallbackTerrainFadeOutEndTick();
            int fadeInStart = getFallbackTerrainFadeInStartTick();
            int fadeInEnd = getFallbackTerrainFadeInEndTick();
            if (frameTick >= fadeOutEnd && frameTick < fadeInStart) {
                return 1.0F;
            } else if (frameTick >= fadeOutStart && frameTick < fadeOutEnd) {
                float progress = smoothStep((frameTick - fadeOutStart) / 10.0F);
                float threshold = getShaderMaskSweepThreshold(getDistanceOrderTowardPoint(centerX, centerZ, startFeet, getBestTargetFeet()));
                return smoothStep((progress - threshold) / 0.22F);
            } else if (frameTick >= fadeInStart && frameTick < fadeInEnd) {
                float progress = smoothStep((frameTick - fadeInStart) / 10.0F);
                float threshold = getShaderMaskSweepThreshold(1.0F - getDistanceOrderTowardPoint(centerX, centerZ, getBestTargetFeet(), startFeet));
                return 1.0F - smoothStep((progress - threshold) / 0.22F);
            } else {
                return 0.0F;
            }
        }
    }

    public static boolean shouldRenderFallbackTerrainSection(double centerX, double centerY, double centerZ) {
        return getFallbackTerrainSectionVisibility(centerX, centerY, centerZ) >= 0.5F;
    }

    public static boolean shouldCullFallbackTerrainSection(double centerX, double centerY, double centerZ) {
        if (!shouldUseHardCutFallbackTerrainCulling()) {
            return false;
        } else {
            return shouldHideFallbackTerrain() ? true : getFallbackTerrainSectionVisibility(centerX, centerY, centerZ) < 0.5F;
        }
    }

    public static float getStepEffectIntensity(float tickProgress) {
        return getStepEffectIntensity(tickProgress, true);
    }

    public static float getCameraMotionStepEffectIntensity(float tickProgress) {
        return getStepEffectIntensity(tickProgress, false);
    }

    private static float getStepEffectIntensity(float tickProgress, boolean includeReleasePulse) {
        if (!isRunning()) {
            return 0.0F;
        }

        float frameTick = ticks + tickProgress;
        float intensity = 0.0F;
        intensity = Math.max(intensity, pulseAfter(frameTick, getPullStageTick(0)));
        intensity = Math.max(intensity, pulseAfter(frameTick, getPullStageTick(1)));
        intensity = Math.max(intensity, pulseAfter(frameTick, getPullStageTick(2)));
        intensity = Math.max(intensity, pulseAfter(frameTick, getPushStageTick(1)));
        intensity = Math.max(intensity, pulseAfter(frameTick, getPushStageTick(2)));
        if (includeReleasePulse) {
            intensity = Math.max(intensity, pulseAfter(frameTick, getEnterHoldStartTick()));
        }

        return intensity;
    }

    public static float getHudFadeOverlayIntensity(float tickProgress) {
        if (!isRunning()) {
            return 0.0F;
        } else {
            float frameTick = ticks + tickProgress;
            float releaseTick = totalTicks;
            if (frameTick < 8.0F) {
                return smoothStep(frameTick / 8.0F) * 0.46F;
            } else if (frameTick < 16.0F) {
                return (1.0F - smoothStep((frameTick - 8.0F) / 8.0F)) * 0.46F;
            } else {
                return frameTick >= releaseTick && frameTick <= releaseTick + 8.0F ? (1.0F - smoothStep((frameTick - releaseTick) / 8.0F)) * 0.46F : 0.0F;
            }
        }
    }

    public static float getShaderScreenMaskIntensity(float tickProgress) {
        if (shouldHardCutFallbackTerrain() && !cameraReleased) {
            float frameTick = ticks + tickProgress;
            int fadeOutStart = getFallbackTerrainFadeOutStartTick();
            int fadeOutEnd = getFallbackTerrainFadeOutEndTick();
            int fadeInStart = getFallbackTerrainFadeInStartTick();
            int fadeInEnd = getFallbackTerrainFadeInEndTick();
            float maxIntensity = 1.0F;
            if (shouldUseShaderScreenMaskOnly()) {
                return frameTick >= fadeOutStart && frameTick < fadeInEnd ? maxIntensity : 0.0F;
            } else if (frameTick >= fadeOutEnd && frameTick < fadeInStart) {
                return maxIntensity;
            } else if (frameTick >= fadeOutStart && frameTick < fadeOutEnd) {
                return smoothStep((frameTick - fadeOutStart) / 10.0F) * maxIntensity;
            } else {
                return frameTick >= fadeInStart && frameTick < fadeInEnd ? (1.0F - smoothStep((frameTick - fadeInStart) / 10.0F)) * maxIntensity : 0.0F;
            }
        } else {
            return 0.0F;
        }
    }

    private static String normalizeTargetDimensionId(String targetDimensionId) {
        return DimensionIds.normalize(targetDimensionId);
    }

    private static boolean shouldSkipTravelForTargetDimension(String targetDimensionId) {
        return targetDimensionId != null
            && startDimension != null
            && !TeleportAnimationConfig.isCrossDimensionTravelEnabled()
            && !targetDimensionId.equals(DimensionIds.fromResourceKey(startDimension));
    }

    private static boolean hasArrivedAtTeleportTarget(Minecraft client, Vec3 playerFeet) {
        return skipTravel && plannedTargetDimensionId != null && client.level != null
            ? plannedTargetDimensionId.equals(DimensionIds.fromResourceKey(client.level.dimension()))
            : isNearPlannedTarget(playerFeet) || hasTeleportedSinceLastObservation(playerFeet);
    }

    private static boolean isNearPlannedTarget(Vec3 playerFeet) {
        if (plannedTargetFeetStable && plannedTargetFeet != null) {
            double dx = playerFeet.x - plannedTargetFeet.x;
            double dz = playerFeet.z - plannedTargetFeet.z;
            double dy = Math.abs(playerFeet.y - plannedTargetFeet.y);
            return dx * dx + dz * dz <= 16.0 && dy <= 8.0;
        } else {
            return false;
        }
    }

    private static boolean hasTeleportedSinceLastObservation(Vec3 playerFeet) {
        return lastObservedPlayerFeet != null && playerFeet.distanceToSqr(lastObservedPlayerFeet) > 9.0;
    }

    private static int calculateTravelTicks(Vec3 fromFeet, Vec3 toFeet) {
        if (fromFeet != null && toFeet != null) {
            double dx = toFeet.x - fromFeet.x;
            double dz = toFeet.z - fromFeet.z;
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            int scaledTicks = Mth.ceil(horizontalDistance / 30.0);
            return Mth.clamp(scaledTicks, 20, 60);
        } else {
            return 40;
        }
    }

    private static void recordActualTargetFeet(Vec3 playerFeet) {
        actualTargetFeet = playerFeet;
        if (!skipTravel && !plannedTargetFeetStable && ticks <= getTravelStartTick()) {
            travelTicks = calculateTravelTicks(startFeet, actualTargetFeet);
            totalTicks = getFixedTotalTicks() + travelTicks;
        }

        if (skipTravel || ticks < getTravelEndTick() || arrivalCameraFeet == null) {
            arrivalCameraFeet = playerFeet;
        }
    }

    private static TeleportTransitionController.CameraFrame exitBodyFrame(float progress) {
        float heightProgress = smoothStep(progress);
        float lookProgress = bodyLookProgress(progress);
        Vec3 source = startEye == null ? startFeet.add(0.0, 1.6, 0.0) : startEye;
        Vec3 target = startFeet.add(0.0, getBodyCameraHeight(), 0.0);
        return new TeleportTransitionController.CameraFrame(source.lerp(target, heightProgress), startYaw, lerpFloat(startPitch, 90.0F, lookProgress));
    }

    private static TeleportTransitionController.CameraFrame startBodyHoldFrame(float frameTick) {
        float progress = (frameTick - 12.0F) / getBodyHoldTicks();
        double altitude = getBodyCameraHeight() + glideProgress(progress) * getBodyGlideHeight();
        return bodyHoldFrame(startFeet, startYaw, altitude);
    }

    private static TeleportTransitionController.CameraFrame enterBodyHoldFrame(float frameTick) {
        float progress = (frameTick - getEnterHoldStartTick()) / getBodyHoldTicks();
        double altitude = getBodyCameraHeight() + (1.0 - glideProgress(progress)) * getBodyGlideHeight();
        return bodyHoldFrame(getArrivalCameraFeet(), startYaw, altitude);
    }

    private static TeleportTransitionController.CameraFrame bodyHoldFrame(Vec3 feet, float yaw, double altitude) {
        return new TeleportTransitionController.CameraFrame(feet.add(0.0, altitude, 0.0), yaw, 90.0F);
    }

    private static TeleportTransitionController.CameraFrame pullFrame(float progress, float frameTick) {
        Vec3 pos = startFeet.add(0.0, pullAltitude(frameTick), 0.0);
        return applyZoomShake(pos, startYaw, 90.0F, frameTick, 1.0F);
    }

    private static TeleportTransitionController.CameraFrame topDownFrame(Vec3 feet, float yaw, float frameTick, double altitude) {
        Vec3 pos = feet.add(0.0, altitude, 0.0);
        return applyZoomShake(pos, yaw, 90.0F, frameTick, 1.0F);
    }

    private static TeleportTransitionController.CameraFrame prePushTopDownFrame(Vec3 feet, float yaw, float frameTick) {
        return topDownFrame(feet, yaw, frameTick, getTargetTravelAltitude());
    }

    private static TeleportTransitionController.CameraFrame travelFrame(float progress, float frameTick) {
        Vec3 source = startFeet.add(0.0, getStartTravelAltitude(), 0.0);
        Vec3 target = getTravelTargetFeet().add(0.0, getTargetTravelAltitude(), 0.0);
        Vec3 pos = source.lerp(target, travelEaseProgress(progress));
        return applyZoomShake(pos, startYaw, 90.0F, frameTick, 1.0F);
    }

    private static float travelEaseProgress(float progress) {
        double total = Math.max(1.0, travelTicks);
        double edge = Math.min(10.0, total * 0.5);
        double elapsed = Mth.clamp(progress, 0.0F, 1.0F) * total;
        double weightedTotal = Math.max(1.0, total - edge);
        if (elapsed < edge) {
            return (float)(edge * integratedSmoothStep(elapsed / edge) / weightedTotal);
        }

        double steadyEnd = total - edge;
        if (elapsed <= steadyEnd) {
            return (float)((edge * 0.5 + elapsed - edge) / weightedTotal);
        }

        double decelElapsed = elapsed - steadyEnd;
        double beforeDecel = edge * 0.5 + Math.max(0.0, total - edge * 2.0);
        return (float)((beforeDecel + edge * integratedSmoothStepDown(decelElapsed / edge)) / weightedTotal);
    }

    private static double integratedSmoothStep(double value) {
        double x = Mth.clamp(value, 0.0, 1.0);
        return x * x * x - 0.5 * x * x * x * x;
    }

    private static double integratedSmoothStepDown(double value) {
        double x = Mth.clamp(value, 0.0, 1.0);
        return x - integratedSmoothStep(x);
    }

    private static TeleportTransitionController.CameraFrame pushFrame(LocalPlayer player, float progress, float frameTick) {
        Vec3 pos = getArrivalCameraFeet().add(0.0, pushAltitude(frameTick), 0.0);
        return applyZoomShake(pos, startYaw, 90.0F, frameTick, 1.0F);
    }

    private static TeleportTransitionController.CameraFrame enterBodyFrame(LocalPlayer player, float progress) {
        captureEnterBodyTarget(player);
        float heightProgress = smoothStep(progress);
        float lookProgress = bodyLookProgress(progress);
        Vec3 source = getArrivalCameraFeet().add(0.0, getBodyCameraHeight(), 0.0);
        Vec3 target = enterBodyTargetEye == null ? player.getEyePosition() : enterBodyTargetEye;
        float yaw = lerpDegrees(startYaw, enterBodyTargetYaw, lookProgress);
        float pitch = lerpFloat(90.0F, enterBodyTargetPitch, lookProgress);
        return new TeleportTransitionController.CameraFrame(source.lerp(target, heightProgress), yaw, pitch);
    }

    private static void captureEnterBodyTarget(LocalPlayer player) {
        if (enterBodyTargetEye == null) {
            enterBodyTargetEye = player.getEyePosition();
            enterBodyTargetYaw = player.getYRot();
            enterBodyTargetPitch = player.getXRot();
        }
    }

    private static Vec3 getBestTargetFeet() {
        if (actualTargetFeet != null) {
            return actualTargetFeet;
        } else {
            return plannedTargetFeetStable && plannedTargetFeet != null ? plannedTargetFeet : startFeet;
        }
    }

    private static Vec3 getArrivalCameraFeet() {
        return arrivalCameraFeet != null ? arrivalCameraFeet : getBestTargetFeet();
    }

    private static Vec3 getTravelTargetFeet() {
        return getArrivalCameraFeet();
    }

    private static TeleportTransitionController.CameraFrame applyZoomShake(Vec3 pos, float yaw, float pitch, float frameTick, float envelope) {
        double easedEnvelope = Mth.clamp(envelope, 0.0F, 1.0F);
        double verticalWave = Math.sin(frameTick * 0.105) * 0.74 + Math.sin(frameTick * 0.052 + 1.6) * 0.26;
        double horizontalWave = Math.sin(frameTick * 0.105 + 2.1) * 0.78 + Math.sin(frameTick * 0.15 + 0.35) * 0.22;
        double pitchShake = (Mth.clamp(verticalWave, -1.0, 1.0) - 1.0) * 0.5 * 1.05 * easedEnvelope;
        double yawShake = horizontalWave * 0.15 * easedEnvelope;
        float shakenYaw = yaw + (float)yawShake;
        float shakenPitch = Mth.clamp(pitch + (float)pitchShake, -90.0F, 90.0F);
        return new TeleportTransitionController.CameraFrame(pos, shakenYaw, shakenPitch);
    }

    private static double getStartTravelAltitude() {
        return getZoomOutStageAltitude(2) + getZoomStageGlideHeight();
    }

    private static double getTargetTravelAltitude() {
        return getZoomInStageAltitude(2) + getZoomStageGlideHeight();
    }

    private static double getZoomOutStageAltitude(int index) {
        return TeleportAnimationConfig.getZoomOutStageHeights(getStartZoomDimension())[index];
    }

    private static double getZoomInStageAltitude(int index) {
        return TeleportAnimationConfig.getZoomInStageHeights(getCurrentZoomDimension())[index];
    }

    private static TeleportAnimationConfig.ZoomDimension getStartZoomDimension() {
        return TeleportAnimationConfig.ZoomDimension.fromLevel(startDimension);
    }

    private static TeleportAnimationConfig.ZoomDimension getCurrentZoomDimension() {
        Minecraft client = Minecraft.getInstance();
        ResourceKey<Level> dimension = client.level == null ? startDimension : client.level.dimension();
        return TeleportAnimationConfig.ZoomDimension.fromLevel(dimension);
    }

    private static double pullAltitude(float frameTick) {
        int firstStage = getPullStageTick(0);
        int secondStage = getPullStageTick(1);
        int thirdStage = getPullStageTick(2);
        int pullEnd = getPullMotionEndTick();
        if (frameTick < secondStage) {
            return departingAltitude(getZoomOutStageAltitude(0), frameTick, firstStage, secondStage);
        } else {
            return frameTick < thirdStage
                ? departingAltitude(getZoomOutStageAltitude(1), frameTick, secondStage, thirdStage)
                : departingFinalAltitude(getZoomOutStageAltitude(2), frameTick, thirdStage, pullEnd);
        }
    }

    private static double pushAltitude(float frameTick) {
        int pushMotionStart = getPushMotionStartTick();
        int firstStage = getPushStageTick(1);
        int secondStage = getPushStageTick(2);
        int pushEnd = getEnterHoldStartTick();
        if (frameTick < firstStage) {
            return arrivingAltitude(getZoomInStageAltitude(2), frameTick, pushMotionStart, pushMotionStart + getZoomInStageTickLength(0));
        } else {
            return frameTick < secondStage
                ? arrivingAltitude(getZoomInStageAltitude(1), frameTick, firstStage, secondStage)
                : arrivingAltitude(getZoomInStageAltitude(0), frameTick, secondStage, pushEnd);
        }
    }

    private static double departingAltitude(double altitude, float frameTick, int startTick, int endTick) {
        return altitude + zoomStageGlideProgressBetween(frameTick, startTick, endTick) * getZoomStageGlideHeight();
    }

    private static double arrivingAltitude(double altitude, float frameTick, int startTick, int endTick) {
        return altitude + (1.0 - zoomStageGlideProgressBetween(frameTick, startTick, endTick)) * getZoomStageGlideHeight();
    }

    private static double departingFinalAltitude(double altitude, float frameTick, int startTick, int endTick) {
        return altitude + finalZoomOutGlideProgressBetween(frameTick, startTick, endTick) * getZoomStageGlideHeight();
    }

    private static double zoomStageGlideProgressBetween(float frameTick, int startTick, int endTick) {
        float progress = (frameTick - startTick) / Math.max(1.0F, endTick - startTick);
        return linearThenEaseOut(progress);
    }

    private static double finalZoomOutGlideProgressBetween(float frameTick, int startTick, int endTick) {
        float progress = (frameTick - startTick) / Math.max(1.0F, endTick - startTick);
        return longEaseOut(progress);
    }

    private static double linearThenEaseOut(float value) {
        double x = Mth.clamp(value, 0.0F, 1.0F);
        return hermiteEaseOutFromLinear(x, 0.64, 1.08);
    }

    private static double longEaseOut(float value) {
        double x = Mth.clamp(value, 0.0F, 1.0F);
        return hermiteEaseOutFromLinear(x, 0.36, 1.0);
    }

    private static double hermiteEaseOutFromLinear(double x, double decelStart, double linearSpeed) {
        if (x <= decelStart) {
            return x * linearSpeed;
        }

        double t = (x - decelStart) / (1.0 - decelStart);
        double t2 = t * t;
        double t3 = t2 * t;
        double y0 = decelStart * linearSpeed;
        double m0 = linearSpeed * (1.0 - decelStart);
        double h00 = 2.0 * t3 - 3.0 * t2 + 1.0;
        double h10 = t3 - 2.0 * t2 + t;
        double h01 = -2.0 * t3 + 3.0 * t2;
        return Mth.clamp(h00 * y0 + h10 * m0 + h01, 0.0, 1.0);
    }

    private static double glideProgress(float value) {
        return Mth.clamp(value, 0.0F, 1.0F);
    }

    private static float pulseAfter(float tick, float startTick) {
        float age = tick - startTick;
        if (!(age < 0.0F) && !(age > 7.0F)) {
            float x = age / 7.0F;
            return (1.0F - x) * (1.0F - x);
        } else {
            return 0.0F;
        }
    }

    private static float smoothStep(float value) {
        float x = Mth.clamp(value, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    private static float lerpFloat(float from, float to, float progress) {
        float x = Mth.clamp(progress, 0.0F, 1.0F);
        return from + (to - from) * x;
    }

    private static float bodyLookProgress(float progress) {
        return easeOutCubic(Mth.clamp(progress * 1.5F, 0.0F, 1.0F));
    }

    private static float easeOutCubic(float progress) {
        float x = Mth.clamp(progress, 0.0F, 1.0F);
        float inverse = 1.0F - x;
        return 1.0F - inverse * inverse * inverse;
    }

    private static float lerpDegrees(float from, float to, float progress) {
        return from + wrapDegrees(to - from) * Mth.clamp(progress, 0.0F, 1.0F);
    }

    private static float wrapDegrees(float value) {
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }

        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }

        return wrapped;
    }

    private static boolean isCameraStepSoundTick(int tick) {
        int firstPullStep = getPullStageTick(0);
        int secondPullStep = getPullStageTick(1);
        int thirdPullStep = getPullStageTick(2);
        int firstPushStep = getPushStageTick(1);
        int secondPushStep = getPushStageTick(2);
        int thirdPushStep = getEnterHoldStartTick();
        return tick == firstPullStep
            || tick == secondPullStep
            || tick == thirdPullStep
            || tick == firstPushStep
            || tick == secondPushStep
            || tick == thirdPushStep;
    }

    private static int getPullStageTick(int stageIndex) {
        int[] stageTicks = TeleportAnimationConfig.getZoomOutStageTicks();
        int offset = 0;

        for (int i = 0; i < stageIndex && i < stageTicks.length; i++) {
            offset += stageTicks[i];
        }

        return getPullStartTick() + offset;
    }

    private static int getPushStageTick(int stageIndex) {
        int[] stageTicks = TeleportAnimationConfig.getZoomInStageTicks();
        int offset = 0;

        for (int i = 0; i < stageIndex && i < stageTicks.length; i++) {
            offset += stageTicks[i];
        }

        return getPushStartTick() + offset;
    }

    private static int getZoomInStageTickLength(int stageIndex) {
        int[] stageTicks = TeleportAnimationConfig.getZoomInStageTicks();
        return stageIndex >= 0 && stageIndex < stageTicks.length ? Math.max(1, stageTicks[stageIndex]) : 1;
    }

    private static void playStepSound(Minecraft client, int tick) {
        if (isCameraStepSoundTick(tick)) {
            if (TeleportAnimationConfig.isCustomSoundsEnabled()) {
                playUiSound(client, getCustomStepSound(tick), (float)TeleportAnimationConfig.getCustomSoundVolume(), 1.0F);
            } else {
                playUiSound(client, TeleportAnimationConfig.getStepSound(), minecraftSoundVolume(0.86F), 1.0F);
            }
        }
    }

    private static SoundEvent getCustomStepSound(int tick) {
        if (tick == getPullStageTick(2)) {
            return TeleportSounds.ZOOM_OUT_LONG;
        } else if (tick == getPushStageTick(1) || tick == getPushStageTick(2)) {
            return TeleportSounds.ZOOM_IN_SHORT;
        } else {
            return tick == getEnterHoldStartTick() ? TeleportSounds.ZOOM_IN_LONG : TeleportSounds.ZOOM_OUT_SHORT;
        }
    }

    private static void playTravelSound(Minecraft client) {
        if (TeleportAnimationConfig.isCustomSoundsEnabled()) {
            activeTravelSound = new TeleportTransitionController.FadingTravelSound((float)TeleportAnimationConfig.getCustomSoundVolume());
            client.getSoundManager().play(activeTravelSound);
        } else {
            playUiSound(client, TeleportAnimationConfig.getTravelSound(), minecraftSoundVolume(0.48F), 1.0F);
        }
    }

    private static void playBodyTransitionSound(Minecraft client, boolean enteringPlayer) {
        if (TeleportAnimationConfig.isCustomSoundsEnabled()) {
            playUiSound(
                client, enteringPlayer ? TeleportSounds.CAMERA_IN : TeleportSounds.CAMERA_OUT, (float)TeleportAnimationConfig.getCustomSoundVolume(), 1.0F
            );
        } else {
            for (int i = 0; i < 3; i++) {
                playUiSound(client, TeleportAnimationConfig.getCameraSound(enteringPlayer), minecraftSoundVolume(1.0F), 0.8F);
            }
        }
    }

    private static float minecraftSoundVolume(float baseVolume) {
        return baseVolume * (float)TeleportAnimationConfig.getMinecraftSoundVolume();
    }

    private static void playUiSound(Minecraft client, SoundEvent sound, float volume, float pitch) {
        client.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private static void fadeCustomTravelSound() {
        if (activeTravelSound != null) {
            activeTravelSound.fadeOut(30);
        }
    }

    private static void stopCustomTravelSound(Minecraft client) {
        if (activeTravelSound != null) {
            client.getSoundManager().stop(activeTravelSound);
            activeTravelSound = null;
        }
    }

    private static float cubicBezierEase(float progress) {
        float x = Mth.clamp(progress, 0.0F, 1.0F);
        float t = x;

        for (int i = 0; i < 5; i++) {
            float currentX = cubicBezier(t, 0.55F, 0.55F);
            float derivative = cubicBezierDerivative(t, 0.55F, 0.55F);
            if (Math.abs(derivative) < 1.0E-4F) {
                break;
            }

            t = Mth.clamp(t - (currentX - x) / derivative, 0.0F, 1.0F);
        }

        return cubicBezier(t, 0.0F, 1.0F);
    }

    private static float cubicBezier(float t, float control1, float control2) {
        float oneMinusT = 1.0F - t;
        return 3.0F * oneMinusT * oneMinusT * t * control1 + 3.0F * oneMinusT * t * t * control2 + t * t * t;
    }

    private static float cubicBezierDerivative(float t, float control1, float control2) {
        float oneMinusT = 1.0F - t;
        return 3.0F * oneMinusT * oneMinusT * control1 + 6.0F * oneMinusT * t * (control2 - control1) + 3.0F * t * t * (1.0F - control2);
    }

    private static Vec3 getFeetPos(LocalPlayer player) {
        return new Vec3(player.getX(), player.getY(), player.getZ());
    }

    private static int getCommandSendTick() {
        if (skipTravel) {
            return getTravelStartTick();
        } else {
            return plannedTargetFeetStable && plannedTargetFeet != null ? getTravelStartTick() + travelTicks / 2 : getPullEndTick();
        }
    }

    private static boolean isFallbackTerrainMode() {
        return isRunning() && !skipTravel && TeleportAnimationConfig.isFallbackChunkFadeEnabled() && !isDistantTerrainAvailable();
    }

    private static boolean isDistantTerrainAvailable() {
        return isVoxyTerrainAvailable() || isDistantHorizonsTerrainAvailable() || isBobbyTerrainAvailable();
    }

    private static boolean isVoxyTerrainAvailable() {
        return VOXY_LOADED && VoxyCompat.isRenderingEnabled();
    }

    private static boolean isDistantHorizonsTerrainAvailable() {
        return DISTANT_HORIZONS_LOADED && DistantHorizonsCompat.isRenderingEnabled();
    }

    private static boolean isBobbyTerrainAvailable() {
        return BOBBY_LOADED && BobbyCompat.isRenderingEnabled();
    }

    private static int getFallbackTerrainHideTick() {
        return getFallbackTerrainFadeOutEndTick();
    }

    private static int getFallbackTerrainShowTick() {
        return getFallbackTerrainFadeInStartTick();
    }

    private static boolean isFallbackTerrainFadeOutActive() {
        return ticks >= getFallbackTerrainFadeOutStartTick() && ticks < getFallbackTerrainFadeOutEndTick();
    }

    private static int getBodyHoldTicks() {
        return TeleportAnimationConfig.getBodyGlideTicks();
    }

    private static int getPullTicks() {
        int total = 0;

        for (int stageTick : TeleportAnimationConfig.getZoomOutStageTicks()) {
            total += stageTick;
        }

        return Math.max(1, total);
    }

    private static int getPushTicks() {
        int total = 0;

        for (int stageTick : TeleportAnimationConfig.getZoomInStageTicks()) {
            total += stageTick;
        }

        return Math.max(1, total);
    }

    private static int getFixedTotalTicks() {
        return 12 + getBodyHoldTicks() + getPullTicks() + getPreTravelWaitTicks() + getPrePushWaitTicks() + getPushTicks() + getBodyHoldTicks() + 12;
    }

    private static double getBodyCameraHeight() {
        return TeleportAnimationConfig.getBodyCameraHeight();
    }

    private static double getBodyGlideHeight() {
        return TeleportAnimationConfig.getBodyGlideHeight();
    }

    private static double getZoomStageGlideHeight() {
        return TeleportAnimationConfig.getZoomStageGlideHeight();
    }

    private static int getPullStartTick() {
        return 12 + getBodyHoldTicks();
    }

    private static int getPullEndTick() {
        return getPullStartTick() + getPullTicks();
    }

    private static int getPreTravelWaitTicks() {
        return skipTravel ? 20 : 10;
    }

    private static int getPrePushWaitTicks() {
        return skipTravel ? 20 : 10;
    }

    private static int getTravelStartTick() {
        return getPullEndTick() + getPreTravelWaitTicks();
    }

    private static int getTravelEndTick() {
        return getTravelStartTick() + travelTicks;
    }

    private static int getPushStartTick() {
        return getTravelEndTick() + getPrePushWaitTicks() + (skipTravel ? skipTravelArrivalDelayTicks : 0);
    }

    private static int getPullMotionEndTick() {
        return skipTravel ? getPullEndTick() : getTravelStartTick();
    }

    private static int getPushMotionStartTick() {
        return skipTravel ? getPushStartTick() : getTravelEndTick();
    }

    private static int getEnterHoldStartTick() {
        return getPushStartTick() + getPushTicks();
    }

    private static int getEnterStartTick() {
        return getEnterHoldStartTick() + getBodyHoldTicks();
    }

    private static int getFallbackTerrainFadeOutStartTick() {
        return getTravelStartTick() + 4;
    }

    private static int getFallbackTerrainFadeOutEndTick() {
        return getFallbackTerrainFadeOutStartTick() + 10;
    }

    private static int getFallbackTerrainFadeInStartTick() {
        return getTravelEndTick() - 10;
    }

    private static int getFallbackTerrainFadeInEndTick() {
        return getTravelEndTick();
    }

    private static int getVanillaTerrainFadeOutStartTick() {
        return getPullEndTick();
    }

    private static int getVanillaTerrainFadeOutEndTick() {
        return getPullEndTick() + 10;
    }

    private static int getVanillaTerrainFadeInStartTick() {
        return getTravelEndTick();
    }

    private static int getVanillaTerrainFadeInEndTick() {
        return getVanillaTerrainFadeInStartTick() + 10;
    }

    private static float getDistanceOrderTowardPoint(double sectionCenterX, double sectionCenterZ, Vec3 centerFeet, Vec3 directionFeet) {
        double radius = getFallbackTerrainRenderRadius();
        double centerDistance = horizontalDistance(centerFeet, directionFeet);
        double distance = horizontalDistance(sectionCenterX, sectionCenterZ, directionFeet);
        double min = Math.max(0.0, centerDistance - radius);
        double max = centerDistance + radius;
        return (float)Mth.clamp((distance - min) / Math.max(1.0, max - min), 0.0, 1.0);
    }

    private static float getShaderMaskSweepThreshold(float order) {
        return Mth.clamp(order, 0.0F, 1.0F) * 0.78F;
    }

    private static double horizontalDistance(Vec3 from, Vec3 to) {
        if (from != null && to != null) {
            double dx = from.x - to.x;
            double dz = from.z - to.z;
            return Math.sqrt(dx * dx + dz * dz);
        } else {
            return 0.0;
        }
    }

    private static double horizontalDistance(double x, double z, Vec3 to) {
        if (to == null) {
            return 0.0;
        }

        double dx = x - to.x;
        double dz = z - to.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double getFallbackTerrainRenderRadius() {
        Minecraft client = Minecraft.getInstance();
        int renderDistance = client.options == null ? 12 : client.options.getEffectiveRenderDistance();
        return Math.max(32.0, renderDistance * 16.0);
    }

    private static int getTerrainVisibilityRefreshSectionStep(Minecraft client) {
        int renderDistance = client.options == null ? 12 : client.options.getEffectiveRenderDistance();
        return Math.max(4, renderDistance / 2);
    }

    private static void restoreCameraType(Minecraft client) {
        if (previousCameraType != null) {
            client.options.setCameraType(previousCameraType);
        }
    }

    private static void restoreSmartCull(Minecraft client) {
        if (client.smartCull != previousSmartCull) {
            client.smartCull = previousSmartCull;
            client.levelRenderer.needsUpdate();
        }
    }

    private static void runPendingAction() {
        if (pendingAction != null) {
            pendingAction.run();
        }
    }

    private static void updateHudVisibility(Minecraft client) {
        if (!hudSuppressed && ticks >= 8) {
            client.options.hideGui = true;
            hudSuppressed = true;
        }

        if (hudSuppressed && cameraReleased) {
            client.options.hideGui = previousHudHidden;
            hudSuppressed = false;
        }
    }

    private static void updateHardCutTerrainState(Minecraft client) {
        boolean active = shouldUseHardCutFallbackTerrainCulling();
        if (active != previousHardCutCullingActive) {
            previousHardCutCullingActive = active;
            resetVisibilityRefreshState();
            if (client.levelRenderer != null) {
                client.levelRenderer.needsUpdate();
            }

            SodiumCompat.scheduleTerrainUpdate();
        }
    }

    private static void clear(Minecraft client) {
        restoreCameraType(client);
        restoreSmartCull(client);
        SodiumCompat.endTransition();
        stopCustomTravelSound(client);
        client.options.hideGui = previousHudHidden;
        ticksSinceCleared = 0;
        pendingAction = null;
        startFeet = null;
        startEye = null;
        plannedTargetFeet = null;
        plannedTargetFeetStable = true;
        actualTargetFeet = null;
        arrivalCameraFeet = null;
        lastObservedPlayerFeet = null;
        enterBodyTargetEye = null;
        plannedTargetDimensionId = null;
        startDimension = null;
        previousCameraType = null;
        previousHudHidden = false;
        previousSmartCull = false;
        startYaw = 0.0F;
        startPitch = 0.0F;
        enterBodyTargetYaw = 0.0F;
        enterBodyTargetPitch = 0.0F;
        ticks = 0;
        travelTicks = 40;
        totalTicks = getFixedTotalTicks() + 40;
        commandSent = false;
        cameraReleased = false;
        hudSuppressed = false;
        previousHardCutCullingActive = false;
        skipTravel = false;
        resetVisibilityRefreshState();
    }

    private static void resetVisibilityRefreshState() {
        lastVisibilitySectionX = Integer.MIN_VALUE;
        lastVisibilitySectionY = Integer.MIN_VALUE;
        lastVisibilitySectionZ = Integer.MIN_VALUE;
    }

    public record CameraFrame(Vec3 pos, float yaw, float pitch) {
    }

    private static final class FadingTravelSound extends AbstractTickableSoundInstance {
        private final float baseVolume;
        private int fadeTicks;
        private int fadeTicksRemaining;

        private FadingTravelSound(float volume) {
            super(TeleportSounds.TELEPORT, SoundSource.UI, SoundInstance.createUnseededRandom());
            this.baseVolume = volume;
            this.volume = volume;
            this.pitch = 1.0F;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
        }

        private void fadeOut(int ticks) {
            this.fadeTicks = Math.max(1, ticks);
            this.fadeTicksRemaining = this.fadeTicks;
        }

        @Override
        public void tick() {
            if (this.fadeTicksRemaining > 0) {
                this.volume = this.baseVolume * ((float)this.fadeTicksRemaining / this.fadeTicks);
                this.fadeTicksRemaining--;
                if (this.fadeTicksRemaining <= 0) {
                    this.volume = 0.0F;
                    this.stop();
                    if (TeleportTransitionController.activeTravelSound == this) {
                        TeleportTransitionController.activeTravelSound = null;
                    }
                }
            }
        }
    }
}
