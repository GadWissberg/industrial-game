package com.gadarts.industrial.systems.ui.menu;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.systems.SystemsCommonData;

public interface MenuHandler {
	void toggleMenu(boolean active);

	void applyMenuOptions(final MenuOptionDefinition[] options);

	void init(Table table, GameAssetsManager assetsManager, SystemsCommonData systemsCommonData, SoundPlayer soundPlayer);
}
