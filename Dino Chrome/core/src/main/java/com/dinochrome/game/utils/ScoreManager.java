package com.dinochrome.game.utils;

public class ScoreManager {

    private int score;
    private float timer;

    public ScoreManager() {
        score = 0;
        timer = 0;
    }

    public void update(float delta) {
        timer += delta;

        if (timer >= 0.1f) {   // suma puntos cada 0.1s
            score += 1;
            timer = 0;
        }
    }

    public int getScore() {
        return score;
    }

    public void reset() {
        score = 0;
        timer = 0;
    }
}
