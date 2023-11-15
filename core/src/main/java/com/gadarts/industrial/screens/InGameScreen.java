package com.gadarts.industrial.screens;

import com.badlogic.gdx.Screen;
import com.gadarts.industrial.InGameHandler;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.shared.assets.GameAssetManager;

public class InGameScreen implements Screen {
	private final InGameHandler inGameHandler;
	private final GameLifeCycleManager gameLifeCycleManager;

	public InGameScreen(GameLifeCycleManager gameLifeCycleManager,
						String versionName,
						int versionNumber,
						GameAssetManager assetsManager,
						SoundPlayer soundPlayer) {
		this.gameLifeCycleManager = gameLifeCycleManager;
		inGameHandler = new InGameHandler(versionName, versionNumber, assetsManager, soundPlayer);
		inGameHandler.init(gameLifeCycleManager);
	}

	@Override
	public void show( ) {
		inGameHandler.onInGameScreenShow(gameLifeCycleManager);
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
