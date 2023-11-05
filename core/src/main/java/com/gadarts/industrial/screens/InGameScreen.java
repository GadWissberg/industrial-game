package com.gadarts.industrial.screens;

import com.badlogic.gdx.Screen;
import com.gadarts.industrial.GeneralHandler;

public class InGameScreen implements Screen {
	private final GeneralHandler generalHandler;

	public InGameScreen(GeneralHandler generalHandler) {
		this.generalHandler = generalHandler;
		generalHandler.init();
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
		generalHandler.dispose();
	}
}
