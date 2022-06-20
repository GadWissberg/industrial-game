package com.gadarts.industrial.systems.turns;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Queue;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.amb.AmbSystemEventsSubscriber;
import com.gadarts.industrial.systems.enemy.EnemySystemEventsSubscriber;
import com.gadarts.industrial.systems.player.PlayerSystemEventsSubscriber;

public class TurnsSystem extends GameSystem<TurnsSystemEventsSubscriber> implements
		PlayerSystemEventsSubscriber,
		EnemySystemEventsSubscriber,
		AmbSystemEventsSubscriber {

	public TurnsSystem(SystemsCommonData systemsCommonData,
					   SoundPlayer soundPlayer,
					   GameAssetsManager assetsManager,
					   GameLifeCycleHandler lifeCycleHandler) {
		super(systemsCommonData, soundPlayer, assetsManager, lifeCycleHandler);
	}

	@Override
	public void onDoorOpened(Entity doorEntity) {
		getSystemsCommonData().getTurnsQueue().addLast(doorEntity);
	}

	@Override
	public void onEnemyFinishedTurn( ) {
		startNewTurn();
	}

	@Override
	public Class<TurnsSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return TurnsSystemEventsSubscriber.class;
	}

	@Override
	public void onPlayerFinishedTurn( ) {
		startNewTurn();
	}

	@Override
	public void onDoorClosed(Entity doorEntity) {
		startNewTurn(false);
	}

	private void startNewTurn( ) {
		startNewTurn(true);
	}

	private void startNewTurn(boolean pushToLast) {
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		systemsCommonData.setCurrentTurnId(systemsCommonData.getCurrentTurnId() + 1);
		Queue<Entity> turnsQueue = getSystemsCommonData().getTurnsQueue();
		Entity removeFirst = turnsQueue.removeFirst();
		if (pushToLast) {
			turnsQueue.addLast(removeFirst);
		}
		subscribers.forEach(s -> s.onNewTurn(turnsQueue.first()));
	}

	@Override
	public void onDoorStayedOpenInTurn(Entity entity) {
		startNewTurn();
	}

	@Override
	public void initializeData( ) {
		getSystemsCommonData().getTurnsQueue().addFirst(getSystemsCommonData().getPlayer());
	}

	@Override
	public void dispose( ) {

	}

}
