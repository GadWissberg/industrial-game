package com.gadarts.industrial.systems.character.actions;

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
import com.gadarts.industrial.shared.model.characters.attributes.Agility;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterCommandContext;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;

import java.util.List;

import static com.gadarts.industrial.map.MapGraphConnectionCosts.CLEAN;
import static com.gadarts.industrial.utils.GameUtils.EPSILON;

public class RunCharacterCommand implements CharacterCommandImplementation {
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final Vector2 auxVector2_2 = new Vector2();
	private static final Vector2 auxVector2_3 = new Vector2();
	private static final float CHARACTER_STEP_SIZE = 0.22f;
	private final static Vector3 auxVector3_1 = new Vector3();
	private MapGraphPath path;
	private MapGraphNode nextNode;

	@Override
	public void initialize(Entity character,
						   SystemsCommonData commonData,
						   Object additionalData,
						   List<CharacterSystemEventsSubscriber> subscribers) {
		Agility agility = ComponentsMapper.character.get(character).getSkills().getAgility();
		path = (MapGraphPath) additionalData;
		Array<MapGraphNode> nodes = path.nodes;
		nodes.removeIndex(0);
		nextNode = nodes.get(0);
		int agilityValue = agility.getValue();
		if (nodes.size > agilityValue) {
			nodes.removeRange(agilityValue, nodes.size - 1);
		}
	}

	private void playStepSound(SystemsCommonData systemsCommonData, Entity character, AtlasRegion newFrame) {
		if (newFrame.index == 0 || newFrame.index == 5) {
			Assets.Sounds stepSound = ComponentsMapper.character.get(character).getSoundData().getStepSound();
			systemsCommonData.getSoundPlayer().playSound(stepSound);
		}
	}

	@Override
	public boolean reactToFrameChange(SystemsCommonData systemsCommonData,
									  Entity character,
									  AtlasRegion newFrame,
									  List<CharacterSystemEventsSubscriber> subscribers,
									  CharacterCommandContext commandContext) {
		playStepSound(systemsCommonData, character, newFrame);
		return applyMovement(systemsCommonData, character, subscribers, commandContext);
	}

	private boolean applyMovement(SystemsCommonData systemsCommonData,
								  Entity character,
								  List<CharacterSystemEventsSubscriber> subscribers,
								  CharacterCommandContext commandContext) {
		Decal decal = ComponentsMapper.characterDecal.get(character).getDecal();
		Vector2 characterPosition = auxVector2_1.set(decal.getX(), decal.getZ());
		if (nextNode == null || characterPosition.dst2(nextNode.getCenterPosition(auxVector2_2)) < EPSILON) {
			return reachedNodeOfPath(character, nextNode, systemsCommonData, subscribers, commandContext);
		} else {
			takeStep(character, systemsCommonData, subscribers);
			return false;
		}
	}

	private void takeStep(Entity entity,
						  SystemsCommonData systemsCommonData,
						  List<CharacterSystemEventsSubscriber> subscribers) {
		CharacterDecalComponent characterDecalComponent = ComponentsMapper.characterDecal.get(entity);
		MapGraph map = systemsCommonData.getMap();
		MapGraphNode oldNode = map.getNode(characterDecalComponent.getNodePosition(auxVector2_3));
		translateCharacter(characterDecalComponent);
		MapGraphNode newNode = map.getNode(characterDecalComponent.getNodePosition(auxVector2_3));
		if (oldNode != newNode) {
			enteredNewNode(entity, oldNode, newNode, subscribers);
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

	private boolean reachedNodeOfPath(Entity character,
									  MapGraphNode node,
									  SystemsCommonData systemsCommonData,
									  List<CharacterSystemEventsSubscriber> subscribers,
									  CharacterCommandContext commandContext) {
		nextNode = path.getNextOf(nextNode);
		commandContext.setDestinationNode(nextNode);
		if (isReachedEndOfPath(systemsCommonData.getMap().findConnection(node, nextNode), systemsCommonData)) {
			return true;
		} else {
			ComponentsMapper.character.get(character).getRotationData().setRotating(true);
			takeStep(character, systemsCommonData, subscribers);
			return false;
		}
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
		Vector2 velocity = auxVector2_2.sub(auxVector2_1.set(decal.getX(), decal.getZ())).nor().scl(CHARACTER_STEP_SIZE);
		decal.translate(auxVector3_1.set(velocity.x, 0, velocity.y));
	}

}
