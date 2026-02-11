package com.dinochrome.game.network;

import com.badlogic.gdx.Gdx;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class NetThread extends Thread {

    public static final class LobbyState {
        public int players;
        public String p1Name; public boolean p1Ready;
        public String p2Name; public boolean p2Ready;
    }

    public static final class StartInfo {
        public int seed;
        public long t0;
        public float speed;
    }

    public static final class ResultInfo {
        public int winner;
        public String reason;
    }

    public static final class RivalState {
        public int id;
        public float y;
        public boolean onGround;
        public boolean sliding;
        public int hp;
        public int score;
    }

    // =========================
    // Config
    // =========================
    private static final int DEFAULT_PORT = 4321;

    // =========================
    // Estado
    // =========================
    private volatile boolean running = true;

    private DatagramSocket socket;

    private volatile InetAddress serverIp = null;
    private volatile int serverPort = DEFAULT_PORT;

    private volatile boolean connected = false;
    private volatile boolean discovering = false;

    private volatile int myId = 0;

    private volatile LobbyState lobbyState = new LobbyState();
    private volatile StartInfo pendingStart = null;
    private volatile ResultInfo pendingResult = null;
    private volatile RivalState lastRivalState = null;

    private final ConcurrentLinkedQueue<String> inbox = new ConcurrentLinkedQueue<>();
    private Thread pingThread;

    public NetThread() {
        try {
            socket = new DatagramSocket(); // puerto aleatorio local
            socket.setBroadcast(true);
            socket.setSoTimeout(500);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo crear DatagramSocket en cliente", e);
        }
        setName("NetThread-Recv");
        setDaemon(true);
    }

    // =========================
    // API
    // =========================

    /** Modo A: conectar directo por IP (para debug) */
    public void setServer(String host, int port) {
        try {
            this.serverIp = InetAddress.getByName(host);
            this.serverPort = port;
        } catch (Exception e) {
            throw new RuntimeException("Host inválido: " + host, e);
        }
    }

    /** Modo B: discovery LAN (recomendado) */
    public void discoverAndConnect(String playerName, int port, int timeoutMs) {
        this.serverPort = port;
        discovering = true;

        long deadline = System.currentTimeMillis() + timeoutMs;

        // Mandamos DISCOVER varias veces (por si el WiFi se come el primer paquete)
        while (running && System.currentTimeMillis() < deadline && serverIp == null) {
            sendDiscover(port);
            // esperamos respuestas en el run() (recibe)
            sleepQuiet(250);
        }

        discovering = false;

        if (serverIp == null) {
            log("NET", "DISCOVER: no se encontró servidor en la LAN (timeout).");
            return;
        }

        conectar(playerName);
    }

    public void conectar(String nombre) {
        if (serverIp == null) {
            log("NET", "No hay serverIp seteada. Usá setServer() o discoverAndConnect().");
            return;
        }
        enviar("CONNECT name=" + sanitizeName(nombre));
        iniciarPing();
    }

    public boolean isConnected() {
        return connected;
    }

    public InetAddress getServerIp() {
        return serverIp;
    }

    public void setReady(boolean ready) {
        enviar("READY v=" + (ready ? "1" : "0"));
    }

    public void avisarMuerte(int score, int timeSeconds) {
        enviar("DEAD score=" + score + ";time=" + timeSeconds);
    }

    public void enviarEstadoJugador(float y, boolean onGround, boolean sliding, int hp, int score) {
        String msg = "STATE y=" + y
                + ";g=" + (onGround ? "1" : "0")
                + ";s=" + (sliding ? "1" : "0")
                + ";hp=" + hp
                + ";score=" + score;
        enviar(msg);
    }

    public void desconectar() {
        running = false;
        try { enviar("DISCONNECT"); } catch (Exception ignored) {}
        if (pingThread != null) pingThread.interrupt();
        if (socket != null && !socket.isClosed()) socket.close();
    }

    public int getMyId() { return myId; }

    public LobbyState getLobbyStateSnapshot() {
        LobbyState src = lobbyState;
        LobbyState copy = new LobbyState();
        copy.players = src.players;
        copy.p1Name = src.p1Name; copy.p1Ready = src.p1Ready;
        copy.p2Name = src.p2Name; copy.p2Ready = src.p2Ready;
        return copy;
    }

    public StartInfo consumeStart() { StartInfo s = pendingStart; pendingStart = null; return s; }
    public ResultInfo consumeResult() { ResultInfo r = pendingResult; pendingResult = null; return r; }
    public RivalState getLastRivalState() { return lastRivalState; }

    public String pollRaw() { return inbox.poll(); }

    // =========================
    // Thread loop
    // =========================
    @Override
    public void run() {
        while (running) {
            try {
                byte[] buf = new byte[1600];
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);

                String msg = new String(pkt.getData(), 0, pkt.getLength(), StandardCharsets.UTF_8).trim();
                inbox.add(msg);

                // Si estamos en discovery, aceptamos HERE aunque no esté “registrado”
                if (msg.startsWith("HERE")) {
                    InetAddress ip = pkt.getAddress();
                    serverIp = ip;
                    connected = false;
                    log("NET", "HERE recibido desde " + ip.getHostAddress() + ":" + pkt.getPort() + " msg=" + msg);
                    continue;
                }

                procesar(msg);

            } catch (SocketTimeoutException ignored) {
            } catch (Exception e) {
                if (!running) break;
                e.printStackTrace();
            }
        }
    }

    // =========================
    // Internals
    // =========================
    private void sendDiscover(int port) {
        List<InetAddress> broadcasts = NetworkUtils.getBroadcastAddresses();
        for (InetAddress b : broadcasts) {
            sendRawTo("DISCOVER", b, port);
            log("NET", "DISCOVER -> " + b.getHostAddress() + ":" + port);
        }
    }

    private void procesar(String msg) {

        if (msg.startsWith("WELCOME")) {
            myId = parseInt(keyOf(msg, "id"), 0);
            connected = true;
            log("NET", "WELCOME myId=" + myId + " server=" + (serverIp != null ? serverIp.getHostAddress() : "?"));
            return;
        }

        if (msg.startsWith("LOBBY")) {
            LobbyState st = new LobbyState();
            st.players = parseInt(keyOf(msg, "players"), 0);

            String p1 = keyOf(msg, "p1");
            if (p1 != null) {
                String[] a = p1.split(",");
                if (a.length == 2) { st.p1Name = a[0]; st.p1Ready = "1".equals(a[1]); }
            }

            String p2 = keyOf(msg, "p2");
            if (p2 != null) {
                String[] a = p2.split(",");
                if (a.length == 2) { st.p2Name = a[0]; st.p2Ready = "1".equals(a[1]); }
            }

            lobbyState = st;
            return;
        }

        if (msg.startsWith("START")) {
            StartInfo s = new StartInfo();
            s.seed = parseInt(keyOf(msg, "seed"), 0);
            s.t0 = parseLong(keyOf(msg, "t0"), 0L);
            s.speed = parseFloat(keyOf(msg, "speed"), 220f);
            pendingStart = s;
            log("NET", "START seed=" + s.seed + " t0=" + s.t0 + " speed=" + s.speed);
            return;
        }

        if (msg.startsWith("RESULT")) {
            ResultInfo r = new ResultInfo();
            r.winner = parseInt(keyOf(msg, "winner"), 0);
            r.reason = keyOf(msg, "reason");
            pendingResult = r;
            log("NET", "RESULT winner=" + r.winner + " reason=" + r.reason);
            return;
        }

        if (msg.startsWith("RIVAL_STATE")) {
            RivalState rs = new RivalState();
            rs.id = parseInt(keyOf(msg, "id"), 0);
            rs.y = parseFloat(keyOf(msg, "y"), 0f);
            rs.onGround = "1".equals(keyOf(msg, "g"));
            rs.sliding = "1".equals(keyOf(msg, "s"));
            rs.hp = parseInt(keyOf(msg, "hp"), 10);
            rs.score = parseInt(keyOf(msg, "score"), 0);
            lastRivalState = rs;
            return;
        }

        if (msg.startsWith("PONG")) return;
    }

    private void enviar(String msg) {
        if (!running) return;
        if (serverIp == null) return;
        sendRawTo(msg, serverIp, serverPort);
    }

    private void sendRawTo(String msg, InetAddress ip, int port) {
        try {
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket pkt = new DatagramPacket(data, data.length, ip, port);
            socket.send(pkt);
        } catch (Exception ignored) {}
    }

    private void iniciarPing() {
        if (pingThread != null) return;

        pingThread = new Thread(() -> {
            while (running) {
                sleepQuiet(1200);
                if (!running) return;
                if (serverIp != null) enviar("PING");
            }
        }, "NetThread-Ping");
        pingThread.setDaemon(true);
        pingThread.start();
    }

    private static String keyOf(String msg, String key) {
        String payload = msg;
        int sp = msg.indexOf(' ');
        if (sp >= 0) payload = msg.substring(sp + 1);
        payload = payload.replace(" ", "");
        String[] parts = payload.split(";");
        for (String part : parts) {
            String[] kv = part.split("=");
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return null;
    }

    private static int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch (Exception e) { return def; } }
    private static long parseLong(String s, long def) { try { return Long.parseLong(s); } catch (Exception e) { return def; } }
    private static float parseFloat(String s, float def) { try { return Float.parseFloat(s); } catch (Exception e) { return def; } }

    private static String sanitizeName(String name) {
        if (name == null) return "Player";
        name = name.trim();
        if (name.isEmpty()) return "Player";
        if (name.length() > 12) name = name.substring(0, 12);
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "");
    }

    private static void log(String tag, String s) {
        try { Gdx.app.log(tag, s); } catch (Exception ignored) { System.out.println("[" + tag + "] " + s); }
    }

    private static void sleepQuiet(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
