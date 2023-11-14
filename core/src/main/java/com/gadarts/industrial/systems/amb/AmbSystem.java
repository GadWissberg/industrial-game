package com.gadarts.industrial.systems.amb;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.components.DoorComponent.DoorStates;
import com.gadarts.industrial.components.EnvironmentObjectComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.screens.GameLifeCycleManager;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.model.env.door.DoorTypes;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.systems.turns.TurnsSystemEventsSubscriber;

import static com.gadarts.industrial.components.DoorComponent.DoorStates.*;

public class AmbSystem extends GameSystem<AmbSystemEventsSubscriber> implements
		CharacterSystemEventsSubscriber,
		TurnsSystemEventsSubscriber {
	private static final Vector3 auxVector3_1 = new Vector3();
	private ImmutableArray<Entity> doorEntities;
	private ImmutableArray<Entity> environmentObjectsEntities;

	public AmbSystem(GameAssetManager assetsManager, GameLifeCycleManager gameLifeCycleManager) {
		super(assetsManager, gameLifeCycleManager);
	}

	@Override
	public Class<AmbSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return AmbSystemEventsSubscriber.class;
	}

	@Override
	public void initializeData( ) {

	}

	@Override
	public void onSystemReset(SystemsCommonData systemsCommonData) {
		super.onSystemReset(systemsCommonData);
		doorEntities = getEngine().getEntitiesFor(Family.all(DoorComponent.class).get());
		environmentObjectsEntities = getEngine().getEntitiesFor(Family.all(EnvironmentObjectComponent.class).get());
	}

	@Override
	public void update(float deltaTime) {
		updateDoors();
	}

	private void updateDoors( ) {
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
		Vector3 nodeCenterPosition = doorComponent.getNode().getCenterPosition(auxVector3_1);
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

		playDoorSound(doorComponent, newState);
		doorComponent.setState(newState);
		subscribers.forEach(s -> s.onDoorStateChanged(doorEntity, newState));
	}

	private void playDoorSound(DoorComponent doorComponent, DoorStates newState) {
		SoundPlayer soundPlayer = getSystemsCommonData().getSoundPlayer();
		DoorTypes type = doorComponent.getDefinition().getType();
		if (newState == OPENING) {
			soundPlayer.playSound(type.getOpenSound());
		} else if (newState == CLOSING) {
			soundPlayer.playSound(type.getClosedSound());
		}
	}


	@Override
	public void dispose( ) {

	}

	@Override
	public void onNewTurn(Entity entity) {
		if (ComponentsMapper.door.has(entity)) {
			DoorComponent doorComponent = ComponentsMapper.door.get(entity);
			MapGraph map = getSystemsCommonData().getMap();
			if (map.checkIfNodeIsFreeOfCharacters(doorComponent.getNode())) {
				applyDoorState(entity, doorComponent, CLOSING);
			} else {
				subscribers.forEach(sub -> sub.onDoorStayedOpenInTurn(entity));
			}
		}
	}

	@Override
	public void onCharacterNodeChanged(Entity entity, MapGraphNode oldNode, MapGraphNode newNode) {
		if (ComponentsMapper.player.has(entity)) {
			for (Entity environmentObject : environmentObjectsEntities) {
				ModelInstanceComponent modelInstanceComponent = ComponentsMapper.modelInstance.get(environmentObject);
				GameModelInstance modelInstance = modelInstanceComponent.getModelInstance();
				MapGraphNode node = getSystemsCommonData().getMap().getNode(modelInstance.transform.getTranslation(auxVector3_1));
				modelInstanceComponent.setGraySignature(ComponentsMapper.modelInstance.get(node.getEntity()).getGraySignature());
			}
		}
	}


}
