package com.gadarts.industrial.console;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.gadarts.industrial.console.commands.*;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.systems.SystemsCommonData;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


public class ConsoleImpl extends Table implements Console, InputProcessor, Disposable {
	public static final String NAME = "console";
	public static final char GRAVE_ASCII = '`';
	public static final Interpolation.Pow INTERPOLATION = Interpolation.pow2;
	private static final float TRANSITION_DURATION = 0.5f;
	private final ConsoleTextures consoleTextures = new ConsoleTextures();
	private final ConsoleComponents consoleComponents = new ConsoleComponents(this, consoleTextures);
	private final Set<ConsoleEventsSubscriber> subscribers = new HashSet<>();
	private final ConsoleCommandResult consoleCommandResult = new ConsoleCommandResult();
	private ConsoleTextData consoleTextData;
	private ConsoleInputHistoryHandler consoleInputHistoryHandler;
	private boolean active;

	@Override
	public void dispose( ) {
		consoleTextData.dispose();
		consoleTextures.dispose();
	}

	private TextField.TextFieldListener createTextFieldListener(SystemsCommonData systemsCommonData) {
		return (textField, c) -> {
			if (c == GRAVE_ASCII) {
				textField.setText(null);
				if (!ConsoleImpl.this.hasActions()) {
					if (isActive()) {
						deactivate();
					}
				}
			}
			if (active) {
				if (c == '\r' || c == '\n') {
					applyInput(consoleComponents.getInput(), systemsCommonData);
				}
			}
		};
	}

