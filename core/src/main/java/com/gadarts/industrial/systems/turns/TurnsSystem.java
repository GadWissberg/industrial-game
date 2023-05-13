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
import com.gadarts.industrial.systems.enemy.ai.EnemyAiStatus;
import com.gadarts.industrial.systems.enemy.EnemySystemEventsSubscriber;
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
		if (systemsCommonData.getCurrentGameMode() == GameMode.COMBAT && currentTurnDone) {
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
		getSystemsCommonData().setCurrentGameMode(gameMode);
		subscribers.forEach(sub -> sub.onGameModeSet());
	}

	private void startNextTurn( ) {
		currentTurnDone = false;
		subscribers.forEach(s -> s.onNewTurn(getSystemsCommonData().getTurnsQueue().first()));
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
	public void onCharacterFinishedTurn( ) {
		markCurrentTurnAsDone();
	}

	@Override
	public void onEnemyAwaken(Entity enemy, EnemyAiStatus prevAiStatus, boolean wokeBySpottingPlayer) {
		if (prevAiStatus != EnemyAiStatus.IDLE) return;

		Queue<Entity> turnsQueue = getSystemsCommonData().getTurnsQueue();
		if (getSystemsCommonData().getCurrentGameMode() == GameMode.EXPLORE) {
			engageCombatMode(enemy, wokeBySpottingPlayer, turnsQueue);
		} else {
			turnsQueue.addLast(enemy);
		}
	}

	private void engageCombatMode(Entity enemy, boolean wokeBySpottingPlayer, Queue<Entity> turnsQueue) {
		setGameMode(GameMode.COMBAT);
		turnsQueue.clear();
		turnsQueue.addFirst(wokeBySpottingPlayer ? enemy : getSystemsCommonData().getPlayer());
		turnsQueue.addLast(wokeBySpottingPlayer ? getSystemsCommonData().getPlayer() : enemy);
		currentTurnDone = true;
		subscribers.forEach(TurnsSystemEventsSubscriber::onCombatModeEngaged);
		startNextTurn();
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
