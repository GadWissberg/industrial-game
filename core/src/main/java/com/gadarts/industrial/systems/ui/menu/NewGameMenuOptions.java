package com.gadarts.industrial.systems.ui.menu;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum NewGameMenuOptions implements MenuOptionDefinition {
	OFFICE("Office - Test Map");
	private final String label;

	@Override
	public MenuOptionAction getAction( ) {
		return menuHandler -> {

		};
	}

	@Override
	public MenuOptionDefinition[] getSubOptions( ) {
		return null;
	}
}
