package fr.poubone.att2.client.teleport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;

public final class TeleportAnimationConfig {
    private static final String FILE_NAME = "att2_teleport.properties";
    private static final String EFFECT_ENABLED_KEY = "effectEnabled";
    private static final String PLAYER_FREEZE_ENABLED_KEY = "playerFreezeEnabled";
    private static final String CROSS_DIMENSION_TRAVEL_ENABLED_KEY = "crossDimensionTravelEnabled";
    private static final String FALLBACK_CHUNK_FADE_ENABLED_KEY = "fallbackChunkFadeEnabled";
    private static final String CUSTOM_SOUNDS_ENABLED_KEY = "customSoundsEnabled";
    private static final String MINECRAFT_SOUND_VOLUME_KEY = "minecraftSoundVolume";
    private static final String CUSTOM_SOUND_VOLUME_KEY = "customSoundVolume";
    private static final String ZOOM_OUT_STAGE_KEY_PREFIX = "zoomOutStage";
    private static final String ZOOM_IN_STAGE_KEY_PREFIX = "zoomInStage";
    private static final String NETHER_ZOOM_OUT_STAGE_KEY_PREFIX = "netherZoomOutStage";
    private static final String NETHER_ZOOM_IN_STAGE_KEY_PREFIX = "netherZoomInStage";
    private static final String END_ZOOM_OUT_STAGE_KEY_PREFIX = "endZoomOutStage";
    private static final String END_ZOOM_IN_STAGE_KEY_PREFIX = "endZoomInStage";
    private static final String ZOOM_OUT_STAGE_TICKS_KEY_PREFIX = "zoomOutStageTicks";
    private static final String ZOOM_IN_STAGE_TICKS_KEY_PREFIX = "zoomInStageTicks";
    private static final String ZOOM_STAGE_GLIDE_HEIGHT_KEY = "zoomStageGlideHeight";
    private static final String ZOOM_STAGE_GLIDE_TICKS_KEY = "zoomStageGlideTicks";
    private static final String BODY_CAMERA_HEIGHT_KEY = "bodyCameraHeight";
    private static final String BODY_GLIDE_HEIGHT_KEY = "bodyGlideHeight";
    private static final String BODY_GLIDE_TICKS_KEY = "bodyGlideTicks";
    private static final String LOCAL_PLAYER_HIDE_TICKS_KEY = "localPlayerHideTicks";
    private static final String SAVE_COMMENT = "ATT2 teleport animation settings (Grand Teleport transition around the map's Teleportation Arrays).\nSounds: minecraftSound* accept any sound event id, e.g. vanilla ids or the map resource pack's own\nsounds (minecraft:swooshing, minecraft:timewarp1, minecraft:teleportation, minecraft:magicspell, minecraft:wind).\ncustomSoundsEnabled=true switches to the att2:teleport.* events: provide the .ogg files through a resource pack\n(assets/att2/sounds.json + assets/att2/sounds/teleport/*.ogg, see README).";
    private static final String SOUND_CAMERA_OUT_KEY = "minecraftSoundCameraOut";
    private static final String SOUND_CAMERA_IN_KEY = "minecraftSoundCameraIn";
    private static final String SOUND_STEP_KEY = "minecraftSoundStep";
    private static final String SOUND_TRAVEL_KEY = "minecraftSoundTravel";
    private static final String DEFAULT_SOUND_CAMERA = "minecraft:ui.toast.out";
    private static final String DEFAULT_SOUND_STEP = "minecraft:block.respawn_anchor.charge";
    private static final String DEFAULT_SOUND_TRAVEL = "minecraft:block.portal.trigger";
    private static final String DEFAULT_CONFIG_PROPERTIES = "effectEnabled=true\nplayerFreezeEnabled=true\ncrossDimensionTravelEnabled=false\nfallbackChunkFadeEnabled=true\ncustomSoundsEnabled=false\nminecraftSoundVolume=0.5\ncustomSoundVolume=0.5\nzoomOutStage1=20\nzoomOutStage2=40\nzoomOutStage3=60\nzoomInStage1=20\nzoomInStage2=40\nzoomInStage3=60\nnetherZoomOutStage1=20\nnetherZoomOutStage2=40\nnetherZoomOutStage3=60\nnetherZoomInStage1=20\nnetherZoomInStage2=40\nnetherZoomInStage3=60\nendZoomOutStage1=20\nendZoomOutStage2=40\nendZoomOutStage3=60\nendZoomInStage1=20\nendZoomInStage2=40\nendZoomInStage3=60\nzoomOutStageTicks1=13\nzoomOutStageTicks2=13\nzoomOutStageTicks3=13\nzoomInStageTicks1=13\nzoomInStageTicks2=13\nzoomInStageTicks3=13\nzoomStageGlideHeight=0.5\nzoomStageGlideTicks=13\nbodyCameraHeight=6.0\nbodyGlideHeight=0.5\nbodyGlideTicks=10\nlocalPlayerHideTicks=2\n";
    private static final double[] DEFAULT_STAGE_HEIGHTS = new double[]{20.0, 40.0, 60.0};
    private static final double MIN_STAGE_HEIGHT = 8.0;
    private static final double MAX_STAGE_HEIGHT = 512.0;
    private static final double MIN_STAGE_GAP = 1.0;
    private static final int[] DEFAULT_STAGE_TICKS = new int[]{13, 13, 13};
    private static final int MIN_STAGE_TICKS = 1;
    private static final int MAX_STAGE_TICKS = 200;
    private static final double DEFAULT_ZOOM_STAGE_GLIDE_HEIGHT = 0.5;
    private static final double MIN_ZOOM_STAGE_GLIDE_HEIGHT = 0.1;
    private static final double MAX_ZOOM_STAGE_GLIDE_HEIGHT = 5.0;
    private static final int DEFAULT_ZOOM_STAGE_GLIDE_TICKS = 13;
    private static final double DEFAULT_BODY_CAMERA_HEIGHT = 6.0;
    private static final double MIN_BODY_CAMERA_HEIGHT = 0.1;
    private static final double MAX_BODY_CAMERA_HEIGHT = 10.0;
    private static final double DEFAULT_BODY_GLIDE_HEIGHT = 0.5;
    private static final double MIN_BODY_GLIDE_HEIGHT = 0.1;
    private static final double MAX_BODY_GLIDE_HEIGHT = 5.0;
    private static final int DEFAULT_BODY_GLIDE_TICKS = 10;
    private static final int MIN_LOCAL_PLAYER_HIDE_TICKS = 0;
    private static final int MAX_LOCAL_PLAYER_HIDE_TICKS = 20;
    private static final int DEFAULT_LOCAL_PLAYER_HIDE_TICKS = 2;
    private static final double DEFAULT_MINECRAFT_SOUND_VOLUME = 0.5;
    private static final double DEFAULT_CUSTOM_SOUND_VOLUME = 0.5;
    private static final double MIN_SOUND_VOLUME = 0.1;
    private static final double MAX_SOUND_VOLUME = 1.0;
    private static Path configPath;
    private static boolean effectEnabled = true;
    private static boolean playerFreezeEnabled = true;
    private static boolean crossDimensionTravelEnabled;
    private static boolean fallbackChunkFadeEnabled = true;
    private static boolean customSoundsEnabled;
    private static double minecraftSoundVolume = 0.5;
    private static double customSoundVolume = 0.5;
    private static double[] zoomOutStageHeights = (double[])DEFAULT_STAGE_HEIGHTS.clone();
    private static double[] zoomInStageHeights = (double[])DEFAULT_STAGE_HEIGHTS.clone();
    private static double[] netherZoomOutStageHeights = (double[])DEFAULT_STAGE_HEIGHTS.clone();
    private static double[] netherZoomInStageHeights = (double[])DEFAULT_STAGE_HEIGHTS.clone();
    private static double[] endZoomOutStageHeights = (double[])DEFAULT_STAGE_HEIGHTS.clone();
    private static double[] endZoomInStageHeights = (double[])DEFAULT_STAGE_HEIGHTS.clone();
    private static int[] zoomOutStageTicks = (int[])DEFAULT_STAGE_TICKS.clone();
    private static int[] zoomInStageTicks = (int[])DEFAULT_STAGE_TICKS.clone();
    private static double zoomStageGlideHeight = 0.5;
    private static int zoomStageGlideTicks = 13;
    private static double bodyCameraHeight = 6.0;
    private static double bodyGlideHeight = 0.5;
    private static int bodyGlideTicks = 10;
    private static int localPlayerHideTicks = 2;
    private static String soundCameraOut = "minecraft:ui.toast.out";
    private static String soundCameraIn = "minecraft:ui.toast.out";
    private static String soundStep = "minecraft:block.respawn_anchor.charge";
    private static String soundTravel = "minecraft:block.portal.trigger";

