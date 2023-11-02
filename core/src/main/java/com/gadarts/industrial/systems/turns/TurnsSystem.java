package com.gadarts.industrial.systems.turns;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Queue;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.DoorComponent.DoorStates;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.amb.AmbSystemEventsSubscriber;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.systems.enemy.EnemySystemEventsSubscriber;
import com.gadarts.industrial.systems.enemy.ai.EnemyAiStatus;
import com.gadarts.industrial.systems.player.PlayerSystemEventsSubscriber;

public class TurnsSystem extends GameSystem<TurnsSystemEventsSubscriber> implements
		PlayerSystemEventsSubscriber,
		EnemySystemEventsSubscriber,
		AmbSystemEventsSubscriber,
		CharacterSystemEventsSubscriber {
	private boolean currentTurnDone;


	public TurnsSystem(GameAssetManager assetsManager,
					   GameLifeCycleHandler lifeCycleHandler) {
		super(assetsManager, lifeCycleHandler);
	}

	private void decideToRemoveOrAddLast(Queue<Entity> turnsQueue, Entity entity) {
		if (ComponentsMapper.door.has(entity)) {
			if (ComponentsMapper.door.get(entity).getState() != DoorStates.CLOSED) {
				turnsQueue.addLast(entity);
			}
		} else if (!ComponentsMapper.enemy.has(entity) || ComponentsMapper.enemy.get(entity).getAiStatus() != EnemyAiStatus.IDLE) {
			turnsQueue.addLast(entity);
		}
	}

	@Override
	public void update(float deltaTime) {
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		if (systemsCommonData.getCurrentGameMode() != GameMode.EXPLORE && currentTurnDone) {
			Queue<Entity> turnsQueue = getSystemsCommonData().getTurnsQueue();
			systemsCommonData.setCurrentTurnId(systemsCommonData.getCurrentTurnId() + 1);
			Entity removeFirst = turnsQueue.removeFirst();
			decideToRemoveOrAddLast(turnsQueue, removeFirst);
			if (turnsQueue.size == 1) {
				setGameMode(GameMode.EXPLORE);
			}
			startNextTurn();
		}
	}

	private void setGameMode(GameMode gameMode) {
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		if (systemsCommonData.getCurrentGameMode() == gameMode) return;

		systemsCommonData.setCurrentGameMode(gameMode);
		subscribers.forEach(TurnsSystemEventsSubscriber::onGameModeSet);
	}

	private void startNextTurn( ) {
		currentTurnDone = false;
		subscribers.forEach(s -> s.onNewTurn(getSystemsCommonData().getTurnsQueue().first()));
	}

	@Override
	public void onCharacterDies(Entity character) {
		Queue<Entity> turnsQueue = getSystemsCommonData().getTurnsQueue();
		turnsQueue.removeValue(character, true);
		if (turnsQueue.size <= 1) {
			setGameMode(GameMode.EXPLORE);
		}
	}

	@Override
	public void onDoorStateChanged(Entity doorEntity, DoorStates newState) {
		if (newState == DoorStates.OPENING) {
			getSystemsCommonData().getTurnsQueue().addLast(doorEntity);
			GameMode currentGameMode = getSystemsCommonData().getCurrentGameMode();
			if (currentGameMode == GameMode.EXPLORE) {
				setGameMode(GameMode.EXPLORE_TURN_BASED);
			}
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
	public void onCharacterFinishedTurn( ) {
		markCurrentTurnAsDone();
	}

	@Override
	public void onEnemyAwaken(Entity enemy, EnemyAiStatus prevAiStatus, boolean wokeBySpottingPlayer) {
		if (prevAiStatus != EnemyAiStatus.IDLE) return;

		Queue<Entity> turnsQueue = getSystemsCommonData().getTurnsQueue();
		GameMode currentGameMode = getSystemsCommonData().getCurrentGameMode();
		if (currentGameMode != GameMode.COMBAT_MODE) {
			setGameMode(GameMode.COMBAT_MODE);
			turnsQueue.clear();
			turnsQueue.addFirst(wokeBySpottingPlayer ? enemy : getSystemsCommonData().getPlayer());
			turnsQueue.addLast(wokeBySpottingPlayer ? getSystemsCommonData().getPlayer() : enemy);
			currentTurnDone = true;
			subscribers.forEach(TurnsSystemEventsSubscriber::onCombatModeEngaged);
			startNextTurn();
		} else {
			turnsQueue.addLast(enemy);
		}
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
