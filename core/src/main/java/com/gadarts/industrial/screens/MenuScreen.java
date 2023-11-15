package com.gadarts.industrial.screens;

import com.badlogic.gdx.Screen;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.systems.ui.menu.MenuHandler;
import com.gadarts.industrial.systems.ui.menu.MenuHandlerImpl;

public class MenuScreen implements Screen {
	private final MenuHandler menuHandler;

	public MenuScreen(GameAssetManager assetsManager,
					  SoundPlayer soundPlayer,
					  String versionName,
					  GameLifeCycleManager gameLifeCycleManager) {
		menuHandler = new MenuHandlerImpl(assetsManager, soundPlayer, versionName, gameLifeCycleManager);
	}

	@Override
	public void show( ) {
		menuHandler.show();
	}

	@Override
	public void render(float delta) {
		menuHandler.render(delta);
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
