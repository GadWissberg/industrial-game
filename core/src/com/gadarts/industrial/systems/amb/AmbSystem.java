package com.gadarts.industrial.systems.amb;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.components.DoorComponent.DoorStates;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.shared.model.env.DoorTypes;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.systems.turns.TurnsSystemEventsSubscriber;

import static com.gadarts.industrial.components.DoorComponent.DoorStates.*;

public class AmbSystem extends GameSystem<AmbSystemEventsSubscriber> implements
		CharacterSystemEventsSubscriber,
		TurnsSystemEventsSubscriber {
	private static final Vector3 auxVector1 = new Vector3();
	private static final Vector3 auxVector2 = new Vector3();
	private static final int DOOR_OPEN_DURATION = 3;
	private ImmutableArray<Entity> doorEntities;
	private ImmutableArray<Entity> enemiesEntities;

	public AmbSystem(SystemsCommonData systemsCommonData,
					 GameAssetsManager assetsManager,
					 GameLifeCycleHandler lifeCycleHandler) {
		super(systemsCommonData, assetsManager, lifeCycleHandler);
	}

	@Override
	public Class<AmbSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return AmbSystemEventsSubscriber.class;
	}

	@Override
	public void addedToEngine(Engine engine) {
		doorEntities = engine.getEntitiesFor(Family.all(DoorComponent.class).get());
	}

	@Override
	public void update(float deltaTime) {
		for (Entity doorEntity : doorEntities) {
			DoorComponent doorComponent = ComponentsMapper.door.get(doorEntity);
			DoorStates state = doorComponent.getState();
			if (doorComponent.getOpenRequestor() != null) {
				doorComponent.clearOpenRequestor();
				applyDoorState(doorEntity, doorComponent, OPENING);
			}
			if (state == OPENING || state == CLOSING) {
				handleDoorAction(doorEntity, doorComponent, state == OPENING ? OPEN : CLOSED);
			}
		}
	}

	private void handleDoorAction(Entity doorEntity, DoorComponent doorComponent, DoorStates targetState) {
		GameModelInstance modelInstance = ComponentsMapper.appendixModelInstance.get(doorEntity).getModelInstance();
		Vector3 nodeCenterPosition = doorComponent.getNode().getCenterPosition(auxVector1);
		DoorTypes doorType = doorComponent.getDefinition().getType();
		DoorAnimation doorAnimation = DoorsAnimations.animations.get(doorType);
		if (doorAnimation.isAnimationEnded(targetState, nodeCenterPosition, doorEntity)) {
			applyDoorState(doorEntity, doorComponent, targetState);
		} else {
			doorAnimation.update(modelInstance, nodeCenterPosition, targetState);
		}
	}

	private void applyDoorState(Entity doorEntity, DoorComponent doorComponent, DoorStates newState) {
		DoorStates oldState = doorComponent.getState();
		if (oldState == newState) return;

		doorComponent.setState(newState);
		subscribers.forEach(s -> s.onDoorStateChanged(doorEntity, oldState, newState));
	}


	@Override
	public void initializeData( ) {
		enemiesEntities = getEngine().getEntitiesFor(Family.all(EnemyComponent.class).get());
	}


	@Override
	public void dispose( ) {

	}

	@Override
	public void onNewTurn(Entity entity) {
		if (ComponentsMapper.door.has(entity)) {
			DoorComponent doorComponent = ComponentsMapper.door.get(entity);
			MapGraph map = getSystemsCommonData().getMap();
			if (shouldCloseDoor(doorComponent, map)) {
				closeDoor(doorComponent, entity);
			} else {
				doorComponent.setOpenCounter(doorComponent.getOpenCounter() + 1);
				subscribers.forEach(s -> s.onDoorStayedOpenInTurn(entity));
			}
		}
	}

	private boolean shouldCloseDoor(DoorComponent doorComponent, MapGraph map) {
		int openCounter = doorComponent.getOpenCounter();
		return openCounter >= DOOR_OPEN_DURATION && map.checkIfNodeIsFreeOfCharacters(doorComponent.getNode());
	}

	private void closeDoor(DoorComponent doorComponent, Entity doorEntity) {
		doorComponent.setOpenCounter(0);
		applyDoorState(doorEntity, doorComponent, CLOSING);
	}
}
