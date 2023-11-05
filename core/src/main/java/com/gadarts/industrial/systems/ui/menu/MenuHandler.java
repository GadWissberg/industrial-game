package com.gadarts.industrial.systems.ui.menu;

public interface MenuHandler {

	void applyMenuOptions(final MenuOptionDefinition[] options);

	void render(float delta);
}
