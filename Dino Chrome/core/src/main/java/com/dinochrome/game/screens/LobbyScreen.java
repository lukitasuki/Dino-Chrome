package com.dinochrome.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import com.dinochrome.game.DinoChromeGame;
import com.dinochrome.game.network.NetThread;

public final class LobbyScreen implements Screen {

    private final DinoChromeGame game;
    private final NetThread net;

    private SpriteBatch batch;
    private BitmapFont font;

    private boolean localReady = false;

    public LobbyScreen(DinoChromeGame game, NetThread net) {
        this.game = game;
        this.net = net;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.6f);
    }

    @Override
    public void render(float delta) {

        // 1) Si llegó START: CAMBIAR DE SCREEN (esto es lo que faltaba)
        NetThread.StartInfo start = net.consumeStart();
        if (start != null) {
            System.out.println("[LOBBY] START recibido, cambiando a GameScreenMulti...");
            game.setScreen(new GameScreenMulti(game, net, start.seed, start.t0, start.speed));
            return;
        }

        // 2) Input ready
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            localReady = !localReady;
            net.setReady(localReady);
        }

        // 3) Dibujo
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();

        NetThread.LobbyState st = net.getLobbyStateSnapshot();

        font.draw(batch, "LOBBY (R = Ready)", 40, 440);
        font.draw(batch, "MyID: " + net.getMyId(), 40, 400);

        String p1 = (st.p1Name == null ? "-" : st.p1Name) + " ready=" + (st.p1Ready ? "1" : "0");
        String p2 = (st.p2Name == null ? "-" : st.p2Name) + " ready=" + (st.p2Ready ? "1" : "0");

        font.draw(batch, "P1: " + p1, 40, 340);
        font.draw(batch, "P2: " + p2, 40, 300);

        font.draw(batch, "Local Ready: " + (localReady ? "YES" : "NO"), 40, 240);

        batch.end();
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
