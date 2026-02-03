package com.dinochrome.game.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Ground {

    public static final float GROUND_HEIGHT = 50f;

    private Texture texture;
    private float x1, x2;
    private float worldWidth;

    public Ground(float worldWidth) {
        this.worldWidth = worldWidth;

        texture = new Texture("ground/ground.png");

        x1 = 0;
        x2 = worldWidth;
    }

    public void update(float delta, float speed) {
        float movement = speed * delta;

        x1 -= movement;
        x2 -= movement;

        if (x1 + worldWidth <= 0) x1 = x2 + worldWidth;
        if (x2 + worldWidth <= 0) x2 = x1 + worldWidth;
    }

    public void render(SpriteBatch batch) {
        batch.draw(texture, x1, 0, worldWidth, GROUND_HEIGHT);
        batch.draw(texture, x2, 0, worldWidth, GROUND_HEIGHT);
    }

    public void dispose() {
        texture.dispose();
    }
}
