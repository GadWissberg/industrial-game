package com.gadarts.industrial.systems.character.commands;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
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
import com.gadarts.industrial.systems.enemy.EnemyAiStatus;

import java.util.List;

import static com.gadarts.industrial.map.MapGraphConnectionCosts.CLEAN;

public class RunCharacterCommand extends CharacterCommand {
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final Vector2 auxVector2_2 = new Vector2();
	private static final Vector2 auxVector2_3 = new Vector2();
	private static final float CHAR_STEP_SIZE = 0.22f;
	private final static Vector3 auxVector3_1 = new Vector3();
	private static final float MOVEMENT_EPSILON = 0.02F;
	private MapGraphPath path = new MapGraphPath();
	private MapGraphNode nextNode;
	private SystemsCommonData systemsCommonData;
	private MapGraphNode prevNode;

	@Override
	public void reset( ) {
		nextNode = null;
		prevNode = null;
	}

	@Override
	public void initialize(Entity character,
						   SystemsCommonData commonData,
						   Object additionalData,
						   List<CharacterSystemEventsSubscriber> subscribers) {
		systemsCommonData = commonData;
		path.set((MapGraphPath) additionalData);
		Array<MapGraphNode> nodes = path.nodes;
		prevNode = nodes.removeIndex(0);
		nextNode = nodes.get(0);
	}

	@Override
	public boolean isNewTurnOnCompletion( ) {
		return false;
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
		if (path == null || path.nodes.isEmpty()) return true;

		playStepSound(systemsCommonData, character, newFrame);
		return applyMovement(systemsCommonData, character, subscribers);
	}

	@Override
	public void onEnemyAwaken(Entity enemy, EnemyAiStatus prevAiStatus) {
		if (path == null || path.nodes.isEmpty()) return;

		int nextNodeIndex = path.nodes.indexOf(nextNode, true);
		if (nextNodeIndex < path.nodes.size - 1) {
			path.nodes.removeRange(nextNodeIndex + 1, path.nodes.size - 1);
		}
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
		boolean done = false;
		if (nextNode == null || characterPosition.dst2(nextNode.getCenterPosition(auxVector2_2)) < MOVEMENT_EPSILON) {
			done = reachedNodeOfPath(nextNode, subscribers, character);
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
