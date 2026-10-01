package fr.poubone.att2.client.teleport;

import net.minecraft.client.Minecraft;

public final class TeleportCommandSender {
    private static boolean bypassNextCommand;

    private TeleportCommandSender() {
    }

    public static boolean isBypassing() {
        return bypassNextCommand;
    }

    public static void sendDeferredCommand(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() != null) {
            bypassNextCommand = true;

            try {
                client.getConnection().sendCommand(command);
            } finally {
                bypassNextCommand = false;
            }
        }
    }
}
