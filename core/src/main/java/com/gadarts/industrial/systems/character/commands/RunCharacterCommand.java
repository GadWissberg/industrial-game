package com.gadarts.industrial.systems.character.commands;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.components.DoorComponent.DoorStates;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.CharacterSkills;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphConnection;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.model.characters.CharacterTypes;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.systems.turns.GameMode;

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
	private SystemsCommonData systemsCommonData;
	private MapGraphNode prevNode;
	private boolean consumeActionPoints;

	@Override
	public void reset( ) {
		prevNode = null;
	}

	@Override
	public boolean initialize(Entity character,
							  SystemsCommonData commonData) {
		consumeActionPoints = commonData.getCurrentGameMode() != GameMode.EXPLORE;
		systemsCommonData = commonData;
		Array<MapGraphNode> nodes = path.nodes;
		prevNode = nodes.removeIndex(0);
		setNextNodeIndex(0);
		MapGraph map = commonData.getMap();
		return isReachedEndOfPath(map.findConnection(prevNode, path.get(getNextNodeIndex())), map);
	}

	@Override
	public boolean reactToFrameChange(SystemsCommonData systemsCommonData,
									  Entity character,
									  AtlasRegion newFrame,
									  List<CharacterSystemEventsSubscriber> subscribers) {
		if (path.nodes.isEmpty() || ComponentsMapper.character.get(character).getSkills().getActionPoints() <= 0)
			return true;

		return updateCommand(systemsCommonData, character, subscribers);
	}

	private void handleDoor(Entity character, Entity door) {
		DoorComponent doorComponent = ComponentsMapper.door.get(door);
		if (doorComponent.getState() == DoorStates.CLOSED) {
			ComponentsMapper.character.get(character).getCharacterSpriteData().setSpriteType(SpriteType.IDLE);
			doorComponent.requestToOpen(character);
			consumeTurnTime(character, OPEN_DOOR_TIME_CONSUME);
		}
	}

	private boolean updateCommand(SystemsCommonData systemsCommonData,
								  Entity character,
								  List<CharacterSystemEventsSubscriber> subscribers) {
		Decal decal = ComponentsMapper.characterDecal.get(character).getDecal();
		boolean done = false;
		placeCharacterInNextNodeIfCloseEnough(decal);
		Vector2 characterPosition = auxVector2_3.set(decal.getX(), decal.getZ());
		int nextNodeIndex = getNextNodeIndex();
		if (nextNodeIndex == -1 || characterPosition.dst2(path.get(nextNodeIndex).getCenterPosition(auxVector2_2)) < MOVEMENT_EPSILON) {
			done = reachedNodeOfPath(subscribers, character);
		}
		if (!done) {
			done = applyMovementToNextNode(systemsCommonData, character);
		}
		return done;
	}

	private void placeCharacterInNextNodeIfCloseEnough(Decal decal) {
		Vector3 decalPos = decal.getPosition();
		float distanceToNextNode = path.get(getNextNodeIndex()).getCenterPosition(auxVector2_1).dst2(decalPos.x, decalPos.z);
		if (distanceToNextNode < CHAR_STEP_SIZE) {
			placeCharacterInTheNextNode(decal);
		}
	}

	private boolean applyMovementToNextNode(SystemsCommonData systemsCommonData, Entity character) {
		boolean commandDone = false;
		int nextNodeIndex = getNextNodeIndex();
		MapGraphNode nextNode = path.get(nextNodeIndex);
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

	private boolean reachedNodeOfPath(List<CharacterSystemEventsSubscriber> subscribers,
									  Entity character) {
		for (CharacterSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onCharacterNodeChanged(character, prevNode, path.nodes.get(getNextNodeIndex()));
		}
		CharacterSkills skills = ComponentsMapper.character.get(character).getSkills();
		if (consumeActionPoints) {
			skills.setActionPoints(skills.getActionPoints() - 1);
		}
		prevNode = path.get(getNextNodeIndex());
		setNextNodeIndex(getNextNodeIndex() + 1);
		MapGraph map = systemsCommonData.getMap();
		MapGraphNode nextNode = getNextNodeIndex() < path.nodes.size ? path.get(getNextNodeIndex()) : null;
		return isReachedEndOfPath(map.findConnection(prevNode, nextNode), map);
	}


	private boolean isReachedEndOfPath(MapGraphConnection connection, MapGraph map) {
		return ComponentsMapper.character.get(getCharacter()).getSkills().getActionPoints() <= 0
				|| getNextNodeIndex() == -1
				|| connection == null
				|| connection.getCost() != CLEAN.getCostValue()
				|| !map.checkIfNodeIsFreeOfAliveCharacters(path.get(getNextNodeIndex()));
	}

	private void translateCharacter(CharacterDecalComponent characterDecalComponent, SystemsCommonData systemsCommonData) {
		Vector3 decalPos = characterDecalComponent.getDecal().getPosition();
		Entity floorEntity = systemsCommonData.getMap().getNode(decalPos).getEntity();
		Decal decal = characterDecalComponent.getDecal();
		if (floorEntity != null && (ComponentsMapper.floor.get(floorEntity).isRevealed())) {
			Vector2 velocity = auxVector2_2.sub(auxVector2_1.set(decal.getX(), decal.getZ())).nor().scl(CHAR_STEP_SIZE);
			decal.translate(auxVector3_1.set(velocity.x, 0, velocity.y));
		} else {
			placeCharacterInTheNextNode(decal);
		}
	}

	private void placeCharacterInTheNextNode(Decal decal) {
		Vector3 centerPos = path.get(getNextNodeIndex()).getCenterPosition(auxVector3_1);
		decal.setPosition(auxVector3_2.set(centerPos.x, centerPos.y + CharacterTypes.BILLBOARD_Y, centerPos.z));
	}
}
