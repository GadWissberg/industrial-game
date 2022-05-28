package com.gadarts.industrial.systems.turns;

import com.gadarts.industrial.systems.SystemEventsSubscriber;

public interface TurnsSystemEventsSubscriber extends SystemEventsSubscriber {
	default void onEnemyTurn(final long currentTurnId) {

	}
	default void onPlayerTurn(final long currentTurnId) {

	}
}
