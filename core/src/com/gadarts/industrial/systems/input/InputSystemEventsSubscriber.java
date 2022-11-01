package com.gadarts.industrial.systems.input;

import com.gadarts.industrial.systems.SystemEventsSubscriber;

public interface InputSystemEventsSubscriber extends SystemEventsSubscriber {
	default void mouseMoved(final int screenX, final int screenY) {

	}

	default void touchDown(final int screenX, final int screenY, final int button) {

	}

	default void touchUp(final int screenX, final int screenY, final int button) {

	}

	default void touchDragged(final int screenX, final int screenY) {

	}

	default void keyDown(final int keycode) {

	}

	default void spaceKeyPressed( ) {

	}
}
