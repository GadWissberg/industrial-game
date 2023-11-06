package com.gadarts.industrial.systems.ui.menu;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetManager;

import java.util.Arrays;


public class MenuHandlerImpl implements MenuHandler, Disposable {

	private final GameAssetManager assetsManager;
	private final Table menuTable;
	private final SoundPlayer soundPlayer;
	private final Stage stage;

	public MenuHandlerImpl(GameAssetManager assetsManager, SoundPlayer soundPlayer) {
		this.assetsManager = assetsManager;
		this.soundPlayer = soundPlayer;
		stage = new Stage();
		Image backgroundImage = new Image(assetsManager.getTexture(Assets.UiTextures.MENU_BACKGROUND));
		stage.addActor(backgroundImage);
		stage.setDebugAll(DebugSettings.DISPLAY_USER_INTERFACE_OUTLINES);
		Image logo = new Image(assetsManager.getTexture(Assets.UiTextures.LOGO));
		menuTable = new Table();
		menuTable.add(logo).row();
		menuTable.setSize(stage.getWidth(), stage.getHeight());
		applyMenuOptions(MainMenuOptions.values(), assetsManager);
		stage.addActor(menuTable);
	}

	@Override
	public void applyMenuOptions(MenuOptionDefinition[] options) {
		menuTable.clear();
		BitmapFont smallFont = assetsManager.getFont(Assets.Fonts.ARIAL_MT_BOLD_SMALL);
		Label.LabelStyle style = new Label.LabelStyle(smallFont, MenuOption.FONT_COLOR_REGULAR);
		Arrays.stream(options).forEach(o -> menuTable.add(new MenuOption(o, style, soundPlayer, this)).row());
	}

	@Override
	public void render(float delta) {
		stage.act(delta);
		stage.draw();
	}

	private void applyMenuOptions(MenuOptionDefinition[] options,
								  GameAssetManager assetsManager) {
		BitmapFont smallFont = assetsManager.getFont(Assets.Fonts.ARIAL_MT_BOLD_SMALL);
		Label.LabelStyle style = new Label.LabelStyle(smallFont, MenuOption.FONT_COLOR_REGULAR);
		Arrays.stream(options).forEach(o -> menuTable.add(new MenuOption(o, style, soundPlayer, this)).row());
	}

	@Override
	public void dispose( ) {
		stage.dispose();
	}
}
