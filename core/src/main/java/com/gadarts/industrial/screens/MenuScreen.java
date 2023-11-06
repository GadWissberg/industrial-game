package com.gadarts.industrial.screens;

import com.badlogic.gdx.Screen;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.systems.ui.menu.MenuHandler;
import com.gadarts.industrial.systems.ui.menu.MenuHandlerImpl;
import com.gadarts.industrial.utils.GameUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MenuScreen implements Screen {
	private final GameAssetManager assetsManager;
	private final SoundPlayer soundPlayer;
	private MenuHandler menuHandler;


	@Override
	public void show( ) {
		menuHandler = new MenuHandlerImpl(assetsManager, soundPlayer);
	}

	@Override
	public void render(float delta) {
		GameUtils.clearDisplay(1F);
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