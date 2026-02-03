package com.dinochrome.game.entities.obstacles;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public abstract class Obstacle {

    protected float x, y;
    protected float speed;
    protected Rectangle bounds;

    public Obstacle(float x, float y, float speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.bounds = new Rectangle();
    }

    public void update(float delta) {
        x -= speed * delta;
        bounds.setPosition(x, y);
    }

    public boolean isOffScreen() {
        return x + bounds.width < 0;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public abstract void render(SpriteBatch batch);
    public abstract void dispose();
}
