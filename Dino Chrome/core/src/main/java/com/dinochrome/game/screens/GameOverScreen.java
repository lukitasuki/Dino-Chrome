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

public class GameOverScreen implements Screen {

    private static final float WORLD_WIDTH = 800;
    private static final float WORLD_HEIGHT = 480;

    private final DinoChromeGame game;
    private final int finalScore;
    private final boolean iWon;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;

    private Background background;
    private BitmapFont font;

    public GameOverScreen(DinoChromeGame game, int finalScore, boolean iWon) {
        this.game = game;
        this.finalScore = finalScore;
        this.iWon = iWon;
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
        font.getData().setScale(2.4f);
    }

    @Override
    public void render(float delta) {

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            NetThread net = game.getNet();
            game.setScreen(new LobbyScreen(game, net));
            return;
        }

        background.update(delta, 40);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        background.render(batch);

        String title = iWon ? "GANASTE EL JUEGO" : "PERDISTE EL JUEGO";

        font.draw(
                batch,
                title + "\n\n" +
                        "SCORE: " + finalScore + "\n\n" +
                        "APRETA ESPACIO\nPARA VOLVER AL LOBBY",
                160,
                360
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
