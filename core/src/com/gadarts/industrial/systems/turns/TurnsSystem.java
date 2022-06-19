package com.gadarts.industrial.systems.turns;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.DefaultGameSettings;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.amb.AmbSystemEventsSubscriber;
import com.gadarts.industrial.systems.enemy.EnemySystemEventsSubscriber;
import com.gadarts.industrial.systems.player.PlayerSystemEventsSubscriber;

import java.util.ArrayList;
import java.util.List;

public class TurnsSystem extends GameSystem<TurnsSystemEventsSubscriber> implements
		PlayerSystemEventsSubscriber,
		EnemySystemEventsSubscriber,
		AmbSystemEventsSubscriber {
	private boolean playerTurnDone;
	private Turns currentTurn;
	private boolean enemyTurnDone;
	private List<Entity> openDoors = new ArrayList<>();
	private com.badlogic.gdx.utils.Queue<Entity> turnsQueue = new com.badlogic.gdx.utils.Queue<>();

	public TurnsSystem(SystemsCommonData systemsCommonData,
					   SoundPlayer soundPlayer,
					   GameAssetsManager assetsManager,
					   GameLifeCycleHandler lifeCycleHandler) {
		super(systemsCommonData, soundPlayer, assetsManager, lifeCycleHandler);
		currentTurn = Turns.PLAYER;
	}

	@Override
	public void onDoorOpened(Entity doorEntity) {
		if (openDoors.contains(doorEntity)) return;
		openDoors.add(doorEntity);
	}

	@Override
	public void onEnemyFinishedTurn( ) {
		enemyTurnDone = true;
	}

	private void resetTurnFlags( ) {
		playerTurnDone = false;
		enemyTurnDone = false;
	}

	private void invokePlayerTurnDone( ) {
		resetTurnFlags();
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		systemsCommonData.setCurrentTurnId(systemsCommonData.getCurrentTurnId() + 1);
		if (!DefaultGameSettings.PARALYZED_ENEMIES) {
			currentTurn = Turns.ENEMY;
			for (TurnsSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onEnemyTurn(systemsCommonData.getCurrentTurnId());
			}
		}
	}

	@Override
	public void update(final float deltaTime) {
		super.update(deltaTime);
		if (currentTurn == Turns.PLAYER && playerTurnDone) {
			invokePlayerTurnDone();
		} else if (currentTurn == Turns.ENEMY && enemyTurnDone) {
			invokeEnemyTurnDone();
		}
	}

	private void invokeEnemyTurnDone( ) {
		resetTurnFlags();
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		systemsCommonData.setCurrentTurnId(systemsCommonData.getCurrentTurnId() + 1);
		currentTurn = Turns.PLAYER;
		for (TurnsSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onPlayerTurn(getSystemsCommonData().getCurrentTurnId());
		}
		for (TurnsSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onPlayerTurn(getSystemsCommonData().getCurrentTurnId());
		}
	}

	@Override
	public Class<TurnsSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return TurnsSystemEventsSubscriber.class;
	}

	@Override
	public void onPlayerFinishedTurn( ) {
		playerTurnDone = true;
	}

	@Override
	public void initializeData( ) {

	}

	@Override
	public void dispose( ) {

	}

}
