package com.gadarts.industrial.systems.ui.menu;

import com.badlogic.gdx.Gdx;
import com.gadarts.industrial.components.ComponentsMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MainMenuOptions implements MenuOptionDefinition {
	CONTINUE("Continue",
			(menuHandler, userInterfaceSystemEventsSubscribers) -> menuHandler.toggleMenu(false),
			player -> !ComponentsMapper.player.get(player).isDisabled()),
	NEW("New Game", NewGameMenuOptions.values()),
	LOAD("Load Game"),
	SAVE("Save Game"),
	OPTIONS("Options"),
	INFO("Info"),
	QUIT("Quit", (menuHandler, generalHandler) -> Gdx.app.exit());

	private final String label;
	private final MenuOptionAction action;
	private final MenuOptionValidation validation;
	private final MenuOptionDefinition[] subOptions;

	MainMenuOptions(final String label) {
		this(label, null, player -> true);
	}

	MainMenuOptions(final String label, final MenuOptionAction action) {
		this(label, action, player -> true);
	}

	MainMenuOptions(final String label, final MenuOptionAction action, final MenuOptionValidation validation) {
		this(label, action, validation, null);
	}

	MainMenuOptions(final String label, final NewGameMenuOptions[] subOptions) {
		this(label, null, player -> true, subOptions);
	}
}
