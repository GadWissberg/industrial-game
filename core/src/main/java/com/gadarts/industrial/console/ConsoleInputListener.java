package com.gadarts.industrial.console;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.StringBuilder;
import com.gadarts.industrial.console.commands.ConsoleCommandsList;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class ConsoleInputListener extends InputListener {
	private static final String OPTIONS_DELIMITER = " | ";

	private final Console console;
	private final ConsoleTextData consoleTextData;
	private final ConsoleInputHistoryHandler consoleInputHistoryHandler;
	private final TextField input;
	private final StringBuilder stringBuilder;
	private final ConsoleScrollPane scrollPane;


	@Override
	public boolean keyDown(final InputEvent event, final int keycode) {
		boolean result = false;
		if (console.isActive()) {
			result = true;
			if (keycode == Input.Keys.PAGE_UP) {
				scroll(-consoleTextData.getFontHeight() * 2);
			} else if (keycode == Input.Keys.PAGE_DOWN) {
				scroll(consoleTextData.getFontHeight() * 2);
			} else if (keycode == Input.Keys.TAB) {
				tryFindingCommand();
			} else if (keycode == Input.Keys.ESCAPE) {
				console.deactivate();
			} else {
				consoleInputHistoryHandler.onKeyDown(keycode);
			}
		}
		return result;
	}

	private void tryFindingCommand( ) {
		if (input.getText().isEmpty()) return;
		java.util.List<ConsoleCommandsList> options = Arrays.stream(ConsoleCommandsList.values())
				.filter(command -> {
					String p = input.getText().toUpperCase();
					String alias = command.getAlias();
					return command.name().startsWith(p) || ((alias != null && alias.startsWith(p.toLowerCase())));
				})
				.collect(toList());
		if (options.size() == 1) {
			console.applyFoundCommandByTab(options);
		} else if (options.size() > 1) {
			logSuggestedCommands(options);
		}
	}

	private void logSuggestedCommands(final java.util.List<ConsoleCommandsList> options) {
		stringBuilder.clear();
		options.forEach(command -> stringBuilder.append(command.name().toLowerCase()).append(OPTIONS_DELIMITER));
		console.insertNewLog(String.format("Possible options:\n%s", stringBuilder), false);
	}

	private void scroll(final float step) {
		scrollPane.setScrollY(scrollPane.getScrollY() + step);
		scrollPane.setScrollToEnd(false);
	}
}
