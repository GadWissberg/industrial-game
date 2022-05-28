package com.gadarts.industrial.console;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Array;

public class ConsoleInputHistoryHandler {
	private final Array<String> inputHistory = new Array<>();
	private Stage stage;
	private int current;

	public void applyInput(final String inputCommand) {
		inputHistory.insert(inputHistory.size, inputCommand);
		current = inputHistory.size;
	}

	public void onKeyDown(final int keycode) {
		if (keycode == Input.Keys.DOWN) {
			current = Math.min(inputHistory.size - 1, current + 1);
			updateInputByHistory();
		} else if (keycode == Input.Keys.UP) {
			current = Math.max(0, current - 1);
			updateInputByHistory();
		}
	}

	private void updateInputByHistory( ) {
		if (inputHistory.isEmpty()) return;
		TextField input = stage.getRoot().findActor(ConsoleImpl.INPUT_FIELD_NAME);
		input.setText(inputHistory.get(current));
		input.setCursorPosition(input.getText().length());
	}

	public void setStage(final Stage stage) {
		this.stage = stage;
	}
}
