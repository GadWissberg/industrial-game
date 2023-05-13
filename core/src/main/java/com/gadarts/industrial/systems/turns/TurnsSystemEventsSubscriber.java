package com.gadarts.industrial.systems.turns;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.systems.SystemEventsSubscriber;

public interface TurnsSystemEventsSubscriber extends SystemEventsSubscriber {
	default void onNewTurn(Entity entity) {

	}

	default void onCombatModeEngaged( ) {

	}

	default void onGameModeSet( ) {

	}
}
