package com.dinochrome.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.dinochrome.game.network.NetThread;
import com.dinochrome.game.screens.LobbyScreen;

public class DinoChromeGame extends Game {

    private Music backgroundMusic;
    private NetThread net;

    @Override
    public void create() {

        // =========================
        // Música de fondo (GLOBAL)
        // =========================
        backgroundMusic = Gdx.audio.newMusic(
                Gdx.files.internal("audio/music/bg_music.mp3")
        );

        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.5f);
        backgroundMusic.play();

        // =========================
        // Red
        // =========================
        net = new NetThread("127.0.0.1", 4321);
        net.start();
        net.conectar("Juan");

        setScreen(new LobbyScreen(this, net));
    }

    public Music getBackgroundMusic() {
        return backgroundMusic;
    }

    public NetThread getNet() {
        return net;
    }

    @Override
    public void pause() {
        super.pause();
        if (backgroundMusic != null) {
            backgroundMusic.pause();
        }
    }

    @Override
    public void resume() {
        super.resume();
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }
    }

    @Override
    public void dispose() {

        if (net != null) {
            net.desconectar();
        }

        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.dispose();
        }

        super.dispose();
    }
}
