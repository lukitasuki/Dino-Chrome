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

    // Vida
    private static final int MAX_HP = 10;
    private int hp;

    private float hitCooldown;
    private static final float HIT_COOLDOWN_TIME = 0.8f;

    // Texturas para dispose
    private Texture run1Tex;
    private Texture run2Tex;
    private Texture jumpTex;
    private Texture slideTex;

    public Dino(float x, float groundY) {
        this.x = x;
        this.groundY = groundY;
        this.y = groundY;

        this.velocityY = 0;
        this.isOnGround = true;
        this.isSliding = false;

        this.hp = MAX_HP;
        this.hitCooldown = 0f;

        loadAnimations();

        bounds = new Rectangle();
        updateBounds();
    }

    private void loadAnimations() {
        run1Tex = new Texture("dino/run_1.png");
        run2Tex = new Texture("dino/run_2.png");

        runAnimation = new Animation<>(
                0.12f,
                new TextureRegion(run1Tex),
                new TextureRegion(run2Tex)
        );
        runAnimation.setPlayMode(Animation.PlayMode.LOOP);

        jumpTex = new Texture("dino/jump.png");
        slideTex = new Texture("dino/slide.png");

        jumpFrame = new TextureRegion(jumpTex);
        slideFrame = new TextureRegion(slideTex);
    }

    public void update(float delta) {

        if (hitCooldown > 0f) hitCooldown -= delta;

        velocityY -= GRAVITY * delta;
        y += velocityY * delta;

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

        if (!isOnGround) currentFrame = jumpFrame;
        else if (isSliding) currentFrame = slideFrame;
        else currentFrame = runAnimation.getKeyFrame(stateTime);

        batch.draw(currentFrame, x, y);
    }

    private void updateBounds() {
        if (isSliding) bounds.set(x, y, 60, 30);
        else bounds.set(x, y, 44, 60);
    }

    public Rectangle getBounds() { return bounds; }

    // Vida API
    public void damage(int amount) {
        if (amount <= 0) return;
        if (hitCooldown > 0f) return;

        hp -= amount;
        if (hp < 0) hp = 0;

        hitCooldown = HIT_COOLDOWN_TIME;
    }

    public boolean isDead() { return hp <= 0; }
    public int getHp() { return hp; }
    public int getMaxHp() { return MAX_HP; }

    // NUEVO: getters para multiplayer
    public float getX() { return x; }
    public float getY() { return y; }
    public boolean isOnGround() { return isOnGround; }
    public boolean isSliding() { return isSliding; }

    // NUEVO: setters para dino remoto (sin física)
    public void setX(float x) { this.x = x; updateBounds(); }
    public void setY(float y) { this.y = y; updateBounds(); }
    public void setOnGround(boolean onGround) { this.isOnGround = onGround; }
    public void setSliding(boolean sliding) { this.isSliding = sliding; updateBounds(); }

    public void tickAnimation(float delta) {
        // para que el dino remoto “corra” aunque no use update()
        stateTime += delta;
    }

    public void dispose() {
        if (run1Tex != null) run1Tex.dispose();
        if (run2Tex != null) run2Tex.dispose();
        if (jumpTex != null) jumpTex.dispose();
        if (slideTex != null) slideTex.dispose();
    }
}
