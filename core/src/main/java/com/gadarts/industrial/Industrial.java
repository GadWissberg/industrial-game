package com.gadarts.industrial;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.gadarts.industrial.screens.InGameScreen;

public class Industrial extends Game {

	public static final int FULL_SCREEN_RESOLUTION_WIDTH = 1920;
	public static final int FULL_SCREEN_RESOLUTION_HEIGHT = 1080;
	public static final int WINDOWED_RESOLUTION_WIDTH = 1280;
	public static final int WINDOWED_RESOLUTION_HEIGHT = 960;
	private final GeneralHandler generalHandler;

	public Industrial(String versionName, int versionNumber) {
		this.generalHandler = new GeneralHandler(versionName, versionNumber);
	}

	@Override
	public void create( ) {
		if (DebugSettings.FULL_SCREEN) {
			Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
		} else {
			Gdx.graphics.setWindowedMode(WINDOWED_RESOLUTION_WIDTH, WINDOWED_RESOLUTION_HEIGHT);
		}
		Gdx.app.setLogLevel(DebugSettings.LOG_LEVEL);
		generalHandler.init();
		setScreen(new InGameScreen(generalHandler));
	}

	@Override
	public void dispose( ) {
		generalHandler.dispose();
	}
}