	@Override
	public boolean keyUp(final int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(final char character) {
		return false;
	}

	@Override
	public boolean touchDown(final int screenX, final int screenY, final int pointer, final int button) {
		return false;
	}

	@Override
	public boolean touchUp(final int screenX, final int screenY, final int pointer, final int button) {
		return false;
	}

	@Override
	public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(final int screenX, final int screenY, final int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(final int screenX, final int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(final float amountX, final float amountY) {
		return false;
	}

	@Override
	public void insertNewLog(String text, boolean logTime) {
		insertNewLog(text, logTime, null);
	}

	@Override
	public boolean keyDown(final int key) {
		boolean result = false;
		if (key == Input.Keys.GRAVE) {
			if (!active) {
				activate();
			}
			result = true;
		}
		return result;
	}

	@Override
	public void insertNewLog(String text, boolean logTime, String color) {
		if (text == null) return;
		consoleTextData.insertNewLog(text, logTime, color);
		consoleComponents.setScrollToEnd(true);
		consoleComponents.showArrow(false);
	}

	@Override
	public ConsoleCommandResult notifyCommandExecution(ConsoleCommands command) {
		return notifyCommandExecution(command, null);
	}

	@Override
	public ConsoleCommandResult notifyCommandExecution(ConsoleCommands command,
													   ConsoleCommandParameter consoleCommandParameter) {
		boolean result = false;
		consoleCommandResult.clear();
		Optional<ConsoleCommandParameter> optional = Optional.ofNullable(consoleCommandParameter);
		for (ConsoleEventsSubscriber sub : subscribers) {
			if (optional.isPresent()) {
				result |= sub.onCommandRun(command, consoleCommandResult, optional.get());
			} else {
				result |= sub.onCommandRun(command, consoleCommandResult);
			}
		}
		consoleCommandResult.setResult(result);
		return consoleCommandResult;
	}

	@Override
	public void activate( ) {
		if (active || !getActions().isEmpty()) return;
		getStage().setKeyboardFocus(getStage().getRoot().findActor(INPUT_FIELD_NAME));
		active = true;
		float amountY = -Gdx.graphics.getHeight() / 3f;
		addAction(Actions.moveBy(0, amountY, TRANSITION_DURATION, INTERPOLATION));
		setVisible(true);
		subscribers.forEach(ConsoleEventsSubscriber::onConsoleActivated);
	}

	@Override
	public void deactivate( ) {
		if (!active || !getActions().isEmpty()) return;
		active = false;
		float amountY = Gdx.graphics.getHeight() / 3f;
		MoveByAction move = Actions.moveBy(0, amountY, TRANSITION_DURATION, Interpolation.pow2);
		addAction(Actions.sequence(move, Actions.visible(false)));
		subscribers.forEach(ConsoleEventsSubscriber::onConsoleDeactivated);
		getStage().unfocusAll();
	}

	@Override
	public boolean isActive( ) {
		return active;
	}

	@Override
	protected void setStage(final Stage stage) {
		super.setStage(stage);
		consoleTextData.setStage(stage);
		consoleInputHistoryHandler.setStage(stage);
	}

	@Override
	public void act(float delta) {
		super.act(delta);
		if (!active) return;
		consoleComponents.update();
	}


	private void applyInput(final TextField textField, SystemsCommonData systemsCommonData) {
		insertNewLog(textField.getText(), true, INPUT_COLOR);
		String inputCommand = textField.getText();
		consoleInputHistoryHandler.applyInput(inputCommand);
		try {
			CommandInvoke commandToInvoke;
			commandToInvoke = parseCommandFromInput(inputCommand);
			ConsoleCommand command = commandToInvoke.getCommand().getCommandImpl();
			ConsoleCommandResult result = command.run(this, commandToInvoke.getParameters(), systemsCommonData);
			insertNewLog(result.getMessage(), false);
		} catch (InputParsingFailureException e) {
			insertNewLog(e.getMessage(), false);
		}
		textField.setText(null);
	}

	private CommandInvoke parseCommandFromInput(final String text) throws InputParsingFailureException {
		if (text == null) return null;
		String[] entries = text.toUpperCase().split(" ");
		String commandName = entries[0];
		CommandInvoke command;
		command = new CommandInvoke(ConsoleCommandsList.findCommandByNameOrAlias(commandName));
		parseParameters(entries, command);
		return command;
	}

	private void parseParameters(final String[] entries, final CommandInvoke command) throws InputParsingFailureException {
		for (int i = 1; i < entries.length; i += 2) {
			String parameter = entries[i];
			if (!parameter.startsWith("-") || parameter.length() < 2)
				throw new InputParsingFailureException(String.format("Failed to apply command! Parameter is expected at" +
						" '%s'", parameter));
			if (i + 1 == entries.length || entries[i + 1].startsWith("-"))
				throw new InputParsingFailureException(String.format("Failed to apply command! Value is expected for " +
						"parameter '%s'", parameter));
			command.addParameter(parameter.substring(1), entries[i + 1]);
		}
	}

	public void applyFoundCommandByTab(final java.util.List<ConsoleCommandsList> options) {
		consoleComponents.applyFoundCommandByTab(options);

	}

	@Override
	public void init(GameAssetManager assetsManager, SystemsCommonData systemsCommonData) {
		consoleTextData = new ConsoleTextData(assetsManager);
		setName(NAME);
		int screenHeight = Gdx.graphics.getHeight();
		float height = screenHeight / 3f;
		setVisible(false);
		setPosition(0, screenHeight);
		consoleTextures.init((int) height);
		TextField.TextFieldListener textFieldListener = createTextFieldListener(systemsCommonData);
		consoleInputHistoryHandler = new ConsoleInputHistoryHandler();
		consoleComponents.init(height, textFieldListener, consoleTextData, consoleInputHistoryHandler, this);
		setBackground(new TextureRegionDrawable(consoleTextures.getBackgroundTexture()));
		setSize(Gdx.graphics.getWidth(), consoleTextures.getBackgroundTexture().getHeight());
		subscribers.forEach(sub -> sub.onConsoleInitialized(this));
	}


	@Override
	public void subscribeForEvents(ConsoleEventsSubscriber sub) {
		subscribers.add(sub);
	}
}
