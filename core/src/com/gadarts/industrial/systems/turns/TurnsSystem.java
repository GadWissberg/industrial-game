package com.gadarts.industrial.systems.turns;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Queue;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.amb.AmbSystemEventsSubscriber;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.systems.enemy.EnemyAiStatus;
import com.gadarts.industrial.systems.enemy.EnemySystemEventsSubscriber;
import com.gadarts.industrial.systems.player.PlayerSystemEventsSubscriber;

public class TurnsSystem extends GameSystem<TurnsSystemEventsSubscriber> implements
		PlayerSystemEventsSubscriber,
		EnemySystemEventsSubscriber,
		AmbSystemEventsSubscriber,
		CharacterSystemEventsSubscriber {
	private boolean currentTurnDone;

	public TurnsSystem(SystemsCommonData systemsCommonData,
					   GameAssetsManager assetsManager,
					   GameLifeCycleHandler lifeCycleHandler) {
		super(systemsCommonData, assetsManager, lifeCycleHandler);
	}

	@Override
	public void update(float deltaTime) {
		if (currentTurnDone) {
			currentTurnDone = false;
			Queue<Entity> turnsQueue = getSystemsCommonData().getTurnsQueue();
			SystemsCommonData systemsCommonData = getSystemsCommonData();
			systemsCommonData.setCurrentTurnId(systemsCommonData.getCurrentTurnId() + 1);
			Entity removeFirst = turnsQueue.removeFirst();
			turnsQueue.addLast(removeFirst);
			subscribers.forEach(s -> s.onNewTurn(turnsQueue.first()));
		}
	}

	@Override
	public void onCharacterDies(Entity character) {
		getSystemsCommonData().getTurnsQueue().removeValue(character, true);
	}

	@Override
	public void onDoorOpened(Entity doorEntity) {
		getSystemsCommonData().getTurnsQueue().addLast(doorEntity);
	}

	@Override
	public void onEnemyFinishedTurn( ) {
		markCurrentTurnAsDone();
	}

	@Override
	public Class<TurnsSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return TurnsSystemEventsSubscriber.class;
	}

	@Override
	public void onPlayerFinishedTurn( ) {
		markCurrentTurnAsDone();
	}

	@Override
	public void onDoorClosed(Entity doorEntity) {
		getSystemsCommonData().getTurnsQueue().removeFirst();
		markCurrentTurnAsDone();
	}

	@Override
	public void onEnemyAwaken(Entity enemy, EnemyAiStatus prevAiStatus) {
		if (prevAiStatus != EnemyAiStatus.IDLE) return;
		Queue<Entity> turnsQueue = getSystemsCommonData().getTurnsQueue();
		turnsQueue.addLast(enemy);
	}

	private void markCurrentTurnAsDone( ) {
		currentTurnDone = true;
	}

	@Override
	public void onDoorStayedOpenInTurn(Entity entity) {
		markCurrentTurnAsDone();
	}

	@Override
	public void initializeData( ) {
		getSystemsCommonData().getTurnsQueue().addFirst(getSystemsCommonData().getPlayer());
	}

	@Override
	public void dispose( ) {

	}

}
