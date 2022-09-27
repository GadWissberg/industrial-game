package com.gadarts.industrial.systems.character.commands;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.components.DoorComponent.DoorStates;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphConnection;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.map.MapGraphPath;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.model.characters.CharacterTypes;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;

import java.util.List;

import static com.gadarts.industrial.map.MapGraphConnectionCosts.CLEAN;

public class RunCharacterCommand extends CharacterCommand {
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final Vector2 auxVector2_2 = new Vector2();
	private static final Vector2 auxVector2_3 = new Vector2();
	private static final float CHAR_STEP_SIZE = 0.22f;
	private final static Vector3 auxVector3_1 = new Vector3();
	private final static Vector3 auxVector3_2 = new Vector3();
	private static final float MOVEMENT_EPSILON = 0.02F;
	private static final float OPEN_DOOR_TIME_CONSUME = 1F;
	private final MapGraphPath path = new MapGraphPath();
	private MapGraphNode nextNode;
	private SystemsCommonData systemsCommonData;
	private MapGraphNode prevNode;

	@Override
	public void reset( ) {
		nextNode = null;
		prevNode = null;
	}

	@Override
	public boolean initialize(Entity character,
							  SystemsCommonData commonData,
							  Object additionalData,
							  List<CharacterSystemEventsSubscriber> subscribers) {
		systemsCommonData = commonData;
		path.set((MapGraphPath) additionalData);
		Array<MapGraphNode> nodes = path.nodes;
		prevNode = nodes.removeIndex(0);
		nextNode = nodes.get(0);
		MapGraph map = commonData.getMap();
		return isReachedEndOfPath(map.findConnection(prevNode, nextNode), map);
	}

	@Override
	public void onInFight( ) {
		if (path.nodes.size > 1) {
			path.nodes.removeRange(1, path.nodes.size - 1);
		}
	}

	@Override
	public void free( ) {
		Pools.get(RunCharacterCommand.class).free(this);
	}

	@Override
	public boolean reactToFrameChange(SystemsCommonData systemsCommonData,
									  Entity character,
									  AtlasRegion newFrame,
									  List<CharacterSystemEventsSubscriber> subscribers) {
		if (path.nodes.isEmpty()) return true;

		playStepSound(systemsCommonData, character, newFrame);
		return updateCommand(systemsCommonData, character, subscribers);
	}

	private void handleDoor(Entity character, Entity door) {
		DoorComponent doorComponent = ComponentsMapper.door.get(door);
		if (doorComponent.getState() == DoorStates.CLOSED) {
			ComponentsMapper.character.get(character).getCharacterSpriteData().setSpriteType(SpriteType.IDLE);
			doorComponent.setState(DoorStates.OPENING);
			consumeTurnTime(character, OPEN_DOOR_TIME_CONSUME);
		}
	}

	private void playStepSound(SystemsCommonData systemsCommonData, Entity character, AtlasRegion newFrame) {
		if (newFrame.index == 0 || newFrame.index == 5) {
			Assets.Sounds stepSound = ComponentsMapper.character.get(character).getSoundData().getStepSound();
			systemsCommonData.getSoundPlayer().playSound(stepSound);
		}
	}

	private boolean updateCommand(SystemsCommonData systemsCommonData,
								  Entity character,
								  List<CharacterSystemEventsSubscriber> subscribers) {
		Decal decal = ComponentsMapper.characterDecal.get(character).getDecal();
		Vector2 characterPosition = auxVector2_1.set(decal.getX(), decal.getZ());
		boolean done = false;
		if (nextNode == null || characterPosition.dst2(nextNode.getCenterPosition(auxVector2_2)) < MOVEMENT_EPSILON) {
			done = reachedNodeOfPath(nextNode, subscribers, character);
		}
		if (!done) {
			done = applyMovementToNextNode(systemsCommonData, character);
		}
		return done;
	}

	private boolean applyMovementToNextNode(SystemsCommonData systemsCommonData, Entity character) {
		boolean commandDone = false;
		if (nextNode.getDoor() != null && ComponentsMapper.door.get(nextNode.getDoor()).getState() != DoorStates.OPEN) {
			handleDoor(character, nextNode.getDoor());
			commandDone = true;
		} else {
			takeStep(character, systemsCommonData);
		}
		return commandDone;
	}

	private void takeStep(Entity entity,
						  SystemsCommonData systemsCommonData) {
		CharacterDecalComponent characterDecalComponent = ComponentsMapper.characterDecal.get(entity);
		MapGraph map = systemsCommonData.getMap();
		MapGraphNode currentNode = map.getNode(characterDecalComponent.getNodePosition(auxVector2_3));
		translateCharacter(characterDecalComponent, systemsCommonData);
		MapGraphNode newNode = map.getNode(characterDecalComponent.getNodePosition(auxVector2_3));
		if (currentNode != newNode) {
			fixHeightPositionOfDecals(entity, newNode);
		}
	}

	private void fixHeightPositionOfDecals(final Entity entity, final MapGraphNode newNode) {
		CharacterDecalComponent characterDecalComponent = ComponentsMapper.characterDecal.get(entity);
		Decal decal = characterDecalComponent.getDecal();
		Vector3 position = decal.getPosition();
		float newNodeHeight = newNode.getHeight();
		decal.setPosition(position.x, newNodeHeight + CharacterTypes.BILLBOARD_Y, position.z);
	}

	private boolean reachedNodeOfPath(MapGraphNode node,
									  List<CharacterSystemEventsSubscriber> subscribers,
									  Entity character) {
		for (CharacterSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onCharacterNodeChanged(character, prevNode, nextNode);
		}
		prevNode = nextNode;
		nextNode = path.getNextOf(nextNode);
		setDestinationNode(nextNode);
		consumeTurnTime(character, ComponentsMapper.character.get(character).getSkills().getAgility());
		MapGraph map = systemsCommonData.getMap();
		return isReachedEndOfPath(map.findConnection(node, nextNode), map);
	}


	private boolean isReachedEndOfPath(MapGraphConnection connection, MapGraph map) {
		return nextNode == null
				|| connection == null
				|| connection.getCost() != CLEAN.getCostValue()
				|| !map.checkIfNodeIsFreeOfAliveCharacters(nextNode);
	}

	private void translateCharacter(CharacterDecalComponent characterDecalComponent, SystemsCommonData systemsCommonData) {
		Vector3 decalPos = characterDecalComponent.getDecal().getPosition();
		Entity floorEntity = systemsCommonData.getMap().getNode(decalPos).getEntity();
		Decal decal = characterDecalComponent.getDecal();
		if (floorEntity != null && (ComponentsMapper.floor.get(floorEntity).getFogOfWarSignature() & 16) == 0) {
			Vector2 velocity = auxVector2_2.sub(auxVector2_1.set(decal.getX(), decal.getZ())).nor().scl(CHAR_STEP_SIZE);
			decal.translate(auxVector3_1.set(velocity.x, 0, velocity.y));
		} else {
			Vector3 centerPos = nextNode.getCenterPosition(auxVector3_1);
			decal.setPosition(auxVector3_2.set(centerPos.x, centerPos.y + CharacterTypes.BILLBOARD_Y, centerPos.z));
		}
	}
}
