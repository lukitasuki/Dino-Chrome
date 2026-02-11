package com.dinochrome.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.dinochrome.game.DinoChromeGame;
import com.dinochrome.game.network.NetThread;
import com.dinochrome.game.world.Background;

public class MenuScreen implements Screen {

    private static final float WORLD_WIDTH = 800;
    private static final float WORLD_HEIGHT = 480;

    private final DinoChromeGame game;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;

    private Background background;
    private BitmapFont font;

    public MenuScreen(DinoChromeGame game) {
        this.game = game;
    }

    @Override
    public void show() {

        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);
        camera.update();

        batch = new SpriteBatch();
        background = new Background(WORLD_WIDTH, WORLD_HEIGHT);

        font = new BitmapFont();
        font.getData().setScale(3.2f, 3.2f);
    }

    @Override
    public void render(float delta) {

        // SPACE -> ir al Lobby (NO al GameScreenMulti directo)
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {

            NetThread net = game.getNet();
            if (net == null) {
                System.out.println("[MENU] net == null. No inicializaste la red en DinoChromeGame.");
                return;
            }

            game.setScreen(new LobbyScreen(game, net));
            return;
        }

        background.update(delta, 50);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        background.render(batch);

        font.draw(
                batch,
                "DINO CHROME\n\nPRESIONA ESPACIO PARA JUGAR",
                120,
                320
        );

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        background.dispose();
    }
}
