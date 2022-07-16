package com.gadarts.industrial.systems.enemy;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.character.CharacterHealthData;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import com.gadarts.industrial.components.sd.RelatedDecal;
import com.gadarts.industrial.components.sd.SimpleDecalComponent;
import com.gadarts.industrial.map.CalculatePathRequest;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphConnectionCosts;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import com.gadarts.industrial.shared.model.characters.attributes.Accuracy;
import com.gadarts.industrial.shared.model.characters.attributes.Range;
import com.gadarts.industrial.shared.model.characters.enemies.Enemies;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterCommandContext;
import com.gadarts.industrial.systems.character.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.systems.player.PathPlanHandler;
import com.gadarts.industrial.systems.render.RenderSystemEventsSubscriber;
import com.gadarts.industrial.systems.turns.TurnsSystemEventsSubscriber;
import com.gadarts.industrial.utils.GameUtils;

import java.util.ArrayList;
import java.util.List;

import static com.badlogic.gdx.utils.TimeUtils.millis;
import static com.badlogic.gdx.utils.TimeUtils.timeSinceMillis;
import static com.gadarts.industrial.components.ComponentsMapper.*;
import static com.gadarts.industrial.map.MapGraphConnectionCosts.CLEAN;
import static com.gadarts.industrial.map.MapGraphConnectionCosts.HEIGHT_DIFF;
import static com.gadarts.industrial.shared.assets.Assets.Sounds;
import static com.gadarts.industrial.shared.assets.Assets.UiTextures;
import static com.gadarts.industrial.systems.enemy.EnemyAiStatus.*;

