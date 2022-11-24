package com.gadarts.industrial.systems.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.systems.GameSystem;

public class InputSystem extends GameSystem<InputSystemEventsSubscriber> implements InputProcessor {
	private CameraInputController debugInput;

	public InputSystem(GameAssetsManager assetsManager,
					   GameLifeCycleHandler lifeCycleHandler) {
		super(assetsManager, lifeCycleHandler);
	}

	@Override
	public void reset( ) {
		InputMultiplexer inputMultiplexer = (InputMultiplexer) Gdx.input.getInputProcessor();
		inputMultiplexer.clear();
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		if (debugInput != null) {
			debugInput.update();
		}
	}

	@Override
	public Class<InputSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return InputSystemEventsSubscriber.class;
	}

	@Override
	public void initializeData( ) {
		initializeInputProcessor();
	}

	private void initializeInputProcessor( ) {
		InputProcessor inputProcessor = Gdx.input.getInputProcessor();
		if (inputProcessor == null) {
			createInputProcessor();
		} else {
			clearMultiplexer((InputMultiplexer) inputProcessor);
		}
		addInputProcessor(this);
		addInputProcessor(getSystemsCommonData().getUiStage());
	}

	private void clearMultiplexer(InputMultiplexer inputProcessor) {
		if (!DebugSettings.DEBUG_INPUT) {
			inputProcessor.clear();
		}
	}

	private void createInputProcessor( ) {
		InputProcessor input;
		if (DebugSettings.DEBUG_INPUT) {
			input = createDebugInput();
		} else {
			input = createMultiplexer();
		}
		Gdx.input.setInputProcessor(input);
	}

	private void addInputProcessor(final InputProcessor inputProcessor) {
		if (DebugSettings.DEBUG_INPUT) return;
		InputMultiplexer inputMultiplexer = (InputMultiplexer) Gdx.input.getInputProcessor();
		inputMultiplexer.addProcessor(0, inputProcessor);
	}

	private InputProcessor createMultiplexer( ) {
		InputProcessor input;
		input = new InputMultiplexer();
		return input;
	}

	private InputProcessor createDebugInput( ) {
		InputProcessor input;
		debugInput = new CameraInputController(getSystemsCommonData().getCamera());
		input = debugInput;
		debugInput.autoUpdate = true;
		return input;
	}

	@Override
	public void dispose( ) {

	}

	@Override
	public boolean keyDown(int keycode) {
		if (DebugSettings.DEBUG_INPUT) return false;
		for (InputSystemEventsSubscriber subscriber : subscribers) {
			subscriber.keyDown(keycode);
		}
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		if (!DebugSettings.SPACE_BAR_SKIPS_PLAYER) return false;
		for (InputSystemEventsSubscriber subscriber : subscribers) {
			subscriber.spaceKeyPressed();
		}
		return true;
	}

	@Override
	public boolean touchDown(final int screenX, final int screenY, final int pointer, final int button) {
		if (DebugSettings.DEBUG_INPUT) return false;
		for (InputSystemEventsSubscriber subscriber : subscribers) {
			subscriber.touchDown(screenX, screenY, button);
		}
		return true;
	}

	@Override
	public boolean touchUp(final int screenX, final int screenY, final int pointer, final int button) {
		if (DebugSettings.DEBUG_INPUT) return false;
		for (InputSystemEventsSubscriber subscriber : subscribers) {
			subscriber.touchUp(screenX, screenY, button);
		}
		return true;
	}

	@Override
	public boolean touchDragged(final int screenX, final int screenY, final int pointer) {
		if (DebugSettings.DEBUG_INPUT) return false;
		for (InputSystemEventsSubscriber subscriber : subscribers) {
			subscriber.touchDragged(screenX, screenY);
		}
		return true;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		if (DebugSettings.DEBUG_INPUT) return false;
		for (InputSystemEventsSubscriber subscriber : subscribers) {
			subscriber.mouseMoved(screenX, screenY);
		}
		return true;
	}

	@Override
	public boolean scrolled(float amountX, float amountY) {
		return false;
	}
}
