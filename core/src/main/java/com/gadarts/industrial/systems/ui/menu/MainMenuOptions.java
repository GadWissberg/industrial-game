package com.gadarts.industrial.systems.ui.menu;

import com.badlogic.gdx.Gdx;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MainMenuOptions implements MenuOptionDefinition {
	CONTINUE("Continue", (menuHandler) -> {
	}),
	NEW("New Game", NewGameMenuOptions.values()),
	LOAD("Load Game"),
	SAVE("Save Game"),
	OPTIONS("Options"),
	INFO("Info"),
	QUIT("Quit", (menuHandler) -> Gdx.app.exit());

	private final String label;
	private final MenuOptionAction action;
	private final MenuOptionDefinition[] subOptions;

	MainMenuOptions(final String label) {
		this(label, (MenuOptionAction) null);
	}

	MainMenuOptions(final String label, final MenuOptionAction action) {
		this(label, action, null);
	}


	MainMenuOptions(final String label, final NewGameMenuOptions[] subOptions) {
		this(label, null, subOptions);
	}
}
