package com.gadarts.industrial.screens;

import com.badlogic.gdx.Screen;
import com.gadarts.industrial.InGameHandler;

public class InGameScreen implements Screen {
	private final InGameHandler inGameHandler;

	public InGameScreen(InGameHandler inGameHandler, String mapName, GameLifeCycleManager gameLifeCycleManager) {
		this.inGameHandler = inGameHandler;
		inGameHandler.init(gameLifeCycleManager);
		inGameHandler.startNewGame(mapName, gameLifeCycleManager);
	}

	@Override
	public void show( ) {

	}

	@Override
	public void render(float delta) {
		inGameHandler.update(delta);
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
		inGameHandler.dispose();
	}
}
