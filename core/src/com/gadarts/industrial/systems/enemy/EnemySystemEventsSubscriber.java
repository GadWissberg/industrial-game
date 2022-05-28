package com.gadarts.industrial.systems.enemy;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.systems.SystemEventsSubscriber;
import com.gadarts.industrial.systems.character.CharacterCommand;

public interface EnemySystemEventsSubscriber extends SystemEventsSubscriber {
	default void onEnemyAwaken(Entity enemy) {

	}

	default void onEnemyFinishedTurn( ) {

	}

	default void onEnemyAppliedCommand(CharacterCommand auxCommand, Entity enemy) {

	}
}
