package com.gadarts.industrial;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.gadarts.industrial.screens.BattleScreen;

public class Industrial extends Game {
	public static final String TITLE = "PyroShock";

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
		if (DefaultGameSettings.FULL_SCREEN) {
			Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
		} else {
			Gdx.graphics.setWindowedMode(WINDOWED_RESOLUTION_WIDTH, WINDOWED_RESOLUTION_HEIGHT);
		}
		Gdx.app.setLogLevel(DefaultGameSettings.LOG_LEVEL);
		generalHandler.init();
		setScreen(new BattleScreen(generalHandler));
	}

	@Override
	public void dispose( ) {
		generalHandler.dispose();
	}
}
