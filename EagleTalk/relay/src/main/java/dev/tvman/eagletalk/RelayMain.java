package dev.tvman.eagletalk;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minimal relay: JSON join handshake followed by binary PCM16 audio frames. */
public final class RelayMain extends WebSocketServer {
    private static final Pattern FIELD = Pattern.compile("\\\"(token|room|name)\\\"\\s*:\\s*\\\"([^\\\"]{0,160})\\\"");
    private final Map<WebSocket, String> rooms = new ConcurrentHashMap<>();
    private final String token = System.getenv().getOrDefault("EAGLETALK_TOKEN", "");

    private RelayMain(int port) { super(new InetSocketAddress("0.0.0.0", port)); }
    public static void main(String[] args) {
        int port = 8765;
        if (args.length > 0) try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) { System.err.println("Invalid port; using 8765"); }
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Port must be 1..65535");
        if (System.getenv("EAGLETALK_TOKEN") == null || System.getenv("EAGLETALK_TOKEN").length() < 16) {
            System.err.println("WARNING: EAGLETALK_TOKEN is unset/short. Set a random token and use TLS via a reverse proxy before public deployment.");
        }
        RelayMain relay = new RelayMain(port);
        relay.setConnectionLostTimeout(30);
        relay.start();
        System.out.println("EagleTalk relay listening on port " + port);
    }
    @Override public void onOpen(WebSocket conn, ClientHandshake handshake) { conn.setAttachment("pending"); }
    @Override public void onMessage(WebSocket conn, String message) {
        if (message.length() > 512) { conn.close(1008, "Handshake too large"); return; }
        Matcher m = FIELD.matcher(message);
        String supplied = null, room = null;
        while (m.find()) { if (m.group(1).equals("token")) supplied = m.group(2); if (m.group(1).equals("room")) room = m.group(2); }
        if (supplied == null || room == null || room.isBlank() || room.length() > 64 || !constantTimeEquals(token, supplied)) {
            conn.close(1008, "Invalid token or room"); return;
        }
        rooms.put(conn, room);
        conn.send("{\"type\":\"ready\",\"room\":\"" + safe(room) + "\"}");
    }
    @Override public void onMessage(WebSocket conn, ByteBuffer bytes) {
        String room = rooms.get(conn);
        if (room == null || bytes.remaining() > 8192) return;
        ByteBuffer copy = ByteBuffer.allocate(bytes.remaining()); copy.put(bytes.slice()); copy.flip();
        for (Map.Entry<WebSocket, String> e : rooms.entrySet()) {
            WebSocket peer = e.getKey();
            if (peer != conn && room.equals(e.getValue()) && peer.isOpen()) peer.send(copy.asReadOnlyBuffer());
        }
    }
    @Override public void onClose(WebSocket conn, int code, String reason, boolean remote) { rooms.remove(conn); }
    @Override public void onError(WebSocket conn, Exception ex) { if (ex != null) System.err.println("Relay client error: " + ex.getMessage()); }
    @Override public void onStart() { }
    private static String safe(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
