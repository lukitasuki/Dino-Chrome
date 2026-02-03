package com.dinochrome.game;

import com.badlogic.gdx.Game;
import com.dinochrome.game.screens.MenuScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class DinoChromeGame extends Game {
    @Override
    public void create() {
        setScreen(new MenuScreen(this));
    }
}
