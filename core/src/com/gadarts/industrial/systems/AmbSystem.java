package com.gadarts.industrial.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.systems.amb.AmbSystemEventsSubscriber;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;

import static com.gadarts.industrial.utils.GameUtils.EPSILON;

public class AmbSystem extends GameSystem<AmbSystemEventsSubscriber> implements CharacterSystemEventsSubscriber {
	private static final Vector3 auxVector1 = new Vector3();
	private static final Vector3 auxVector2 = new Vector3();
	private static final Vector3 auxVector3 = new Vector3();
	private static final Matrix4 auxMatrix = new Matrix4();
	public static final float DOOR_OPEN_OFFSET = 1F;
	public static final float DOOR_MOVEMENT_INTERPOLATION_COEF = 0.03F;
	private ImmutableArray<Entity> doorEntities;

	public AmbSystem(SystemsCommonData systemsCommonData,
					 SoundPlayer soundPlayer,
					 GameAssetsManager assetsManager,
					 GameLifeCycleHandler lifeCycleHandler) {
		super(systemsCommonData, soundPlayer, assetsManager, lifeCycleHandler);
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
			if (doorComponent.getState() == DoorComponent.DoorStates.OPENING) {
				handleDoorOpening(doorEntity, doorComponent);
			}
		}
	}

	private void handleDoorOpening(Entity doorEntity, DoorComponent doorComponent) {
		GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(doorEntity).getModelInstance();
		Vector3 nodeCenterPosition = doorComponent.getNode().getCenterPosition(auxVector2);
		if (nodeCenterPosition.dst2(modelInstance.transform.getTranslation(auxVector1)) > DOOR_OPEN_OFFSET - 3 * EPSILON) {
			openDoor(doorEntity, doorComponent);
		} else {
			modelInstance.transform.lerp(auxMatrix.set(modelInstance.transform)
					.setTranslation(nodeCenterPosition)
					.translate(0F, 0F, DOOR_OPEN_OFFSET), DOOR_MOVEMENT_INTERPOLATION_COEF);
		}
	}

	private void openDoor(Entity doorEntity, DoorComponent doorComponent) {
		doorComponent.setState(DoorComponent.DoorStates.OPEN);
		subscribers.forEach(s -> s.onDoorOpened(doorEntity));
	}


	@Override
	public void initializeData( ) {

	}


	@Override
	public void dispose( ) {

	}
}
