package com.gadarts.industrial.systems.turns;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Queue;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.DoorComponent.DoorStates;
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

	private static void decideToRemoveOrAddLast(Queue<Entity> turnsQueue, Entity entity) {
		if (ComponentsMapper.door.has(entity)) {
			if (ComponentsMapper.door.get(entity).getState() != DoorStates.CLOSED) {
				turnsQueue.addLast(entity);
			}
		} else {
			turnsQueue.addLast(entity);
		}
	}

	@Override
	public void update(float deltaTime) {
		if (currentTurnDone) {
			currentTurnDone = false;
			Queue<Entity> turnsQueue = getSystemsCommonData().getTurnsQueue();
			SystemsCommonData systemsCommonData = getSystemsCommonData();
			systemsCommonData.setCurrentTurnId(systemsCommonData.getCurrentTurnId() + 1);
			Entity removeFirst = turnsQueue.removeFirst();
			decideToRemoveOrAddLast(turnsQueue, removeFirst);
			subscribers.forEach(s -> s.onNewTurn(turnsQueue.first()));
		}
	}

	@Override
	public void onCharacterDies(Entity character) {
		getSystemsCommonData().getTurnsQueue().removeValue(character, true);
	}

	@Override
	public void onDoorStateChanged(Entity doorEntity, DoorStates oldState, DoorStates newState) {
		if (newState == DoorStates.OPEN) {
			getSystemsCommonData().getTurnsQueue().addLast(doorEntity);
		} else if (newState == DoorStates.CLOSED) {
			markCurrentTurnAsDone();
		}
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
