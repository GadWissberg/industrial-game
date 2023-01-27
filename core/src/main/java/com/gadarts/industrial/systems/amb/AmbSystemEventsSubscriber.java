package com.gadarts.industrial.systems.amb;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.systems.SystemEventsSubscriber;

public interface AmbSystemEventsSubscriber extends SystemEventsSubscriber {

	default void onDoorStayedOpenInTurn(Entity entity) {

	}

	default void onDoorStateChanged(Entity doorEntity,
									DoorComponent.DoorStates oldState,
									DoorComponent.DoorStates newState) {

	}
}
