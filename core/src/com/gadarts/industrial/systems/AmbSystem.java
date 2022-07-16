package com.gadarts.industrial.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.components.DoorComponent.DoorStates;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.systems.amb.AmbSystemEventsSubscriber;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.systems.turns.TurnsSystemEventsSubscriber;

import static com.gadarts.industrial.components.DoorComponent.DoorStates.*;
import static com.gadarts.industrial.utils.GameUtils.EPSILON;

public class AmbSystem extends GameSystem<AmbSystemEventsSubscriber> implements CharacterSystemEventsSubscriber, TurnsSystemEventsSubscriber {
	private static final Vector3 auxVector1 = new Vector3();
	private static final Vector3 auxVector2 = new Vector3();
	private static final Matrix4 auxMatrix = new Matrix4();
	public static final float DOOR_OPEN_OFFSET = 1F;
	public static final float DOOR_MOVE_INTER_COEF = 0.03F;
	private static final int DOOR_OPEN_DURATION = 1;
	private static final float DOOR_OPEN_OFFSET_EPSILON = 3 * EPSILON;
	private static final float DOOR_CLOSED_OFFSET_EPSILON = 0.003f;
	private ImmutableArray<Entity> doorEntities;

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
	public void onCharacterOpenedDoor(MapGraphNode doorNode) {
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
			if (state == OPENING || state == CLOSING) {
				handleDoorAction(doorEntity, doorComponent, state == OPENING ? OPEN : CLOSED);
			}
		}
	}

	private void handleDoorAction(Entity doorEntity, DoorComponent doorComponent, DoorStates targetState) {
		GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(doorEntity).getModelInstance();
		Vector3 nodeCenterPosition = doorComponent.getNode().getCenterPosition(auxVector2);
		float distance = nodeCenterPosition.dst2(modelInstance.transform.getTranslation(auxVector1));
		boolean farEnough = distance > DOOR_OPEN_OFFSET - DOOR_OPEN_OFFSET_EPSILON;
		boolean closeEnough = distance < DOOR_CLOSED_OFFSET_EPSILON;

		if ((targetState == OPEN && farEnough) || (targetState == CLOSED && closeEnough)) {
			applyDoorState(doorEntity, doorComponent, targetState);
		} else {
			modelInstance.transform.lerp(
					auxMatrix.set(modelInstance.transform)
							.setTranslation(nodeCenterPosition)
							.translate(0F, 0F, (targetState == OPEN ? 1 : -1) * DOOR_OPEN_OFFSET),
					DOOR_MOVE_INTER_COEF);
		}
	}

	private void applyDoorState(Entity doorEntity, DoorComponent doorComponent, DoorStates state) {
		doorComponent.setState(state);
		subscribers.forEach(state == OPEN ? s -> s.onDoorOpened(doorEntity) : s -> s.onDoorClosed(doorEntity));
	}


	@Override
	public void initializeData( ) {

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
				closeDoor(doorComponent);
			} else {
				doorComponent.setOpenCounter(doorComponent.getOpenCounter() + 1);
				subscribers.forEach(s -> s.onDoorStayedOpenInTurn(entity));
			}
		}
	}

	private boolean shouldCloseDoor(DoorComponent doorComponent, MapGraph map) {
		return doorComponent.getOpenCounter() >= DOOR_OPEN_DURATION
				&& map.checkIfNodeIsFreeOfAliveCharactersAndClosedDoors(doorComponent.getNode());
	}

	private void closeDoor(DoorComponent doorComponent) {
		doorComponent.setOpenCounter(0);
		doorComponent.setState(CLOSING);
	}
}
