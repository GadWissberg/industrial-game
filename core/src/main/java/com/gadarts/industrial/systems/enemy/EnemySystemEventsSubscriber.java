package com.gadarts.industrial.systems.enemy;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.systems.SystemEventsSubscriber;
import com.gadarts.industrial.systems.enemy.ai.EnemyAiStatus;

public interface EnemySystemEventsSubscriber extends SystemEventsSubscriber {
	default void onEnemyAwaken(Entity enemy, EnemyAiStatus prevAiStatus, boolean wokeBySpottingPlayer) {

	}

	default void onEnemyFinishedTurn( ) {

	}


	default void onEnemyAiStatusChange(Entity enemy, EnemyAiStatus enemyAiStatus){

	}
}
