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
import com.dinochrome.game.network.NetThread;
import com.dinochrome.game.utils.ScoreManager;
import com.dinochrome.game.world.Background;
import com.dinochrome.game.world.Ground;

import java.util.Random;

public final class GameScreenMulti implements Screen {

    public static final float WORLD_WIDTH = 800;
    public static final float WORLD_HEIGHT = 480;

    private final DinoChromeGame game;
    private final NetThread net;

    private final int seed;
    private final long t0Ms;
    private final Random rng;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;

    private Background background;
    private Ground ground;

    private Dino dinoLocal;
    private Dino dinoRival;

    private Array<Obstacle> obstacles;

    private ScoreManager scoreLocal;
    private int scoreRival = 0;

    private int hpRival = 10;

    private BitmapFont font;

    private float runtime;
    private boolean started;

    private float gameSpeed;

    private float nextSpawnAt;

    // envío de estado
    private float stateSendTimer = 0f;
    private static final float STATE_SEND_INTERVAL = 0.05f; // 20 Hz

    public GameScreenMulti(DinoChromeGame game, NetThread net, int seed, long t0Ms, float startSpeed) {
        this.game = game;
        this.net = net;
        this.seed = seed;
        this.t0Ms = t0Ms;
        this.gameSpeed = startSpeed;
        this.rng = new Random(seed);
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

        dinoLocal = new Dino(100, Ground.GROUND_HEIGHT);

        // Rival un poco más adelante para verlo
        dinoRival = new Dino(160, Ground.GROUND_HEIGHT);

        obstacles = new Array<>();
        scoreLocal = new ScoreManager();

        font = new BitmapFont();
        font.getData().setScale(1.6f);

        runtime = 0f;
        started = false;

        scheduleNextSpawn(0f);
    }

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        background.render(batch);
        ground.render(batch);

        for (Obstacle obstacle : obstacles) obstacle.render(batch);

        // Render dinos
        dinoLocal.render(batch);
        dinoRival.render(batch);

        // UI separado
        font.draw(batch, "YOU HP: " + dinoLocal.getHp() + "  SCORE: " + scoreLocal.getScore(), 20, WORLD_HEIGHT - 20);
        font.draw(batch, "RIVAL HP: " + hpRival + "  SCORE: " + scoreRival, 20, WORLD_HEIGHT - 50);
        font.draw(batch, "T: " + (int) runtime, 20, WORLD_HEIGHT - 80);

        if (!started) {
            long now = System.currentTimeMillis();
            long remain = Math.max(0, t0Ms - now);
            font.draw(batch, "Starting in: " + (remain / 1000f), 280, 260);
        }

        batch.end();
    }

    private void update(float delta) {

        // RESULT
        NetThread.ResultInfo result = net.consumeResult();
        if (result != null) {

            int myScore = scoreLocal.getScore();           // <-- ACÁ está el myScore
            boolean iWon = (result.winner == net.getMyId());

            // Opcional: manejar empate
            if (result.winner == 0) {
                // si querés pantalla de empate, podés usar iWon=false y cambiar texto en GameOver
                iWon = false;
            }

            game.setScreen(new GameOverScreen(game, myScore, iWon));
            return;
        }

        long now = System.currentTimeMillis();
        if (!started) {
            if (now >= t0Ms) started = true;
            else return;
        }

        runtime = (now - t0Ms) / 1000f;

        // INPUT local
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) dinoLocal.jump();

        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) dinoLocal.startSlide();
        else dinoLocal.stopSlide();

        // Score local
        scoreLocal.update(delta);

        // dificultad determinista
        gameSpeed = 220f + runtime * 2.5f;

        // mundo
        background.update(delta, gameSpeed * 0.3f);
        ground.update(delta, gameSpeed);

        // actualizar local con física
        dinoLocal.update(delta);

        // rival: solo animación (posición viene por red)
        dinoRival.tickAnimation(delta);

        // aplicar estado rival recibido
        NetThread.RivalState rs = net.getLastRivalState();
        if (rs != null) {
            dinoRival.setY(rs.y);
            dinoRival.setOnGround(rs.onGround);
            dinoRival.setSliding(rs.sliding);
            hpRival = rs.hp;
            scoreRival = rs.score;
        }

        // spawn determinista
        while (runtime >= nextSpawnAt) {
            spawnObstacleDeterministic();
            scheduleNextSpawn(nextSpawnAt);
        }

        // obstaculos + colisiones SOLO contra local
        for (int i = obstacles.size - 1; i >= 0; i--) {
            Obstacle obstacle = obstacles.get(i);
            obstacle.update(delta);

            if (obstacle.getBounds().overlaps(dinoLocal.getBounds())) {
                dinoLocal.damage(1);

                obstacle.dispose();
                obstacles.removeIndex(i);

                if (dinoLocal.isDead()) {
                    net.avisarMuerte(scoreLocal.getScore(), (int) runtime);
                }
                continue;
            }

            if (obstacle.isOffScreen()) {
                obstacle.dispose();
                obstacles.removeIndex(i);
            }
        }

        // enviar estado al server a 20Hz
        stateSendTimer += delta;
        if (stateSendTimer >= STATE_SEND_INTERVAL) {
            stateSendTimer = 0f;

            net.enviarEstadoJugador(
                    dinoLocal.getY(),
                    dinoLocal.isOnGround(),
                    dinoLocal.isSliding(),
                    dinoLocal.getHp(),
                    scoreLocal.getScore()
            );
        }
    }

    private void spawnObstacleDeterministic() {
        if (rng.nextFloat() < 0.7f) obstacles.add(new Cactus(WORLD_WIDTH + 60));
        else obstacles.add(new Bird(WORLD_WIDTH + 60));
    }

    private void scheduleNextSpawn(float fromTime) {
        float min = 0.9f;
        float max = 1.6f;
        float interval = min + rng.nextFloat() * (max - min);
        nextSpawnAt = fromTime + interval;
    }

    @Override public void resize(int width, int height) { viewport.update(width, height); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();

        background.dispose();
        ground.dispose();

        dinoLocal.dispose();
        dinoRival.dispose();

        for (Obstacle o : obstacles) o.dispose();
    }
}
