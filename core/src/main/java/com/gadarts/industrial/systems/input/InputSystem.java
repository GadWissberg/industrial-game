package com.gadarts.industrial.systems.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.screens.GameLifeCycleManager;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.systems.GameSystem;

public class InputSystem extends GameSystem<InputSystemEventsSubscriber> implements InputProcessor {

	public InputSystem(GameAssetManager assetsManager, GameLifeCycleManager gameLifeCycleManager) {
		super(assetsManager, gameLifeCycleManager);
	}

	@Override
	public Class<InputSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return InputSystemEventsSubscriber.class;
	}

	@Override
	public void initializeData( ) {
		initializeInputProcessor();
	}

	@Override
	public void onGameResume( ) {
		super.onGameResume();
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
		inputProcessor.clear();
	}

	private void createInputProcessor( ) {
		InputMultiplexer input;
		input = createMultiplexer();
		Gdx.input.setInputProcessor(input);
	}

	private void addInputProcessor(final InputProcessor inputProcessor) {
		InputMultiplexer inputMultiplexer = (InputMultiplexer) Gdx.input.getInputProcessor();
		inputMultiplexer.addProcessor(0, inputProcessor);
	}

	private InputMultiplexer createMultiplexer( ) {
		InputMultiplexer input;
		input = new InputMultiplexer();
		return input;
	}

	@Override
	public void dispose( ) {

	}

	@Override
	public boolean keyDown(int keycode) {
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

		for (InputSystemEventsSubscriber subscriber : subscribers) {
			subscriber.touchDown(screenX, screenY, button);
		}
		return true;
	}

	@Override
	public boolean touchUp(final int screenX, final int screenY, final int pointer, final int button) {
		for (InputSystemEventsSubscriber subscriber : subscribers) {
			subscriber.touchUp(screenX, screenY, button);
		}
		return true;
	}

	@Override
	public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(final int screenX, final int screenY, final int pointer) {
		for (InputSystemEventsSubscriber subscriber : subscribers) {
			subscriber.touchDragged(screenX, screenY);
		}
		return true;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(float amountX, float amountY) {
		return false;
	}
}
