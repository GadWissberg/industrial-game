package com.gadarts.industrial.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;

public class AmbSystem extends GameSystem<SystemEventsSubscriber> implements CharacterSystemEventsSubscriber {
	private static final Vector3 auxVector1 = new Vector3();
	private static final Vector3 auxVector2 = new Vector3();
	private ImmutableArray<Entity> doorEntities;

	public AmbSystem(SystemsCommonData systemsCommonData,
					 SoundPlayer soundPlayer,
					 GameAssetsManager assetsManager,
					 GameLifeCycleHandler lifeCycleHandler) {
		super(systemsCommonData, soundPlayer, assetsManager, lifeCycleHandler);
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
				GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(doorEntity).getModelInstance();
				Vector3 position = modelInstance.transform.getTranslation(auxVector1);
				if (doorComponent.getNode().getCenterPosition(auxVector2).dst2(position) > 0.5F) {
					doorComponent.setState(DoorComponent.DoorStates.OPEN);
				} else {
					modelInstance.transform.translate(0F, 0F, 0.4F * deltaTime);
				}
			}
		}
	}

	@Override
	public Class<SystemEventsSubscriber> getEventsSubscriberClass( ) {
		return null;
	}

	@Override
	public void initializeData( ) {

	}


	@Override
	public void dispose( ) {

	}
}
