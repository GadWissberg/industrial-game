package com.gadarts.industrial.systems.ui.menu;

import com.badlogic.gdx.graphics.Color;

public interface MenuHandler {
	Color FONT_COLOR_REGULAR = Color.RED;
	Color FONT_COLOR_HOVER = Color.YELLOW;

	void applyMenuOptions(MenuOptionDefinition[] options, boolean clearTableBefore);

	void render(float delta);
}
