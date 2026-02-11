package com.dinochrome.game.network;

import com.badlogic.gdx.Gdx;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class NetThread extends Thread {

    public static final class LobbyState {
        public int players;
        public String p1Name;
        public boolean p1Ready;
        public String p2Name;
        public boolean p2Ready;
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

    private volatile boolean running = true;

    private DatagramSocket socket;
    private InetAddress serverIp;
    private int serverPort;

    private volatile int myId = 0;

    private volatile LobbyState lobbyState = new LobbyState();
    private volatile StartInfo pendingStart = null;
    private volatile ResultInfo pendingResult = null;

    private volatile RivalState lastRivalState = null;

    private final ConcurrentLinkedQueue<String> inbox = new ConcurrentLinkedQueue<>();
    private Thread pingThread;

    public NetThread(String host, int port) {
        try {
            this.serverIp = InetAddress.getByName(host);
            this.serverPort = port;

            this.socket = new DatagramSocket();
            this.socket.setSoTimeout(800);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo crear cliente UDP", e);
        }
        setName("NetThread-Recv");
        setDaemon(true);
    }

    public void conectar(String nombre) {
        enviar("CONNECT name=" + sanitizeName(nombre));
        iniciarPing();
    }

    public void setReady(boolean ready) {
        enviar("READY v=" + (ready ? "1" : "0"));
    }

    public void avisarMuerte(int score, int timeSeconds) {
        enviar("DEAD score=" + score + ";time=" + timeSeconds);
    }

    // NUEVO: estado periódico
    public void enviarEstadoJugador(float y, boolean onGround, boolean sliding, int hp, int score) {
        // STATE y=123.4;g=1;s=0;hp=9;score=120
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

    public int getMyId() {
        return myId;
    }

    public LobbyState getLobbyStateSnapshot() {
        LobbyState src = lobbyState;
        LobbyState copy = new LobbyState();
        copy.players = src.players;
        copy.p1Name = src.p1Name;
        copy.p1Ready = src.p1Ready;
        copy.p2Name = src.p2Name;
        copy.p2Ready = src.p2Ready;
        return copy;
    }

    public StartInfo consumeStart() {
        StartInfo s = pendingStart;
        pendingStart = null;
        return s;
    }

    public ResultInfo consumeResult() {
        ResultInfo r = pendingResult;
        pendingResult = null;
        return r;
    }

    public RivalState getLastRivalState() {
        return lastRivalState;
    }

    public String pollRaw() {
        return inbox.poll();
    }

    @Override
    public void run() {
        while (running) {
            try {
                byte[] buf = new byte[1500];
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);

                String msg = new String(pkt.getData(), 0, pkt.getLength(), StandardCharsets.UTF_8).trim();
                inbox.add(msg);
                procesar(msg);

            } catch (SocketTimeoutException ignored) {
            } catch (Exception e) {
                if (!running) break;
                e.printStackTrace();
            }
        }
    }

    private void procesar(String msg) {

        if (msg.startsWith("WELCOME")) {
            myId = parseInt(keyOf(msg, "id"), 0);
            log("WELCOME myId=" + myId);
            return;
        }

        if (msg.startsWith("LOBBY")) {
            LobbyState st = new LobbyState();
            st.players = parseInt(keyOf(msg, "players"), 0);

            String p1 = keyOf(msg, "p1");
            if (p1 != null) {
                String[] a = p1.split(",");
                if (a.length == 2) {
                    st.p1Name = a[0];
                    st.p1Ready = "1".equals(a[1]);
                }
            }

            String p2 = keyOf(msg, "p2");
            if (p2 != null) {
                String[] a = p2.split(",");
                if (a.length == 2) {
                    st.p2Name = a[0];
                    st.p2Ready = "1".equals(a[1]);
                }
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
            log("START seed=" + s.seed + " t0=" + s.t0 + " speed=" + s.speed);
            return;
        }

        if (msg.startsWith("RESULT")) {
            ResultInfo r = new ResultInfo();
            r.winner = parseInt(keyOf(msg, "winner"), 0);
            r.reason = keyOf(msg, "reason");
            pendingResult = r;
            log("RESULT winner=" + r.winner + " reason=" + r.reason);
            return;
        }

        // NUEVO: estado del rival
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

        if (msg.startsWith("ERROR")) log("SERVER ERROR: " + msg);
    }

    private void enviar(String msg) {
        if (!running) return;
        try {
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket pkt = new DatagramPacket(data, data.length, serverIp, serverPort);
            socket.send(pkt);
        } catch (Exception ignored) {}
    }

    private void iniciarPing() {
        if (pingThread != null) return;

        pingThread = new Thread(() -> {
            while (running) {
                try { Thread.sleep(1200); } catch (InterruptedException e) { return; }
                enviar("PING");
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

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s); } catch (Exception e) { return def; }
    }
    private static float parseFloat(String s, float def) {
        try { return Float.parseFloat(s); } catch (Exception e) { return def; }
    }

    private static String sanitizeName(String name) {
        if (name == null) return "Player";
        name = name.trim();
        if (name.isEmpty()) return "Player";
        if (name.length() > 12) name = name.substring(0, 12);
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "");
    }

    private static void log(String s) {
        try { Gdx.app.log("NET", s); } catch (Exception ignored) { System.out.println("[NET] " + s); }
    }
}
