package com.dinochrome.game.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Background {

    private Texture texture;
    private float x1, x2;
    private float worldWidth, worldHeight;

    public Background(float worldWidth, float worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;

        texture = new Texture("background/bg.png");

        x1 = 0;
        x2 = worldWidth;
    }

    public void update(float delta, float speed) {
        float movement = speed * 0.3f * delta;

        x1 -= movement;
        x2 -= movement;

        if (x1 + worldWidth <= 0) x1 = x2 + worldWidth;
        if (x2 + worldWidth <= 0) x2 = x1 + worldWidth;
    }

    public void render(SpriteBatch batch) {
        batch.draw(texture, x1, 0, worldWidth, worldHeight);
        batch.draw(texture, x2, 0, worldWidth, worldHeight);
    }

    public void dispose() {
        texture.dispose();
    }
}
