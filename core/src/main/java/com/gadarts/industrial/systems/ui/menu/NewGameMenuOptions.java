package com.gadarts.industrial.systems.ui.menu;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum NewGameMenuOptions implements MenuOptionDefinition {
	OFFICE("Office - Test Map",
			(menuHandler, gameLifeCycleManager) -> gameLifeCycleManager.startNewGame("office")),
	BACK("Back to Main Menu",
			(menuHandler, gameLifeCycleManager) -> menuHandler.applyMenuOptions(MainMenuOptions.values()));
	private final String label;
	private final MenuOptionAction action;

	@Override
	public MenuOptionAction getAction( ) {
		return action;
	}

	@Override
	public MenuOptionDefinition[] getSubOptions( ) {
		return null;
	}

	@Override
	public String getMenuName( ) {
		return "new_game";
	}
}
