package com.gadarts.industrial.systems.ui.menu;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.DefaultGameSettings;
import com.gadarts.industrial.Industrial;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.ui.GameStage;
import com.gadarts.industrial.systems.ui.UserInterfaceSystemEventsSubscriber;

import java.util.Arrays;
import java.util.List;

import static com.gadarts.industrial.systems.SystemsCommonData.TABLE_NAME_HUD;


public class MenuHandlerImpl implements MenuHandler {
	private static final String TABLE_NAME_MENU = "menu";

	private final SystemsCommonData systemsCommonData;

	private final List<UserInterfaceSystemEventsSubscriber> subscribers;
	private final GameAssetsManager assetsManager;
	private final SoundPlayer soundPlayer;

	public MenuHandlerImpl(SystemsCommonData systemsCommonData,
						   List<UserInterfaceSystemEventsSubscriber> subscribers,
						   GameAssetsManager assetsManager, SoundPlayer soundPlayer) {
		this.systemsCommonData = systemsCommonData;
		this.subscribers = subscribers;
		this.assetsManager = assetsManager;
		this.soundPlayer = soundPlayer;
	}

	public void toggleMenu(final boolean active) {
		toggleMenu(active, systemsCommonData.getUiStage());
		subscribers.forEach(subscriber -> subscriber.onMenuToggled(active));
	}

	@Override
	public void applyMenuOptions(MenuOptionDefinition[] options) {
		Table menuTable = systemsCommonData.getMenuTable();
		menuTable.clear();
		BitmapFont smallFont = assetsManager.getFont(Assets.Fonts.CHUBGOTHIC_SMALL);
		Label.LabelStyle style = new Label.LabelStyle(smallFont, MenuOption.FONT_COLOR_REGULAR);
		Arrays.stream(options).forEach(o -> {
			if (o.getValidation().validate(systemsCommonData.getPlayer())) {
				menuTable.add(new MenuOption(o, style, soundPlayer, this, subscribers)).row();
			}
		});
	}

	@Override
	public void init(Table table,
					 GameAssetsManager assetsManager,
					 SystemsCommonData systemsCommonData,
					 SoundPlayer soundPlayer) {
		addMenuTable(table, assetsManager, systemsCommonData, soundPlayer);
	}

	private Label createLogo(GameAssetsManager assetsManager) {
		BitmapFont largeFont = assetsManager.getFont(Assets.Fonts.CHUBGOTHIC_LARGE);
		Label.LabelStyle logoStyle = new Label.LabelStyle(largeFont, MenuOption.FONT_COLOR_REGULAR);
		return new Label(Industrial.TITLE, logoStyle);
	}

	private void addMenuTable(Table table,
							  GameAssetsManager assetsManager,
							  SystemsCommonData systemsCommonData,
							  SoundPlayer soundPlayer) {
		systemsCommonData.setMenuTable(table);
		table.setName(TABLE_NAME_MENU);
		table.add(createLogo(assetsManager)).row();
		applyMenuOptions(MainMenuOptions.values(), assetsManager, systemsCommonData, soundPlayer);
		table.toFront();
		toggleMenu(DefaultGameSettings.MENU_ON_STARTUP);
	}

	public void applyMenuOptions(MenuOptionDefinition[] options,
								 GameAssetsManager assetsManager,
								 SystemsCommonData commonData,
								 SoundPlayer soundPlayer) {
		commonData.getMenuTable().clear();
		BitmapFont smallFont = assetsManager.getFont(Assets.Fonts.CHUBGOTHIC_SMALL);
		Label.LabelStyle style = new Label.LabelStyle(smallFont, MenuOption.FONT_COLOR_REGULAR);
		Arrays.stream(options).forEach(o -> {
			if (o.getValidation().validate(commonData.getPlayer())) {
				commonData.getMenuTable().add(new MenuOption(o, style, soundPlayer, this, subscribers)).row();
			}
		});
	}

	public void toggleMenu(final boolean active, final GameStage stage) {
		systemsCommonData.getMenuTable().setVisible(active);
		stage.getRoot().findActor(TABLE_NAME_HUD).setTouchable(active ? Touchable.disabled : Touchable.enabled);
	}
}
