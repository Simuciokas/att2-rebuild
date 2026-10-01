package fr.poubone.att2.client.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class DiscordIpcClient implements AutoCloseable {
    static final int OP_HANDSHAKE = 0;
    static final int OP_FRAME = 1;
    static final int OP_CLOSE = 2;
    static final int OP_PING = 3;
    static final int OP_PONG = 4;
    private static final int HEADER = 8;
    private static final int MAX_PAYLOAD = 65536;
    private final DiscordIpcClient.Transport transport;

    private DiscordIpcClient(DiscordIpcClient.Transport transport) {
        this.transport = transport;
    }

    static DiscordIpcClient connect(String clientId) throws IOException {
        IOException last = null;

        for (String endpoint : endpoints()) {
            try {
                DiscordIpcClient.Transport transport = open(endpoint);
                DiscordIpcClient client = new DiscordIpcClient(transport);
                JsonObject handshake = new JsonObject();
                handshake.addProperty("v", 1);
                handshake.addProperty("client_id", clientId);
                client.write(0, handshake.toString());
                JsonObject reply = client.readBlocking(2000L);
                if (reply == null) {
                    transport.close();
                    throw new IOException("Discord IPC handshake timed out on " + endpoint);
                }

                return client;
            } catch (IOException e) {
                last = e;
            }
        }

        throw last != null ? last : new IOException("Discord IPC endpoint not found");
    }

    void sendFrame(JsonObject payload) throws IOException {
        this.write(1, payload.toString());
    }

    JsonObject poll() throws IOException {
        JsonObject frame = this.readAvailable();
        if (frame == null) {
            return null;
        }

        if ("PING".equals(frame.get("cmd") == null ? "" : frame.get("cmd").getAsString())) {
            this.write(4, frame.toString());
        }

        return frame;
    }

    @Override
    public void close() {
        try {
            this.write(2, "{}");
        } catch (IOException var2) {
        }

        this.transport.close();
    }

    private void write(int opcode, String json) throws IOException {
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + payload.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(opcode);
        buf.putInt(payload.length);
        buf.put(payload);
        this.transport.writeFully(buf.array());
    }

    private JsonObject readBlocking(long timeoutMs) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            JsonObject frame = this.readAvailable();
            if (frame != null) {
                return frame;
            }

            try {
                Thread.sleep(20L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        return null;
    }

    private JsonObject readAvailable() throws IOException {
        byte[] header = this.transport.readExactIfAvailable(8);
        if (header == null) {
            return null;
        }

        ByteBuffer buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int opcode = buf.getInt();
        int length = buf.getInt();
        if (length >= 0 && length <= 65536) {
            byte[] payload = this.transport.readExact(length);
            if (opcode == 3) {
                this.write(4, new String(payload, StandardCharsets.UTF_8));
            }

            return payload.length == 0 ? new JsonObject() : JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
        } else {
            throw new IOException("Discord IPC payload too large: " + length);
        }
    }

    private static DiscordIpcClient.Transport open(String endpoint) throws IOException {
        return isWindows() ? new DiscordIpcClient.PipeTransport(endpoint) : new DiscordIpcClient.UnixTransport(Path.of(endpoint));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static List<String> endpoints() {
        List<String> list = new ArrayList<>();
        if (isWindows()) {
            for (int i = 0; i < 10; i++) {
                list.add("\\\\.\\pipe\\discord-ipc-" + i);
            }

            return list;
        } else {
            List<Path> roots = new ArrayList<>();
            String xdg = System.getenv("XDG_RUNTIME_DIR");
            if (xdg != null && !xdg.isBlank()) {
                Path runtime = Path.of(xdg);
                roots.add(runtime);
                roots.add(runtime.resolve("app").resolve("com.discordapp.Discord"));
                roots.add(runtime.resolve("snap.discord"));
            }

            String tmp = System.getProperty("java.io.tmpdir");
            if (tmp != null) {
                roots.add(Path.of(tmp));
            }

            roots.add(Path.of("/tmp"));

            for (Path root : roots) {
                for (int i = 0; i < 10; i++) {
                    Path path = root.resolve("discord-ipc-" + i);
                    if (Files.exists(path)) {
                        list.add(path.toString());
                    }
                }
            }

            if (list.isEmpty()) {
                Path fallback = Path.of(xdg != null && !xdg.isBlank() ? xdg : "/tmp").resolve("discord-ipc-0");
                list.add(fallback.toString());
            }

            return list;
        }
    }

    private static final class PipeTransport implements DiscordIpcClient.Transport {
        private final RandomAccessFile pipe;

        PipeTransport(String name) throws IOException {
            this.pipe = new RandomAccessFile(name, "rw");
        }

        @Override
        public void writeFully(byte[] bytes) throws IOException {
            this.pipe.write(bytes);
        }

        @Override
        public byte[] readExactIfAvailable(int count) throws IOException {
            if (this.pipe.length() < count) {
                return null;
            }

            byte[] data = new byte[count];
            this.pipe.readFully(data);
            return data;
        }

        @Override
        public byte[] readExact(int count) throws IOException {
            long deadline = System.currentTimeMillis() + 2000L;

            while (this.pipe.length() < count) {
                if (System.currentTimeMillis() > deadline) {
                    throw new IOException("Timed out reading Discord IPC pipe");
                }

                try {
                    Thread.sleep(5L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while reading Discord IPC", e);
                }
            }

            byte[] data = new byte[count];
            this.pipe.readFully(data);
            return data;
        }

        @Override
        public void close() {
            try {
                this.pipe.close();
            } catch (IOException var2) {
            }
        }
    }

    private interface Transport {
        void writeFully(byte[] var1) throws IOException;

        byte[] readExactIfAvailable(int var1) throws IOException;

        byte[] readExact(int var1) throws IOException;

        void close();
    }

    private static final class UnixTransport implements DiscordIpcClient.Transport {
        private final SocketChannel channel;

        UnixTransport(Path path) throws IOException {
            SocketChannel open = SocketChannel.open(StandardProtocolFamily.UNIX);
            open.configureBlocking(false);
            open.connect(UnixDomainSocketAddress.of(path));
            if (!open.finishConnect()) {
                open.close();
                throw new IOException("Unix Discord IPC connect failed: " + path);
            }

            this.channel = open;
        }

        @Override
        public void writeFully(byte[] bytes) throws IOException {
            ByteBuffer buf = ByteBuffer.wrap(bytes);

            while (buf.hasRemaining()) {
                int n = this.channel.write(buf);
                if (n < 0) {
                    throw new IOException("Discord IPC socket closed");
                }

                if (n == 0) {
                    try {
                        Thread.sleep(5L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while writing Discord IPC", e);
                    }
                }
            }
        }

        @Override
        public byte[] readExactIfAvailable(int count) throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(count);
            int n = this.channel.read(buf);
            if (n < 0) {
                throw new IOException("Discord IPC socket closed");
            }

            if (n == 0) {
                return null;
            }

            while (buf.hasRemaining()) {
                int more = this.channel.read(buf);
                if (more < 0) {
                    throw new IOException("Discord IPC socket closed");
                }

                if (more == 0) {
                    try {
                        Thread.sleep(5L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while reading Discord IPC", e);
                    }
                }
            }

            return buf.array();
        }

        @Override
        public byte[] readExact(int count) throws IOException {
            byte[] data = this.readExactIfAvailable(count);
            if (data != null) {
                return data;
            }

            long deadline = System.currentTimeMillis() + 2000L;

            while (System.currentTimeMillis() < deadline) {
                data = this.readExactIfAvailable(count);
                if (data != null) {
                    return data;
                }
            }

            throw new IOException("Timed out reading Discord IPC payload");
        }

        @Override
        public void close() {
            try {
                this.channel.close();
            } catch (IOException var2) {
            }
        }
    }
}
