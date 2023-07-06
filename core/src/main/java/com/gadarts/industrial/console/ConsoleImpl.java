package com.gadarts.industrial.console;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.StringBuilder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.console.commands.*;
import com.gadarts.industrial.systems.SystemsCommonData;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;


public class ConsoleImpl extends Table implements Console, InputProcessor, Disposable {
	public static final String NAME = "console";
	public static final String INPUT_SIGN = ">";
	public static final char GRAVE_ASCII = '`';
	public static final String OPTIONS_DELIMITER = " | ";
	public static final Interpolation.Pow INTERPOLATION = Interpolation.pow2;
	private static final float PADDING = 10f;
	private static final float INPUT_HEIGHT = 20f;
	private static final float TRANSITION_DURATION = 0.5f;
	private final ConsoleTextures consoleTextures = new ConsoleTextures();
	private final StringBuilder stringBuilder = new StringBuilder();
	private final Set<ConsoleEventsSubscriber> subscribers = new HashSet<>();
	private final ConsoleCommandResult consoleCommandResult = new ConsoleCommandResult();
	private ConsoleTextData consoleTextData;
	private ConsoleInputHistoryHandler consoleInputHistoryHandler;
	private ScrollPane scrollPane;
	private Image arrow;
	private TextField input;
	private boolean active;
	private boolean scrollToEnd = true;

	@Override
	public void dispose( ) {
		consoleTextData.dispose();
		consoleTextures.dispose();
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
		scrollToEnd = true;
		arrow.setVisible(false);
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
		if (scrollToEnd || scrollPane.isBottomEdge()) {
			scrollToEnd = true;
			scrollPane.setScrollPercentY(1);
			arrow.setVisible(false);
		} else if (!arrow.isVisible()) {
			arrow.setVisible(true);
		}
	}

	private void addTextView(final TextureRegionDrawable textBackgroundTexture, final int consoleHeight) {
		Label.LabelStyle textStyle = consoleTextData.getTextStyle();
		textStyle.background = textBackgroundTexture;
		float width = Gdx.graphics.getWidth() - PADDING * 2;
		float height = consoleHeight - (INPUT_HEIGHT);
		Label textView = new Label(consoleTextData.getStringBuilder(), textStyle);
		textView.setAlignment(Align.bottomLeft);
		textView.setName(TEXT_VIEW_NAME);
		textView.setWrap(true);
		scrollPane = new ScrollPane(textView);
		scrollPane.setTouchable(Touchable.disabled);
		Stack textWindowStack = new Stack(scrollPane);
		arrow = new Image(consoleTextures.getArrowTexture());
		arrow.setAlign(Align.bottomRight);
		textWindowStack.add(arrow);
		arrow.setScaling(Scaling.none);
		arrow.setFillParent(false);
		arrow.setVisible(false);
		add(textWindowStack).colspan(2).size(width, height).align(Align.bottomLeft).padRight(PADDING).padLeft(PADDING).row();
	}

	private void addInputField(final TextureRegionDrawable textBackgroundTexture) {
		TextField.TextFieldStyle style = new TextField.TextFieldStyle(consoleTextData.getFont(), Color.YELLOW, new TextureRegionDrawable(consoleTextures.getCursorTexture()),
				null, textBackgroundTexture);
		input = new TextField("", style);
		input.setName(INPUT_FIELD_NAME);
		Label arrow = new Label(INPUT_SIGN, consoleTextData.getTextStyle());
		add(arrow).padBottom(PADDING).padLeft(PADDING).size(10f, INPUT_HEIGHT);
		add(input).size(Gdx.graphics.getWidth() - PADDING * 3, INPUT_HEIGHT).padBottom(PADDING).padRight(PADDING).align(Align.left).row();
		input.setFocusTraversal(false);
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

	private void applyFoundCommandByTab(final java.util.List<ConsoleCommandsList> options) {
		input.setText(options.get(0).name().toLowerCase());
		input.setCursorPosition(input.getText().length());
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
		TextureRegionDrawable textBackgroundTextureRegionDrawable = new TextureRegionDrawable(consoleTextures.getTextBackgroundTexture());
		addTextView(textBackgroundTextureRegionDrawable, (int) height);
		addInputField(textBackgroundTextureRegionDrawable);
		setBackground(new TextureRegionDrawable(consoleTextures.getBackgroundTexture()));
		setSize(Gdx.graphics.getWidth(), consoleTextures.getBackgroundTexture().getHeight());
		consoleInputHistoryHandler = new ConsoleInputHistoryHandler();
		input.setTextFieldFilter((textField, c) -> c != '\t');
		input.setTextFieldListener((textField, c) -> {
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
					applyInput(input, systemsCommonData);
				}
			}
		});
		input.addCaptureListener(new InputListener() {
			@Override
			public boolean keyDown(final InputEvent event, final int keycode) {
				boolean result = false;
				if (active) {
					result = true;
					if (keycode == Input.Keys.PAGE_UP) {
						scroll(-consoleTextData.getFontHeight() * 2);
					} else if (keycode == Input.Keys.PAGE_DOWN) {
						scroll(consoleTextData.getFontHeight() * 2);
					} else if (keycode == Input.Keys.TAB) {
						tryFindingCommand();
					} else if (keycode == Input.Keys.ESCAPE) {
						deactivate();
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
					applyFoundCommandByTab(options);
				} else if (options.size() > 1) {
					logSuggestedCommands(options);
				}
			}

			private void logSuggestedCommands(final java.util.List<ConsoleCommandsList> options) {
				stringBuilder.clear();
				options.forEach(command -> stringBuilder.append(command.name().toLowerCase()).append(OPTIONS_DELIMITER));
				insertNewLog(String.format("Possible options:\n%s", stringBuilder), false);
			}

			private void scroll(final float step) {
				scrollPane.setScrollY(scrollPane.getScrollY() + step);
				scrollToEnd = false;
			}
		});
		subscribers.forEach(sub -> sub.onConsoleInitialized(this));
	}

	@Override
	public void subscribeForEvents(ConsoleEventsSubscriber sub) {
		subscribers.add(sub);
	}
}
