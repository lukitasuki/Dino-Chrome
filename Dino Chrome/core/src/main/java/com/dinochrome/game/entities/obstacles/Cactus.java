package com.dinochrome.game.entities.obstacles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dinochrome.game.world.Ground;

public class Cactus extends Obstacle {

    private static final float SPEED = 220f;
    private Texture texture;

    public Cactus(float startX) {
        super(startX, Ground.GROUND_HEIGHT, SPEED);

        texture = new Texture("cactus/cactus.png");
        bounds.setSize(texture.getWidth(), texture.getHeight());
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.draw(texture, x, y);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
