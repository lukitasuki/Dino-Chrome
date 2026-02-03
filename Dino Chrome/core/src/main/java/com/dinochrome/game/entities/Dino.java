package com.dinochrome.game.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class Dino {

    // Posición
    private float x;
    private float y;

    // Física vertical
    private float velocityY;
    private float groundY;
    private boolean isOnGround;

    // Estados
    private boolean isSliding;

    // Animaciones
    private Animation<TextureRegion> runAnimation;
    private TextureRegion jumpFrame;
    private TextureRegion slideFrame;

    private float stateTime;

    // Colisiones
    private Rectangle bounds;

    // Constantes físicas
    private static final float GRAVITY = 1200f;
    private static final float JUMP_FORCE = 480f;

    public Dino(float x, float groundY) {
        this.x = x;
        this.groundY = groundY;
        this.y = groundY;

        this.velocityY = 0;
        this.isOnGround = true;
        this.isSliding = false;

        loadAnimations();

        bounds = new Rectangle();
        updateBounds();
    }

    private void loadAnimations() {

        Texture run1 = new Texture("dino/run_1.png");
        Texture run2 = new Texture("dino/run_2.png");

        runAnimation = new Animation<>(
                0.12f,
                new TextureRegion(run1),
                new TextureRegion(run2)
        );
        runAnimation.setPlayMode(Animation.PlayMode.LOOP);

        jumpFrame = new TextureRegion(new Texture("dino/jump.png"));
        slideFrame = new TextureRegion(new Texture("dino/slide.png"));
    }

    public void update(float delta) {

        // Gravedad
        velocityY -= GRAVITY * delta;
        y += velocityY * delta;

        // Corrección explícita contra el suelo (CLAVE)
        if (y <= groundY) {
            y = groundY;
            velocityY = 0;
            isOnGround = true;
        }

        stateTime += delta;
        updateBounds();
    }

    public void jump() {
        if (isOnGround) {
            velocityY = JUMP_FORCE;
            isOnGround = false;
        }
    }

    public void startSlide() {
        if (isOnGround) {
            isSliding = true;
            updateBounds();
        }
    }

    public void stopSlide() {
        if (isSliding) {
            isSliding = false;
            updateBounds();
        }
    }

    public void render(SpriteBatch batch) {

        TextureRegion currentFrame;

        if (!isOnGround) {
            currentFrame = jumpFrame;
        } else if (isSliding) {
            currentFrame = slideFrame;
        } else {
            currentFrame = runAnimation.getKeyFrame(stateTime);
        }

        batch.draw(currentFrame, x, y);
    }

    private void updateBounds() {

        if (isSliding) {
            bounds.set(x, y, 60, 30);
        } else {
            bounds.set(x, y, 44, 60);
        }
    }

    public Rectangle getBounds() {
        return bounds;
    }
}
