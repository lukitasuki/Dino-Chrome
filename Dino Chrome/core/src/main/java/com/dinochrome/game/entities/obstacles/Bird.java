package com.dinochrome.game.entities.obstacles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dinochrome.game.world.Ground;

public class Bird extends Obstacle {

    private static final float SPEED = 260f;
    private Texture texture;

    public Bird(float startX) {
        super(startX, Ground.GROUND_HEIGHT + 60, SPEED);

        texture = new Texture("bird/bird.png");
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
