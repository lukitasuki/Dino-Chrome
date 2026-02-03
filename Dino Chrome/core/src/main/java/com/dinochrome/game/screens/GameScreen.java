package com.dinochrome.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.dinochrome.game.DinoChromeGame;
import com.dinochrome.game.entities.Dino;
import com.dinochrome.game.entities.obstacles.Bird;
import com.dinochrome.game.entities.obstacles.Cactus;
import com.dinochrome.game.entities.obstacles.Obstacle;
import com.dinochrome.game.utils.ScoreManager;
import com.dinochrome.game.world.Background;
import com.dinochrome.game.world.Ground;

public class GameScreen implements Screen {

    public static final float WORLD_WIDTH = 800;
    public static final float WORLD_HEIGHT = 480;

    private final DinoChromeGame game;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;

    private Background background;
    private Ground ground;
    private Dino dino;

    private Array<Obstacle> obstacles;

    private ScoreManager scoreManager;
    private BitmapFont font;

    private float spawnTimer;
    private float spawnInterval;
    private float gameSpeed;

    public GameScreen(DinoChromeGame game) {
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
        ground = new Ground(WORLD_WIDTH);
        dino = new Dino(100, Ground.GROUND_HEIGHT);

        obstacles = new Array<>();
        scoreManager = new ScoreManager();

        font = new BitmapFont();
        font.getData().setScale(1.6f);

        spawnTimer = 0f;
        spawnInterval = 1.6f;
        gameSpeed = 220f;
    }

    @Override
    public void render(float delta) {

        update(delta);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        background.render(batch);
        ground.render(batch);

        for (Obstacle obstacle : obstacles) {
            obstacle.render(batch);
        }

        dino.render(batch);

        font.draw(batch, "SCORE: " + scoreManager.getScore(), 20, WORLD_HEIGHT - 20);

        batch.end();
    }

    private void update(float delta) {

        // INPUT
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            dino.jump();
        }

        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            dino.startSlide();
        } else {
            dino.stopSlide();
        }

        // SCORE
        scoreManager.update(delta);

        // DIFICULTAD PROGRESIVA
        gameSpeed += delta * 2.5f;

        // UPDATE DEL MUNDO
        background.update(delta, gameSpeed * 0.3f); // parallax
        ground.update(delta, gameSpeed);
        dino.update(delta);

        // SPAWN DE OBSTÁCULOS
        spawnTimer += delta;
        if (spawnTimer >= spawnInterval) {

            if (Math.random() < 0.7f) {
                obstacles.add(new Cactus(WORLD_WIDTH + 60));
            } else {
                obstacles.add(new Bird(WORLD_WIDTH + 60));
            }

            spawnTimer = 0f;
        }

        // UPDATE + COLISIONES
        for (int i = obstacles.size - 1; i >= 0; i--) {

            Obstacle obstacle = obstacles.get(i);
            obstacle.update(delta);

            if (obstacle.getBounds().overlaps(dino.getBounds())) {
                game.setScreen(
                        new GameOverScreen(game, scoreManager.getScore())
                );
                return;
            }

            if (obstacle.isOffScreen()) {
                obstacles.removeIndex(i);
            }
        }
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
    }
}
