package com.gadarts.industrial.systems.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Disposable;

public interface MenuHandler extends Disposable {
	Color FONT_COLOR_REGULAR = Color.RED;
	Color FONT_COLOR_HOVER = Color.YELLOW;

	void applyMenuOptions(MenuOptionDefinition[] options);

	void render(float delta);


	void show( );
}
