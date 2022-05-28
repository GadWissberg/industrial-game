package com.gadarts.industrial.screens;

import com.badlogic.gdx.Screen;
import com.gadarts.industrial.GeneralHandler;

public class BattleScreen implements Screen {
	private final GeneralHandler generalHandler;

	public BattleScreen(GeneralHandler generalHandler) {
		this.generalHandler = generalHandler;
	}

	@Override
	public void show( ) {

	}

	@Override
	public void render(float delta) {
		generalHandler.update(delta);
	}

	@Override
	public void resize(int width, int height) {

	}

	@Override
	public void pause( ) {

	}

	@Override
	public void resume( ) {

	}

	@Override
	public void hide( ) {

	}

	@Override
	public void dispose( ) {
	}
}
