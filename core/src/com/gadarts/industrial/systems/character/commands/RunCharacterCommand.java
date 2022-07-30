package com.gadarts.industrial.systems.character.commands;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphConnection;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.map.MapGraphPath;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.model.characters.CharacterTypes;
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
	private static final float MOVEMENT_EPSILON = 0.02F;
	private MapGraphPath path;

	private MapGraphNode nextNode;
	private SystemsCommonData systemsCommonData;

	@Override
	public void initialize(Entity character,
						   SystemsCommonData commonData,
						   Object additionalData,
						   List<CharacterSystemEventsSubscriber> subscribers) {
		systemsCommonData = commonData;
		path = (MapGraphPath) additionalData;
		Array<MapGraphNode> nodes = path.nodes;
		nodes.removeIndex(0);
		nextNode = nodes.get(0);
		int agilityValue = ComponentsMapper.character.get(character).getSkills().getAgility().getValue();
		if (nodes.size > agilityValue) {
			nodes.removeRange(agilityValue, nodes.size - 1);
		}
	}

	@Override
	public boolean reactToFrameChange(SystemsCommonData systemsCommonData,
									  Entity character,
									  AtlasRegion newFrame,
									  List<CharacterSystemEventsSubscriber> subscribers) {
		playStepSound(systemsCommonData, character, newFrame);
		return applyMovement(systemsCommonData, character, subscribers);
	}

	private void playStepSound(SystemsCommonData systemsCommonData, Entity character, AtlasRegion newFrame) {
		if (newFrame.index == 0 || newFrame.index == 5) {
			Assets.Sounds stepSound = ComponentsMapper.character.get(character).getSoundData().getStepSound();
			systemsCommonData.getSoundPlayer().playSound(stepSound);
		}
	}


	private boolean applyMovement(SystemsCommonData systemsCommonData,
								  Entity character,
								  List<CharacterSystemEventsSubscriber> subscribers) {
		Decal decal = ComponentsMapper.characterDecal.get(character).getDecal();
		Vector2 characterPosition = auxVector2_1.set(decal.getX(), decal.getZ());
		Vector2 nextNodeCenterPosition = nextNode.getCenterPosition(auxVector2_2);
		boolean done = false;
		if (nextNode == null || characterPosition.dst2(nextNodeCenterPosition) < MOVEMENT_EPSILON) {
			done = reachedNodeOfPath(nextNode);
		}
		if (!done) {
			takeStep(character, systemsCommonData, subscribers);
		}
		return done;
	}

	private void takeStep(Entity entity,
						  SystemsCommonData systemsCommonData,
						  List<CharacterSystemEventsSubscriber> subscribers) {
		CharacterDecalComponent characterDecalComponent = ComponentsMapper.characterDecal.get(entity);
		MapGraph map = systemsCommonData.getMap();
		MapGraphNode currentNode = map.getNode(characterDecalComponent.getNodePosition(auxVector2_3));
		translateCharacter(characterDecalComponent);
		MapGraphNode newNode = map.getNode(characterDecalComponent.getNodePosition(auxVector2_3));
		if (currentNode != newNode) {
			enteredNewNode(entity, currentNode, newNode, subscribers);
		}
	}

	private void fixHeightPositionOfDecals(final Entity entity, final MapGraphNode newNode) {
		CharacterDecalComponent characterDecalComponent = ComponentsMapper.characterDecal.get(entity);
		Decal decal = characterDecalComponent.getDecal();
		Vector3 position = decal.getPosition();
		float newNodeHeight = newNode.getHeight();
		decal.setPosition(position.x, newNodeHeight + CharacterTypes.BILLBOARD_Y, position.z);
	}

	private void enteredNewNode(Entity entity,
								MapGraphNode oldNode,
								MapGraphNode newNode,
								List<CharacterSystemEventsSubscriber> subscribers) {
		fixHeightPositionOfDecals(entity, newNode);
		for (CharacterSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onCharacterNodeChanged(entity, oldNode, newNode);
		}
	}

	private boolean reachedNodeOfPath(MapGraphNode node) {
		nextNode = path.getNextOf(nextNode);
		setDestinationNode(nextNode);
		return isReachedEndOfPath(systemsCommonData.getMap().findConnection(node, nextNode), systemsCommonData);
	}

	private boolean isReachedEndOfPath(MapGraphConnection connection,
									   SystemsCommonData systemsCommonData) {
		MapGraph map = systemsCommonData.getMap();

		return nextNode == null
				|| connection == null
				|| connection.getCost() != CLEAN.getCostValue()
				|| !map.checkIfNodeIsFreeOfAliveCharactersAndClosedDoors(nextNode, null);
	}

	private void translateCharacter(CharacterDecalComponent characterDecalComponent) {
		Decal decal = characterDecalComponent.getDecal();
		Vector2 velocity = auxVector2_2.sub(auxVector2_1.set(decal.getX(), decal.getZ())).nor().scl(CHAR_STEP_SIZE);
		decal.translate(auxVector3_1.set(velocity.x, 0, velocity.y));
	}

}
