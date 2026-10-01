package fr.poubone.att2.client.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Optional Flashback integration, looked up reflectively so Flashback stays a soft dependency.
 *
 * <p>Flashback replays the map's chat messages, so the shop menus they trigger open again during
 * playback. Closing a menu is purely client-side and never recorded, so the replayed menu would
 * never close. Callers use {@link #isInReplay()} to skip opening those menus while a replay plays.
 */
public final class FlashbackCompat {

    private static final MethodHandle IS_IN_REPLAY = findIsInReplay();

    private FlashbackCompat() {
    }

    private static MethodHandle findIsInReplay() {
        if (!FabricLoader.getInstance().isModLoaded("flashback")) {
            return null;
        }
        try {
            Class<?> flashback = Class.forName("com.moulberry.flashback.Flashback");
            return MethodHandles.publicLookup().findStatic(flashback, "isInReplay", MethodType.methodType(boolean.class));
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    public static boolean isInReplay() {
        if (IS_IN_REPLAY == null) {
            return false;
        }
        try {
            return (boolean) IS_IN_REPLAY.invokeExact();
        } catch (Throwable t) {
            return false;
        }
    }
}
