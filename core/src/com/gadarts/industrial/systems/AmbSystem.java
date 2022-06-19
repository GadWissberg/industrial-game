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
import com.gadarts.industrial.systems.player.PlayerSystemEventsSubscriber;

public class AmbSystem extends GameSystem<SystemEventsSubscriber> implements PlayerSystemEventsSubscriber {
	private static final Vector3 auxVector = new Vector3();
	private ImmutableArray<Entity> doorsEntities;

	public AmbSystem(SystemsCommonData systemsCommonData,
						SoundPlayer soundPlayer,
						GameAssetsManager assetsManager,
						GameLifeCycleHandler lifeCycleHandler) {
		super(systemsCommonData, soundPlayer, assetsManager, lifeCycleHandler);
	}

	@Override
	public void addedToEngine(Engine engine) {
		doorsEntities = engine.getEntitiesFor(Family.all(DoorComponent.class).get());
	}

	@Override
	public Class<SystemEventsSubscriber> getEventsSubscriberClass( ) {
		return null;
	}

	@Override
	public void initializeData( ) {

	}

	@Override
	public void onPlayerPathCreated(MapGraphNode destination) {
		for (Entity doorEntity : doorsEntities) {
			GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(doorEntity).getModelInstance();
			Vector3 position = modelInstance.transform.getTranslation(auxVector);
			MapGraphNode doorNode = getSystemsCommonData().getMap().getNode(position);
			if (doorNode.equals(destination)) {

			}
		}
	}

	@Override
	public void dispose( ) {

	}
}
