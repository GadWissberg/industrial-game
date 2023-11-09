package com.gadarts.industrial.systems.ui.menu;

public interface MenuHandler {

	void applyMenuOptions(MenuOptionDefinition[] options, boolean clearTableBefore);

	void render(float delta);
}