    private TeleportAnimationConfig() {
    }

    public static void load() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("att2_teleport.properties");
        resetToDefaults();
        if (!Files.exists(configPath)) {
            save();
        } else {
            Properties properties = new Properties();

            try (InputStream input = Files.newInputStream(configPath)) {
                properties.load(input);
                applyConfigProperties(properties);
            } catch (IOException ignored) {
                resetToDefaults();
            }
        }
    }

    public static boolean save() {
        if (configPath == null) {
            configPath = FabricLoader.getInstance().getConfigDir().resolve("att2_teleport.properties");
        }

        Properties properties = new Properties();
        properties.setProperty("effectEnabled", Boolean.toString(effectEnabled));
        properties.setProperty("playerFreezeEnabled", Boolean.toString(playerFreezeEnabled));
        properties.setProperty("crossDimensionTravelEnabled", Boolean.toString(crossDimensionTravelEnabled));
        properties.setProperty("fallbackChunkFadeEnabled", Boolean.toString(fallbackChunkFadeEnabled));
        properties.setProperty("customSoundsEnabled", Boolean.toString(customSoundsEnabled));
        properties.setProperty("minecraftSoundVolume", Double.toString(minecraftSoundVolume));
        properties.setProperty("customSoundVolume", Double.toString(customSoundVolume));
        writeStageHeights(properties, "zoomOutStage", zoomOutStageHeights);
        writeStageHeights(properties, "zoomInStage", zoomInStageHeights);
        writeStageHeights(properties, "netherZoomOutStage", netherZoomOutStageHeights);
        writeStageHeights(properties, "netherZoomInStage", netherZoomInStageHeights);
        writeStageHeights(properties, "endZoomOutStage", endZoomOutStageHeights);
        writeStageHeights(properties, "endZoomInStage", endZoomInStageHeights);
        writeStageTicks(properties, "zoomOutStageTicks", zoomOutStageTicks);
        writeStageTicks(properties, "zoomInStageTicks", zoomInStageTicks);
        properties.setProperty("zoomStageGlideHeight", Double.toString(zoomStageGlideHeight));
        properties.setProperty("zoomStageGlideTicks", Integer.toString(zoomStageGlideTicks));
        properties.setProperty("bodyCameraHeight", Double.toString(bodyCameraHeight));
        properties.setProperty("bodyGlideHeight", Double.toString(bodyGlideHeight));
        properties.setProperty("bodyGlideTicks", Integer.toString(bodyGlideTicks));
        properties.setProperty("localPlayerHideTicks", Integer.toString(localPlayerHideTicks));
        properties.setProperty("minecraftSoundCameraOut", soundCameraOut);
        properties.setProperty("minecraftSoundCameraIn", soundCameraIn);
        properties.setProperty("minecraftSoundStep", soundStep);
        properties.setProperty("minecraftSoundTravel", soundTravel);

        try {
            Files.createDirectories(configPath.getParent());

            try (OutputStream output = Files.newOutputStream(configPath)) {
                properties.store(
                    output,
                    "ATT2 teleport animation settings (Grand Teleport transition around the map's Teleportation Arrays).\nSounds: minecraftSound* accept any sound event id, e.g. vanilla ids or the map resource pack's own\nsounds (minecraft:swooshing, minecraft:timewarp1, minecraft:teleportation, minecraft:magicspell, minecraft:wind).\ncustomSoundsEnabled=true switches to the att2:teleport.* events: provide the .ogg files through a resource pack\n(assets/att2/sounds.json + assets/att2/sounds/teleport/*.ogg, see README)."
                );
            }

            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public static boolean isEffectEnabled() {
        return effectEnabled;
    }

    public static boolean setEffectEnabled(boolean enabled) {
        effectEnabled = enabled;
        return save();
    }

    public static boolean isPlayerFreezeEnabled() {
        return playerFreezeEnabled;
    }

    public static boolean setPlayerFreezeEnabled(boolean enabled) {
        playerFreezeEnabled = enabled;
        return save();
    }

    public static boolean isCrossDimensionTravelEnabled() {
        return crossDimensionTravelEnabled;
    }

    public static boolean isFallbackChunkFadeEnabled() {
        return fallbackChunkFadeEnabled;
    }

    public static boolean isCustomSoundsEnabled() {
        return customSoundsEnabled;
    }

    public static double getMinecraftSoundVolume() {
        return minecraftSoundVolume;
    }

    public static double getCustomSoundVolume() {
        return customSoundVolume;
    }

    public static double[] getZoomOutStageHeights() {
        return getZoomOutStageHeights(TeleportAnimationConfig.ZoomDimension.OVERWORLD);
    }

    public static double[] getZoomOutStageHeights(TeleportAnimationConfig.ZoomDimension dimension) {
        return switch (sanitizeZoomDimension(dimension)) {
            case NETHER -> (double[])netherZoomOutStageHeights.clone();
            case END -> (double[])endZoomOutStageHeights.clone();
            default -> (double[])zoomOutStageHeights.clone();
        };
    }

    public static double[] getZoomInStageHeights() {
        return getZoomInStageHeights(TeleportAnimationConfig.ZoomDimension.OVERWORLD);
    }

    public static double[] getZoomInStageHeights(TeleportAnimationConfig.ZoomDimension dimension) {
        return switch (sanitizeZoomDimension(dimension)) {
            case NETHER -> (double[])netherZoomInStageHeights.clone();
            case END -> (double[])endZoomInStageHeights.clone();
            default -> (double[])zoomInStageHeights.clone();
        };
    }

    public static int[] getZoomOutStageTicks() {
        return (int[])zoomOutStageTicks.clone();
    }

    public static int[] getZoomInStageTicks() {
        return (int[])zoomInStageTicks.clone();
    }

    public static double getZoomStageGlideHeight() {
        return zoomStageGlideHeight;
    }

    public static int getZoomStageGlideTicks() {
        return zoomStageGlideTicks;
    }

    public static double getBodyCameraHeight() {
        return bodyCameraHeight;
    }

    public static double getBodyGlideHeight() {
        return bodyGlideHeight;
    }

    public static int getBodyGlideTicks() {
        return bodyGlideTicks;
    }

    public static SoundEvent getCameraSound(boolean entering) {
        return soundEvent(entering ? soundCameraIn : soundCameraOut, "minecraft:ui.toast.out");
    }

    public static SoundEvent getStepSound() {
        return soundEvent(soundStep, "minecraft:block.respawn_anchor.charge");
    }

    public static SoundEvent getTravelSound() {
        return soundEvent(soundTravel, "minecraft:block.portal.trigger");
    }

    private static SoundEvent soundEvent(String id, String fallback) {
        Identifier identifier = Identifier.tryParse(id == null ? "" : id.trim());
        if (identifier == null) {
            identifier = Identifier.parse(fallback);
        }

        return SoundEvent.createVariableRangeEvent(identifier);
    }

    public static int getLocalPlayerHideTicks() {
        return localPlayerHideTicks;
    }

    private static void applyConfigProperties(Properties properties) {
        effectEnabled = readBoolean(properties, "effectEnabled", effectEnabled);
        playerFreezeEnabled = readBoolean(properties, "playerFreezeEnabled", playerFreezeEnabled);
        crossDimensionTravelEnabled = readBoolean(properties, "crossDimensionTravelEnabled", crossDimensionTravelEnabled);
        fallbackChunkFadeEnabled = readBoolean(properties, "fallbackChunkFadeEnabled", fallbackChunkFadeEnabled);
        customSoundsEnabled = readBoolean(properties, "customSoundsEnabled", customSoundsEnabled);
        minecraftSoundVolume = sanitizeSoundVolume(readDouble(properties, "minecraftSoundVolume", minecraftSoundVolume));
        customSoundVolume = sanitizeSoundVolume(readDouble(properties, "customSoundVolume", customSoundVolume));
        zoomOutStageHeights = readStageHeights(properties, "zoomOutStage", DEFAULT_STAGE_HEIGHTS);
        zoomInStageHeights = readStageHeights(properties, "zoomInStage", DEFAULT_STAGE_HEIGHTS);
        netherZoomOutStageHeights = readStageHeights(properties, "netherZoomOutStage", zoomOutStageHeights);
        netherZoomInStageHeights = readStageHeights(properties, "netherZoomInStage", zoomInStageHeights);
        endZoomOutStageHeights = readStageHeights(properties, "endZoomOutStage", zoomOutStageHeights);
        endZoomInStageHeights = readStageHeights(properties, "endZoomInStage", zoomInStageHeights);
        zoomOutStageTicks = readStageTicks(properties, "zoomOutStageTicks", DEFAULT_STAGE_TICKS);
        zoomInStageTicks = readStageTicks(properties, "zoomInStageTicks", DEFAULT_STAGE_TICKS);
        zoomStageGlideHeight = sanitizeZoomStageGlideHeight(readDouble(properties, "zoomStageGlideHeight", zoomStageGlideHeight));
        zoomStageGlideTicks = sanitizeStageTicksValue(readInt(properties, "zoomStageGlideTicks", zoomStageGlideTicks));
        bodyCameraHeight = sanitizeBodyCameraHeight(readDouble(properties, "bodyCameraHeight", bodyCameraHeight));
        bodyGlideHeight = sanitizeBodyGlideHeight(readDouble(properties, "bodyGlideHeight", bodyGlideHeight));
        bodyGlideTicks = sanitizeStageTicksValue(readInt(properties, "bodyGlideTicks", bodyGlideTicks));
        localPlayerHideTicks = sanitizeLocalPlayerHideTicks(readInt(properties, "localPlayerHideTicks", localPlayerHideTicks));
        soundCameraOut = properties.getProperty("minecraftSoundCameraOut", soundCameraOut).trim();
        soundCameraIn = properties.getProperty("minecraftSoundCameraIn", soundCameraIn).trim();
        soundStep = properties.getProperty("minecraftSoundStep", soundStep).trim();
        soundTravel = properties.getProperty("minecraftSoundTravel", soundTravel).trim();
    }

    private static void resetToDefaults() {
        applyConfigProperties(createDefaultProperties());
    }

    private static Properties createDefaultProperties() {
        Properties properties = new Properties();

        try {
            properties.load(
                new StringReader(
                    "effectEnabled=true\nplayerFreezeEnabled=true\ncrossDimensionTravelEnabled=false\nfallbackChunkFadeEnabled=true\ncustomSoundsEnabled=false\nminecraftSoundVolume=0.5\ncustomSoundVolume=0.5\nzoomOutStage1=20\nzoomOutStage2=40\nzoomOutStage3=60\nzoomInStage1=20\nzoomInStage2=40\nzoomInStage3=60\nnetherZoomOutStage1=20\nnetherZoomOutStage2=40\nnetherZoomOutStage3=60\nnetherZoomInStage1=20\nnetherZoomInStage2=40\nnetherZoomInStage3=60\nendZoomOutStage1=20\nendZoomOutStage2=40\nendZoomOutStage3=60\nendZoomInStage1=20\nendZoomInStage2=40\nendZoomInStage3=60\nzoomOutStageTicks1=13\nzoomOutStageTicks2=13\nzoomOutStageTicks3=13\nzoomInStageTicks1=13\nzoomInStageTicks2=13\nzoomInStageTicks3=13\nzoomStageGlideHeight=0.5\nzoomStageGlideTicks=13\nbodyCameraHeight=6.0\nbodyGlideHeight=0.5\nbodyGlideTicks=10\nlocalPlayerHideTicks=2\n"
                )
            );
        } catch (IOException var2) {
        }

        return properties;
    }

    private static double[] readStageHeights(Properties properties, String prefix, double[] defaults) {
        double[] values = (double[])defaults.clone();

        for (int i = 0; i < values.length; i++) {
            values[i] = readDouble(properties, prefix + (i + 1), values[i]);
        }

        return sanitizeStageHeights(values);
    }

    private static int[] readStageTicks(Properties properties, String prefix, int[] defaults) {
        int[] values = (int[])defaults.clone();

        for (int i = 0; i < values.length; i++) {
            values[i] = sanitizeStageTicksValue(readInt(properties, prefix + (i + 1), values[i]));
        }

        return sanitizeStageTicks(values);
    }

    private static double[] sanitizeStageHeights(double[] values) {
        double[] source = values != null && values.length >= 3 ? values : DEFAULT_STAGE_HEIGHTS;
        double[] sanitized = new double[]{clamp(roundStageHeight(source[0]), 8.0, 510.0), 0.0, 0.0};
        sanitized[1] = clamp(roundStageHeight(source[1]), sanitized[0] + 1.0, 511.0);
        sanitized[2] = clamp(roundStageHeight(source[2]), sanitized[1] + 1.0, 512.0);
        return sanitized;
    }

    private static int[] sanitizeStageTicks(int[] values) {
        int[] source = values != null && values.length >= 3 ? values : DEFAULT_STAGE_TICKS;
        int[] sanitized = new int[3];

        for (int i = 0; i < sanitized.length; i++) {
            sanitized[i] = sanitizeStageTicksValue(source[i]);
        }

        return sanitized;
    }

    private static double sanitizeZoomStageGlideHeight(double value) {
        return Math.round(clamp(value, 0.1, 5.0) * 10.0) / 10.0;
    }

    private static double sanitizeBodyCameraHeight(double value) {
        return Math.round(clamp(value, 0.1, 10.0) * 10.0) / 10.0;
    }

    private static double sanitizeBodyGlideHeight(double value) {
        return Math.round(clamp(value, 0.1, 5.0) * 10.0) / 10.0;
    }

    private static int sanitizeStageTicksValue(int value) {
        return clamp(value, 1, 200);
    }

    private static int sanitizeLocalPlayerHideTicks(int value) {
        return clamp(value, 0, 20);
    }

    private static double sanitizeSoundVolume(double value) {
        return Math.round(clamp(value, 0.1, 1.0) * 10.0) / 10.0;
    }

    private static boolean readBoolean(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static double readDouble(Properties properties, String key, double fallback) {
        try {
            return Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int readInt(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double roundStageHeight(double value) {
        return Math.rint(value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static TeleportAnimationConfig.ZoomDimension sanitizeZoomDimension(TeleportAnimationConfig.ZoomDimension dimension) {
        return dimension == null ? TeleportAnimationConfig.ZoomDimension.OVERWORLD : dimension;
    }

    private static void writeStageHeights(Properties properties, String prefix, double[] values) {
        double[] sanitized = sanitizeStageHeights(values);

        for (int i = 0; i < sanitized.length; i++) {
            properties.setProperty(prefix + (i + 1), Integer.toString((int)sanitized[i]));
        }
    }

    private static void writeStageTicks(Properties properties, String prefix, int[] values) {
        int[] sanitized = sanitizeStageTicks(values);

        for (int i = 0; i < sanitized.length; i++) {
            properties.setProperty(prefix + (i + 1), Integer.toString(sanitized[i]));
        }
    }

    public enum ZoomDimension {
        OVERWORLD,
        NETHER,
        END;

        public static TeleportAnimationConfig.ZoomDimension fromLevel(ResourceKey<Level> dimension) {
            if (Level.NETHER.equals(dimension)) {
                return NETHER;
            } else {
                return Level.END.equals(dimension) ? END : OVERWORLD;
            }
        }
    }
}
