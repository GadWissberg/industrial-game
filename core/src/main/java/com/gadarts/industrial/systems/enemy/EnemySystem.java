package com.gadarts.industrial.systems.enemy;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pools;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.sd.SimpleDecalComponent;
import com.gadarts.industrial.map.CalculatePathRequest;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphConnectionCosts;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.assets.declarations.weapons.WeaponDeclaration;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.ModelInstancePools;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.amb.AmbSystemEventsSubscriber;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.systems.character.commands.CharacterCommand;
import com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.player.PathPlanHandler;
import com.gadarts.industrial.systems.render.RenderSystemEventsSubscriber;
import com.gadarts.industrial.systems.turns.TurnsSystemEventsSubscriber;
import com.gadarts.industrial.utils.EntityBuilder;
import com.gadarts.industrial.utils.GameUtils;

import java.util.LinkedHashSet;

import static com.gadarts.industrial.DebugSettings.PARALYZED_ENEMIES;
import static com.gadarts.industrial.components.character.CharacterComponent.TURN_DURATION;
import static com.gadarts.industrial.map.MapGraphConnectionCosts.CLEAN;
import static com.gadarts.industrial.systems.enemy.EnemyAiStatus.*;

public class EnemySystem extends GameSystem<EnemySystemEventsSubscriber> implements
		CharacterSystemEventsSubscriber,
		TurnsSystemEventsSubscriber,
		RenderSystemEventsSubscriber,
		AmbSystemEventsSubscriber {
	private final static Vector2 auxVector2_1 = new Vector2();
	private final static Vector2 auxVector2_2 = new Vector2();
	private static final float ENEMY_HALF_FOV_ANGLE = 95F;
	private static final float METAL_PART_FLY_AWAY_STRENGTH = 0.2F;
	private static final float METAL_PART_FLY_AWAY_MIN_DEGREE = -45F;
	private static final float METAL_PART_FLY_AWAY_MAX_DEGREE_TO_ADD = -90F;
	private static final float METAL_PART_FLY_AWAY_MIN_DEC = 0.9F;
	private static final float METAL_PART_FLY_AWAY_MAX_DEC = 0.97F;
	private static final int MIN_METAL_PARTS_TO_SPAWN = 2;
	private static final int MAX_METAL_PARTS_TO_SPAWN = 5;
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final float SMOKE_HEIGHT_BIAS = 0.4F;
	private final static LinkedHashSet<GridPoint2> bresenhamOutput = new LinkedHashSet<>();
	private static final CalculatePathRequest request = new CalculatePathRequest();
	private PathPlanHandler pathPlanner;
	private ImmutableArray<Entity> enemies;
	private ParticleEffect smokeEffect;

	public EnemySystem(GameAssetManager assetsManager, GameLifeCycleHandler lifeCycleHandler) {
		super(assetsManager, lifeCycleHandler);
	}


	private static void consumeEngineEnergy(Entity character) {
		EnemyComponent enemyComp = ComponentsMapper.enemy.get(character);
		WeaponDeclaration primaryAttack = enemyComp.getEnemyDeclaration().attackPrimary();
		enemyComp.setEngineEnergy(Math.max(enemyComp.getEngineEnergy() - primaryAttack.engineConsumption(), 0));
	}

	private static boolean checkIfFloorNodeBlockSightToTarget(Vector2 enemyPosition,
															  MapGraph map,
															  MapGraphNode node,
															  Entity door) {
		return node.getHeight() > map.getNode((int) enemyPosition.x, (int) enemyPosition.y).getHeight() + 1
				|| (door != null && ComponentsMapper.door.get(door).getState() == DoorComponent.DoorStates.CLOSED);
	}

	@Override
	public void onSpriteTypeChanged(Entity entity, SpriteType spriteType) {
		if (ComponentsMapper.enemy.has(entity) && spriteType == SpriteType.ATTACK_PRIMARY) {
			consumeEngineEnergy(entity);
		}
	}

	@Override
	public void onCharacterCommandDone(final Entity character) {
		if (ComponentsMapper.enemy.has(character)) {
			if (ComponentsMapper.character.get(character).getSkills().getActionPoints() <= 0) {
				subscribers.forEach(EnemySystemEventsSubscriber::onEnemyFinishedTurn);
			}
		}
	}

	@Override
	public void onDestinationReached(Entity character) {
		CharacterSystemEventsSubscriber.super.onDestinationReached(character);
	}

	boolean checkIfWayIsClearToTarget(final Entity enemy) {
		LinkedHashSet<GridPoint2> nodes = GameUtils.findAllNodesToTarget(enemy, bresenhamOutput, true);
		boolean blocked = checkIfFloorNodesBlockSightToTarget(enemy, nodes);
		if (!blocked) {
			blocked = checkIfFloorNodesContainObjects(nodes);
		}
		return !blocked;
	}

	private boolean checkIfFloorNodesContainObjects(LinkedHashSet<GridPoint2> nodes) {
		boolean result = false;
		for (GridPoint2 point : nodes) {
			MapGraph map = getSystemsCommonData().getMap();
			if (!map.checkIfNodeIsFreeOfAliveCharactersAndClosedDoors(point) || !map.checkIfNodeIsFreeOfEnvObjects(point)) {
				result = true;
				break;
			}
		}
		return result;
	}

	@Override
	public void onCharacterGotDamage(final Entity entity) {
		if (ComponentsMapper.enemy.has(entity)) {
			EnemyComponent enemyComponent = ComponentsMapper.enemy.get(entity);
			if (enemyComponent.getAiStatus() != ATTACKING) {
				awakeEnemy(entity);
			}
			if (!enemyComponent.getEnemyDeclaration().human()) {
				createFlyingMetalParts(entity);
			}
		}
	}

	private void createFlyingMetalParts(Entity entity) {
		for (int i = 0; i < MathUtils.random(MIN_METAL_PARTS_TO_SPAWN, MAX_METAL_PARTS_TO_SPAWN); i++) {
			generateFlyingMetalPart(entity);
		}
	}

	private void generateFlyingMetalPart(Entity entity) {
		Decal decal = ComponentsMapper.characterDecal.get(entity).getDecal();
		ModelInstancePools pooledModelInstances = getSystemsCommonData().getPooledModelInstances();
		GameModelInstance modelInstance = pooledModelInstances.obtain(getAssetsManager(), Assets.Models.METAL_PART);
		float characterHeight = ComponentsMapper.enemy.get(entity).getEnemyDeclaration().getHeight();
		modelInstance.transform.setTranslation(decal.getPosition()).trn(0F, characterHeight / 2F, 0F);
		createAndAddFlyingMetalPartEntity(decal, modelInstance);
	}

	private void createAndAddFlyingMetalPartEntity(Decal decal, GameModelInstance modelInstance) {
		EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
				.addFlyingParticleComponent(
						getSystemsCommonData().getMap().getNode(decal.getPosition()).getHeight(),
						METAL_PART_FLY_AWAY_STRENGTH,
						MathUtils.random(METAL_PART_FLY_AWAY_MIN_DEC, METAL_PART_FLY_AWAY_MAX_DEC),
						METAL_PART_FLY_AWAY_MIN_DEGREE,
						METAL_PART_FLY_AWAY_MAX_DEGREE_TO_ADD)
				.addModelInstanceComponent(modelInstance)
				.finishAndAddToEngine();
	}

	@Override
	public void onCharacterDies(final Entity character) {
		if (ComponentsMapper.enemy.has(character)) {
			if (!ComponentsMapper.enemy.get(character).getEnemyDeclaration().human()) {
				createSmoke(character);
				createFlyingMetalParts(character);
			}
			EnemyComponent enemyComponent = ComponentsMapper.enemy.get(character);
			if (enemyComponent.getAiStatus() != IDLE) {
				enemyComponent.setAiStatus(IDLE);
			}
			character.remove(SimpleDecalComponent.class);
		}
	}

	private void createSmoke(Entity character) {
		MapGraph map = getSystemsCommonData().getMap();
		MapGraphNode node = map.getNode(ComponentsMapper.characterDecal.get(character).getDecal().getPosition());
		Vector3 centerPosition = node.getCenterPosition(auxVector3_1.add(0F, SMOKE_HEIGHT_BIAS, 0F));
		EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
				.addParticleEffectComponent(smokeEffect, centerPosition)
				.finishAndAddToEngine();
	}

	private void initializePathPlanRequest(MapGraphNode destinationNode,
										   CharacterDecalComponent charDecalComp,
										   MapGraphConnectionCosts maxCostInclusive,
										   Entity enemy) {
		initializePathPlanRequest(
				getSystemsCommonData().getMap().getNode(charDecalComp.getNodePosition(auxVector2_1)),
				destinationNode,
				maxCostInclusive,
				true,
				enemy);
	}

	private void initializePathPlanRequest(MapGraphNode sourceNode,
										   MapGraphNode destinationNode,
										   MapGraphConnectionCosts maxCostInclusive,
										   boolean avoidCharactersInCalculations,
										   Entity character) {
		request.setSourceNode(sourceNode);
		request.setDestNode(destinationNode);
		request.setOutputPath(pathPlanner.getCurrentPath());
		request.setAvoidCharactersInCalculations(avoidCharactersInCalculations);
		request.setMaxCostInclusive(maxCostInclusive);
		request.setRequester(character);
	}


	void invokeEnemyTurn(final Entity enemy) {
		CharacterComponent character = ComponentsMapper.character.get(enemy);
		character.getCommands().clear();
		EnemyComponent enemyComp = ComponentsMapper.enemy.get(enemy);
		int turnAgility = character.getSkills().getActionPoints();
		if (turnAgility > 0) {
			EnemyAiStatus aiStatus = enemyComp.getAiStatus();
			if (aiStatus == ATTACKING) {
				handleAttackingStatus(enemy, character);
			} else if (aiStatus == RUNNING_TO_LAST_SEEN_POSITION) {
				handleRunningToLastSeenPositionStatus(enemy, character, enemyComp);
			}
		} else {
			enemyFinishedTurn();
		}
	}

	private void handleRunningToLastSeenPositionStatus(Entity enemy, CharacterComponent character, EnemyComponent enemyComp) {
		MapGraphNode targetLastVisibleNode = enemyComp.getTargetLastVisibleNode();
		initializePathPlanRequest(targetLastVisibleNode, ComponentsMapper.characterDecal.get(enemy), CLEAN, enemy);
		if (GameUtils.calculatePath(request, pathPlanner.getPathFinder(), pathPlanner.getHeuristic())) {
			addCommand(enemy, character, targetLastVisibleNode, CharacterCommandsDefinitions.RUN);
		} else {
			enemyFinishedTurn();
		}
	}

	private void handleAttackingStatus(Entity enemy, CharacterComponent character) {
		if (character.getPrimaryAttack().melee()) {
			CharacterDecalComponent characterDecalComponent = ComponentsMapper.characterDecal.get(character.getTarget());
			MapGraphNode targetNode = getSystemsCommonData().getMap().getNode(characterDecalComponent.getDecal().getPosition());
			initializePathPlanRequest(targetNode, ComponentsMapper.characterDecal.get(enemy), CLEAN, enemy);
			if (GameUtils.calculatePath(request, pathPlanner.getPathFinder(), pathPlanner.getHeuristic())) {
				addCommand(enemy, character, targetNode, CharacterCommandsDefinitions.RUN);
				addCommand(enemy, character, targetNode, CharacterCommandsDefinitions.ATTACK_PRIMARY);
			} else {
				enemyFinishedTurn();
			}
		}
	}

	private void addCommand(Entity enemy,
							CharacterComponent character,
							MapGraphNode targetNode,
							CharacterCommandsDefinitions characterCommandsDefinitions) {
		CharacterCommand command = Pools.get(characterCommandsDefinitions.getCharacterCommandImplementation()).obtain();
		command.reset(characterCommandsDefinitions, enemy, request.getOutputPath(), targetNode);
		character.getCommands().addLast(command);
	}

	@Override
	public void onNewTurn(Entity entity) {
		if (ComponentsMapper.enemy.has(entity)) {
			invokeEnemyTurn(entity);
		}
	}

	private void enemyFinishedTurn( ) {
		ComponentsMapper.character.get(getSystemsCommonData().getTurnsQueue().first()).setTurnTimeLeft(TURN_DURATION);
		for (EnemySystemEventsSubscriber subscriber : subscribers) {
			subscriber.onEnemyFinishedTurn();
		}
	}

	private boolean isTargetInFov(final Entity enemy) {
		Vector3 enemyPos = ComponentsMapper.characterDecal.get(enemy).getDecal().getPosition();
		CharacterComponent charComponent = ComponentsMapper.character.get(enemy);
		Vector3 targetPos = ComponentsMapper.characterDecal.get(charComponent.getTarget()).getDecal().getPosition();
		Vector2 enemyDirection = charComponent.getFacingDirection().getDirection(auxVector2_1);
		float dirToTarget = auxVector2_2.set(targetPos.x, targetPos.z).sub(enemyPos.x, enemyPos.z).nor().angleDeg();
		float angleDiff = (enemyDirection.angleDeg() - dirToTarget + 180 + 360) % 360 - 180;
		return angleDiff <= ENEMY_HALF_FOV_ANGLE && angleDiff >= -ENEMY_HALF_FOV_ANGLE;
	}

	private boolean checkIfFloorNodesBlockSightToTarget(final Entity enemy) {
		LinkedHashSet<GridPoint2> allNodesToTarget = GameUtils.findAllNodesToTarget(enemy, bresenhamOutput, true);
		return checkIfFloorNodesBlockSightToTarget(enemy, allNodesToTarget);
	}

	private boolean checkIfFloorNodesBlockSightToTarget(Entity enemy, LinkedHashSet<GridPoint2> nodes) {
		Vector2 enemyPosition = ComponentsMapper.characterDecal.get(enemy).getNodePosition(auxVector2_1);
		for (GridPoint2 n : nodes) {
			MapGraph map = getSystemsCommonData().getMap();
			MapGraphNode node = map.getNode(n.x, n.y);
			Entity door = node.getDoor();
			if (checkIfFloorNodeBlockSightToTarget(enemyPosition, map, node, door)) {
				return true;
			}
		}
		return false;
	}

	private void awakeEnemyIfTargetSpotted(final Entity enemy) {
		if (!isTargetInFov(enemy) || ComponentsMapper.character.get(enemy).getSkills().getHealthData().getHp() <= 0)
			return;

		LinkedHashSet<GridPoint2> nodes = GameUtils.findAllNodesToTarget(enemy, bresenhamOutput, true);
		if (!checkIfFloorNodesBlockSightToTarget(enemy, nodes)) {
			boolean targetIsClose = isTargetCloseEnough(enemy);
			if (targetIsClose) {
				awakeEnemy(enemy);
			}
		}
	}

	private boolean isTargetCloseEnough(Entity enemy) {
		Vector2 enemyPos = ComponentsMapper.characterDecal.get(enemy).getNodePosition(auxVector2_1);
		Entity target = ComponentsMapper.character.get(enemy).getTarget();
		Vector2 targetPos = ComponentsMapper.characterDecal.get(target).getNodePosition(auxVector2_2);
		ComponentsMapper.enemy.get(enemy).setTargetLastVisibleNode(getSystemsCommonData().getMap().getNode(targetPos));
		int maxDistance = ComponentsMapper.enemy.get(enemy).getEnemyDeclaration().sight().getMaxDistance();
		return enemyPos.dst2(targetPos) <= Math.pow(maxDistance, 2);
	}

	private void awakeEnemy(final Entity enemy) {
		if (PARALYZED_ENEMIES || ComponentsMapper.character.get(enemy).getSkills().getHealthData().getHp() <= 0) return;

		EnemyComponent enemyComponent = ComponentsMapper.enemy.get(enemy);
		EnemyAiStatus prevAiStatus = enemyComponent.getAiStatus();
		enemyComponent.setAiStatus(ATTACKING);
		Assets.Sounds awakeSound = enemyComponent.getEnemyDeclaration().soundAwake();
		if (prevAiStatus == IDLE && awakeSound != null) {
			getSystemsCommonData().getSoundPlayer().playSound(awakeSound);
		}
		for (EnemySystemEventsSubscriber subscriber : subscribers) {
			subscriber.onEnemyAwaken(enemy, prevAiStatus, true);
		}
	}

	@Override
	public void onCharacterNodeChanged(final Entity entity, final MapGraphNode oldNode, final MapGraphNode newNode) {
		if (ComponentsMapper.player.has(entity)) {
			for (int i = 0; i < enemies.size(); i++) {
				updateEnemyStatusAccordingToPlayerNewNode(oldNode, enemies.get(i));
			}
		} else if (ComponentsMapper.enemy.has(entity)) {
			awakeEnemyIfTargetSpotted(entity);
		}
	}

	private void updateEnemyStatusAccordingToPlayerNewNode(MapGraphNode oldNode, Entity enemy) {
		EnemyComponent enemyComponent = ComponentsMapper.enemy.get(enemy);
		if (enemyComponent.getAiStatus() != ATTACKING) {
			awakeEnemyIfTargetSpotted(enemy);
		} else {
			tryToToSetToLastSeenPosition(oldNode, enemy, enemyComponent);
		}
	}

	private void tryToToSetToLastSeenPosition(MapGraphNode oldNode, Entity enemy, EnemyComponent enemyComponent) {
		if (checkIfFloorNodesBlockSightToTarget(enemy)
				|| checkIfFloorNodesContainObjects(GameUtils.findAllNodesToTarget(enemy, bresenhamOutput, true))) {
			enemyComponent.setAiStatus(RUNNING_TO_LAST_SEEN_POSITION);
			enemyComponent.setTargetLastVisibleNode(oldNode);
		}
	}

	@Override
	public void onSystemReset(SystemsCommonData systemsCommonData) {
		super.onSystemReset(systemsCommonData);
		pathPlanner = new PathPlanHandler(getSystemsCommonData().getMap());
		enemies = getEngine().getEntitiesFor(Family.all(EnemyComponent.class).get());
	}

	@Override
	public Class<EnemySystemEventsSubscriber> getEventsSubscriberClass( ) {
		return EnemySystemEventsSubscriber.class;
	}

	@Override
	public void initializeData( ) {
		smokeEffect = getAssetsManager().getParticleEffect(Assets.ParticleEffects.SMOKE);
	}

	@Override
	public void dispose( ) {

	}

	@Override
	public void onDoorStateChanged(Entity doorEntity,
								   DoorComponent.DoorStates oldState,
								   DoorComponent.DoorStates newState) {
		if (newState == DoorComponent.DoorStates.OPEN) {
			for (Entity enemy : enemies) {
				awakeEnemyIfTargetSpotted(enemy);
			}
		}
	}
}
