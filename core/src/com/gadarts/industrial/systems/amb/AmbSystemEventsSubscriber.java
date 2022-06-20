package com.gadarts.industrial.systems.amb;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.systems.SystemEventsSubscriber;

public interface AmbSystemEventsSubscriber extends SystemEventsSubscriber {
	default void onDoorOpened(Entity doorEntity) {

	}

	default void onDoorClosed(Entity doorEntity) {

	}

	default void onDoorStayedOpenInTurn(Entity entity){

	}
}