public class EnemySystem extends GameSystem<EnemySystemEventsSubscriber> implements
		CharacterSystemEventsSubscriber,
		TurnsSystemEventsSubscriber,
		RenderSystemEventsSubscriber {
	public static final float SKILL_FLOWER_HEIGHT_RELATIVE = 1F;
	private static final long AMB_SOUND_INTERVAL_MIN = 10L;
	private static final long AMB_SOUND_INTERVAL_MAX = 50L;
	private final static Vector2 auxVector2_1 = new Vector2();
	private final static Vector2 auxVector2_2 = new Vector2();
	private static final float ENEMY_HALF_FOV_ANGLE = 75F;
	private static final float MAX_SIGHT = 11;
	private static final CalculatePathRequest request = new CalculatePathRequest();
	private static final List<MapGraphNode> auxNodesList = new ArrayList<>();
	private static final CharacterCommandContext auxCommand = new CharacterCommandContext();
	private static final int NUMBER_OF_SKILL_FLOWER_LEAF = 8;
	private final List<Sounds> ambSounds = List.of(Sounds.AMB_CHAINS, Sounds.AMB_SIGH, Sounds.AMB_LAUGH);
	private final TextureRegion skillFlowerTexture;
	private final PathPlanHandler enemyPathPlanner;
	private ImmutableArray<Entity> enemies;
	private long nextAmbSoundTime;
	private TextureRegion iconAttack;

	public EnemySystem(SystemsCommonData systemsCommonData,
					   GameAssetsManager assetsManager,
					   GameLifeCycleHandler lifeCycleHandler) {
		super(systemsCommonData, assetsManager, lifeCycleHandler);
		skillFlowerTexture = new TextureRegion(assetsManager.getTexture(UiTextures.SKILL_FLOWER_CENTER));
		enemyPathPlanner = new PathPlanHandler(getAssetsManager(), getSystemsCommonData().getMap());
	}

	private void onFrameChangedOfRun(final Entity entity) {
		Vector3 position = ComponentsMapper.characterDecal.get(entity).getDecal().getPosition();
		SimpleDecalComponent simpleDecalComponent = ComponentsMapper.simpleDecal.get(entity);
		float height = ComponentsMapper.enemy.get(entity).getEnemyDefinition().getHeight();
		simpleDecalComponent.getDecal().setPosition(position.x, height + SKILL_FLOWER_HEIGHT_RELATIVE, position.z);
		List<RelatedDecal> relatedDecals = simpleDecalComponent.getRelatedDecals();
		for (RelatedDecal decal : relatedDecals) {
			if (decal.isVisible()) {
				decal.setPosition(position.x, height + SKILL_FLOWER_HEIGHT_RELATIVE, position.z);
			}
		}
	}

	@Override
	public void onFrameChanged(final Entity entity, final float deltaTime, final TextureAtlas.AtlasRegion newFrame) {
		SpriteType spriteType = ComponentsMapper.character.get(entity).getCharacterSpriteData().getSpriteType();
		if (ComponentsMapper.enemy.has(entity)) {
			if (spriteType == SpriteType.RUN) {
				onFrameChangedOfRun(entity);
			}
		}
	}

	private void invokeEnemyAttackBehaviour(final Entity enemy) {
		EnemyComponent enemyComponent = ComponentsMapper.enemy.get(enemy);
		Enemies enemyDefinition = enemyComponent.getEnemyDefinition();
		if (checkIfWayIsClearToTarget(enemy)) {
			engagePrimaryAttack(enemy, enemyDefinition);
		} else {
			enemyComponent.setAiStatus(RUNNING_TO_LAST_SEEN_POSITION);
			enemyFinishedTurn();
		}
	}

	@Override
	public void onCharacterCommandDone(final Entity character, final CharacterCommandContext executedCommand) {
		if (ComponentsMapper.enemy.has(character)) {
			EnemyComponent enemyComponent = ComponentsMapper.enemy.get(character);
			long currentTurnId = getSystemsCommonData().getCurrentTurnId();
			if (executedCommand != null && executedCommand.getDefinition() == CharacterCommandsDefinitions.ATTACK_PRIMARY) {
				enemyComponent.getTimeStamps().setLastPrimaryAttack(currentTurnId);
			}
			enemyComponent.getTimeStamps().setLastTurn(currentTurnId);
			enemyFinishedTurn();
		}
	}

	@Override
	public void onDestinationReached(Entity character) {
		CharacterSystemEventsSubscriber.super.onDestinationReached(character);
	}

	private float calculateDistanceToTarget(final Entity enemy) {
		Entity target = character.get(enemy).getTarget();
		Vector3 targetPosition = characterDecal.get(target).getDecal().getPosition();
		Vector3 position = characterDecal.get(enemy).getDecal().getPosition();
		return position.dst(targetPosition);
	}

	private void engagePrimaryAttack(final Entity enemy,
									 final Enemies def) {
		Accuracy accuracy = def.getAccuracy();
		if (accuracy != null && def.getRange() != Range.NONE) {
			float disToTarget = calculateDistanceToTarget(enemy);
			if (disToTarget <= def.getRange().getMaxDistance()) {
				enemyPathPlanner.getCurrentPath().clear();
				MapGraph map = getSystemsCommonData().getMap();
				MapGraphNode currentNode = map.getNode(characterDecal.get(enemy).getNodePosition(auxVector2_1));
				enemyPathPlanner.getCurrentPath().add(currentNode);
				applyCommand(enemy, CharacterCommandsDefinitions.ATTACK_PRIMARY);
			}
		}
	}

	private boolean checkIfFloorNodesContainObjects(final Array<GridPoint2> nodes, Entity enemyToCheckFor) {
		boolean result = false;
		for (GridPoint2 point : nodes) {
			if (checkIfNodeContainsObject(enemyToCheckFor, point)) {
				result = true;
				break;
			}
		}
		return result;
	}

	private boolean checkIfNodeContainsObject(Entity enemyToCheckFor, GridPoint2 point) {
		MapGraphNode node = getSystemsCommonData().getMap().getNode(point.x, point.y);
		Entity enemy = getSystemsCommonData().getMap().fetchAliveEnemyFromNode(node);
		if (enemy != null && enemy != enemyToCheckFor) {
			return true;
		}
		Entity obstacle = getSystemsCommonData().getMap().fetchObstacleFromNode(node);
		return obstacle != null;
	}

	private boolean checkIfWayIsClearToTarget(final Entity enemy) {
		Array<GridPoint2> nodes = GameUtils.findAllNodesToTarget(enemy);
		boolean blocked = checkIfFloorNodesBlockSightToTarget(enemy, nodes);
		if (!blocked) {
			blocked = checkIfFloorNodesContainObjects(nodes, enemy);
		}
		return !blocked;
	}

	private void addAsPossibleNodeToLookIn(final MapGraphNode enemyNode, final MapGraphNode node, Entity enemy) {
		initializePathPlanRequest(enemyNode, node, CLEAN, true, enemy);
		if (GameUtils.calculatePath(request, enemyPathPlanner.getPathFinder(), enemyPathPlanner.getHeuristic())) {
			if (!auxNodesList.contains(node)) {
				auxNodesList.add(node);
			}
		}
	}

	private void applySearchingModeOnEnemy(final Entity enemy) {
		MapGraph map = getSystemsCommonData().getMap();
		EnemyComponent enemyComponent = ComponentsMapper.enemy.get(enemy);
		enemyComponent.setAiStatus(SEARCHING);
		addPossibleNodesToLookIn(map, map.getNode(characterDecal.get(enemy).getNodePosition(auxVector2_1)), enemy);
		if (!auxNodesList.isEmpty()) {
			enemyComponent.setTargetLastVisibleNode(auxNodesList.get(MathUtils.random(auxNodesList.size() - 1)));
		}
	}

	private void addPossibleNodesToLookIn(MapGraph map, MapGraphNode enemyNode, Entity enemy) {
		auxNodesList.clear();
		int col = enemyNode.getCol();
		int row = enemyNode.getRow();
		int left = Math.max(col - 1, 0);
		int top = Math.max(row - 1, 0);
		int bottom = Math.min(row + 1, map.getDepth());
		int right = Math.min(col + 1, map.getWidth() - 1);
		addAsPossibleNodeToLookIn(enemyNode, map.getNode(left, top), enemy);
		addAsPossibleNodeToLookIn(enemyNode, map.getNode(col, top), enemy);
		addAsPossibleNodeToLookIn(enemyNode, map.getNode(right, top), enemy);
		addAsPossibleNodeToLookIn(enemyNode, map.getNode(left, row), enemy);
		addAsPossibleNodeToLookIn(enemyNode, map.getNode(right, row), enemy);
		addAsPossibleNodeToLookIn(enemyNode, map.getNode(left, bottom), enemy);
		addAsPossibleNodeToLookIn(enemyNode, map.getNode(col, bottom), enemy);
		addAsPossibleNodeToLookIn(enemyNode, map.getNode(right, bottom), enemy);
	}

	@Override
	public void onCharacterGotDamage(final Entity entity) {
		if (ComponentsMapper.enemy.has(entity)) {
			if (ComponentsMapper.enemy.get(entity).getAiStatus() != ATTACKING) {
				awakeEnemy(entity);
			}
			refreshSkillFlower(entity);
		}
	}

	private void refreshSkillFlower(Entity enemy) {
		List<RelatedDecal> relatedDecals = ComponentsMapper.simpleDecal.get(enemy).getRelatedDecals();
		CharacterHealthData healthData = ComponentsMapper.character.get(enemy).getSkills().getHealthData();
		float div = (((float) healthData.getHp()) / ((float) healthData.getInitialHp()));
		int numberOfVisibleLeaf = (int) (div * NUMBER_OF_SKILL_FLOWER_LEAF);
		for (int i = 0; i < relatedDecals.size(); i++) {
			relatedDecals.get(i).setVisible(i < numberOfVisibleLeaf);
		}
	}

	@Override
	public void onCharacterDies(final Entity character) {
		if (ComponentsMapper.enemy.has(character)) {
			EnemyComponent enemyComponent = ComponentsMapper.enemy.get(character);
			if (enemyComponent.getAiStatus() != IDLE) {
				enemyComponent.setAiStatus(IDLE);
			}
			character.remove(SimpleDecalComponent.class);
		}
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

	public void initializePathPlanRequest(MapGraphNode sourceNode,
										  MapGraphNode destinationNode,
										  MapGraphConnectionCosts maxCostInclusive,
										  boolean avoidCharactersInCalculations,
										  Entity character) {
		request.setSourceNode(sourceNode);
		request.setDestNode(destinationNode);
		request.setOutputPath(enemyPathPlanner.getCurrentPath());
		request.setAvoidCharactersInCalculations(avoidCharactersInCalculations);
		request.setMaxCostInclusive(maxCostInclusive);
		request.setRequester(character);
	}

	private void applyCommand(final Entity enemy, final CharacterCommandsDefinitions attackPrimary) {
		auxCommand.init(attackPrimary, enemy, enemyPathPlanner.getCurrentPath(), enemyPathPlanner.getCurrentPath().get(0));
		subscribers.forEach(sub -> sub.onEnemyAppliedCommand(auxCommand, enemy));
	}

	private void invokeEnemyTurn(final Entity enemy) {
		EnemyComponent enemyComponent = ComponentsMapper.enemy.get(enemy);
		if (enemyComponent.getAiStatus() == ATTACKING) {
			invokeEnemyAttackBehaviour(enemy);
		} else {
			CharacterDecalComponent charDecalComp = characterDecal.get(enemy);
			MapGraphNode targetLastVisibleNode = enemyComponent.getTargetLastVisibleNode();
			if (targetLastVisibleNode != null) {
				goAttackAtTheLastVisibleNodeOfTarget(enemy, charDecalComp, targetLastVisibleNode);
			}
		}
	}

	private void goAttackAtTheLastVisibleNodeOfTarget(Entity enemy,
													  CharacterDecalComponent characterDecalComp,
													  MapGraphNode targetLastVisibleNode) {
		MapGraphNode enemyNode = getSystemsCommonData().getMap().getNode(characterDecalComp.getNodePosition(auxVector2_1));
		if (enemyNode.equals(targetLastVisibleNode)) {
			applySearchingModeOnEnemy(enemy);
		}
		initializePathPlanRequest(targetLastVisibleNode, characterDecalComp, CLEAN, enemy);
		tryToPlanThroughHeightDiff(enemy, characterDecalComp, targetLastVisibleNode);
	}

	private void tryToPlanThroughHeightDiff(Entity enemy,
											CharacterDecalComponent characterDecalComp,
											MapGraphNode targetLastVisibleNode) {
		initializePathPlanRequest(targetLastVisibleNode, characterDecalComp, HEIGHT_DIFF, enemy);
		GameUtils.calculatePath(request, enemyPathPlanner.getPathFinder(), enemyPathPlanner.getHeuristic());
	}

	@Override
	public void onNewTurn(Entity entity) {
		if (enemy.has(entity)) {
			invokeEnemyTurn(entity);
		}
	}

	private void enemyFinishedTurn( ) {
		for (EnemySystemEventsSubscriber subscriber : subscribers) {
			subscriber.onEnemyFinishedTurn();
		}
	}

	private boolean isTargetInFov(final Entity enemy) {
		Vector3 enemyPos = characterDecal.get(enemy).getDecal().getPosition();
		CharacterComponent charComponent = character.get(enemy);
		Vector3 targetPos = characterDecal.get(charComponent.getTarget()).getDecal().getPosition();
		Vector2 enemyDirection = charComponent.getCharacterSpriteData().getFacingDirection().getDirection(auxVector2_1);
		float toDeg = enemyDirection.angleDeg() - ENEMY_HALF_FOV_ANGLE;
		float fromDeg = enemyDirection.angleDeg() + ENEMY_HALF_FOV_ANGLE;
		float dirToTarget = auxVector2_2.set(targetPos.x, targetPos.z).sub(enemyPos.x, enemyPos.z).nor().angleDeg();
		return (dirToTarget - fromDeg + 360 + 180) % 360 - 180 + ((toDeg - dirToTarget + 360 + 180) % 360 - 180) < 180;
	}


	private boolean checkIfFloorNodesBlockSightToTarget(final Entity enemy) {
		return checkIfFloorNodesBlockSightToTarget(enemy, GameUtils.findAllNodesToTarget(enemy));
	}

	private boolean checkIfFloorNodesBlockSightToTarget(final Entity enemy, final Array<GridPoint2> nodes) {
		Vector2 pos = characterDecal.get(enemy).getNodePosition(auxVector2_1);
		for (GridPoint2 n : nodes) {
			MapGraph map = getSystemsCommonData().getMap();
			if (map.getNode(n.x, n.y).getHeight() > map.getNode((int) pos.x, (int) pos.y).getHeight() + 1) {
				return true;
			}
		}
		return false;
	}

	private void awakeEnemyIfTargetSpotted(final Entity enemy) {
		if (isTargetInFov(enemy) && !checkIfFloorNodesBlockSightToTarget(enemy)) {
			Vector2 enemyPos = characterDecal.get(enemy).getNodePosition(auxVector2_1);
			Entity target = character.get(enemy).getTarget();
			Vector2 targetPos = characterDecal.get(target).getNodePosition(auxVector2_2);
			if (enemyPos.dst2(targetPos) <= Math.pow(MAX_SIGHT, 2)) {
				awakeEnemy(enemy);
			}
		}
	}

	private void awakeEnemy(final Entity enemy) {
		if (character.get(enemy).getSkills().getHealthData().getHp() <= 0) return;
		ComponentsMapper.enemy.get(enemy).setAiStatus(ATTACKING);
		Sounds awakeSound = ComponentsMapper.enemy.get(enemy).getEnemyDefinition().getAwakeSound();
		getSystemsCommonData().getSoundPlayer().playSound(awakeSound);
		updateFlowerOnAwakening(enemy);
		for (EnemySystemEventsSubscriber subscriber : subscribers) {
			subscriber.onEnemyAwaken(enemy);
		}
	}

	private void updateFlowerOnAwakening(Entity enemy) {
		SimpleDecalComponent simpleDecalComponent = simpleDecal.get(enemy);
		Decal flowerDecal = simpleDecalComponent.getDecal();
		flowerDecal.setTextureRegion(skillFlowerTexture);
		List<RelatedDecal> relatedDecals = simpleDecalComponent.getRelatedDecals();
		relatedDecals.get(relatedDecals.size() - 1).setTextureRegion(iconAttack);
	}

	@Override
	public void onCharacterNodeChanged(final Entity entity, final MapGraphNode oldNode, final MapGraphNode newNode) {
		if (player.has(entity)) {
			for (int i = 0; i < enemies.size(); i++) {
				updateEnemyStatusAccordingToPlayerNewNode(oldNode, enemies.get(i));
			}
		}
	}

	private void updateEnemyStatusAccordingToPlayerNewNode(MapGraphNode oldNode, Entity enemy) {
		EnemyComponent enemyComponent = ComponentsMapper.enemy.get(enemy);
		if (enemyComponent.getAiStatus() != ATTACKING) {
			awakeEnemyIfTargetSpotted(enemy);
		} else if (!isTargetInFov(enemy)
				|| checkIfFloorNodesBlockSightToTarget(enemy)
				|| checkIfFloorNodesContainObjects(GameUtils.findAllNodesToTarget(enemy), enemy)) {
			enemyComponent.setAiStatus(RUNNING_TO_LAST_SEEN_POSITION);
			enemyComponent.setTargetLastVisibleNode(oldNode);
		}
	}

	@Override
	public void addedToEngine(Engine engine) {
		super.addedToEngine(engine);
		enemies = engine.getEntitiesFor(Family.all(EnemyComponent.class).get());
	}

	@Override
	public void update(final float deltaTime) {
		super.update(deltaTime);
		handleRoamSounds();
		if (millis() > nextAmbSoundTime) {
			getSystemsCommonData().getSoundPlayer().playSound(ambSounds.get(MathUtils.random(0, ambSounds.size() - 1)));
			resetNextAmbSound();
		}
	}

	private void handleRoamSounds( ) {
		for (Entity enemy : enemies) {
			EnemyComponent enemyComp = ComponentsMapper.enemy.get(enemy);
			if (enemyComp.getAiStatus() != IDLE && timeSinceMillis(enemyComp.getNextRoamSound()) >= 0) {
				if (enemyComp.getNextRoamSound() != 0) {
					getSystemsCommonData().getSoundPlayer().playSound(enemyComp.getEnemyDefinition().getRoamSound());
				}
				enemyComp.calculateNextRoamSound();
			}
		}
	}

	private void resetNextAmbSound( ) {
		nextAmbSoundTime = millis() + MathUtils.random(AMB_SOUND_INTERVAL_MIN, AMB_SOUND_INTERVAL_MAX) * 1000L;
	}

	@Override
	public Class<EnemySystemEventsSubscriber> getEventsSubscriberClass( ) {
		return EnemySystemEventsSubscriber.class;
	}

	@Override
	public void initializeData( ) {
		iconAttack = new TextureRegion(getAssetsManager().getTexture(UiTextures.ICON_ATTACK));
	}

	@Override
	public void dispose( ) {

	}
}
