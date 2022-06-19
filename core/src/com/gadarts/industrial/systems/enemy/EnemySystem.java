package com.gadarts.industrial.systems.enemy;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import com.gadarts.industrial.shared.model.characters.attributes.Accuracy;
import com.gadarts.industrial.shared.model.characters.attributes.Range;
import com.gadarts.industrial.shared.model.characters.enemies.Enemies;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.FlowerSkillIconComponent;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.character.CharacterHealthData;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import com.gadarts.industrial.components.sd.RelatedDecal;
import com.gadarts.industrial.components.sd.SimpleDecalComponent;
import com.gadarts.industrial.map.*;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterCommand;
import com.gadarts.industrial.systems.character.CharacterCommandsTypes;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.systems.player.PathPlanHandler;
import com.gadarts.industrial.systems.render.RenderSystemEventsSubscriber;
import com.gadarts.industrial.systems.turns.TurnsSystemEventsSubscriber;
import com.gadarts.industrial.utils.EntityBuilder;
import com.gadarts.industrial.utils.GameUtils;

import java.util.ArrayList;
import java.util.List;

import static com.badlogic.gdx.utils.TimeUtils.millis;
import static com.badlogic.gdx.utils.TimeUtils.timeSinceMillis;
import static com.gadarts.industrial.shared.assets.Assets.Sounds;
import static com.gadarts.industrial.shared.assets.Assets.UiTextures;
import static com.gadarts.industrial.shared.model.characters.attributes.Accuracy.NONE;
import static com.gadarts.industrial.components.ComponentsMapper.*;
import static com.gadarts.industrial.components.ComponentsMapper.flowerSkillIcon;
import static com.gadarts.industrial.components.ComponentsMapper.simpleDecal;
import static com.gadarts.industrial.map.MapGraphConnectionCosts.CLEAN;
import static com.gadarts.industrial.map.MapGraphConnectionCosts.HEIGHT_DIFF;
import static com.gadarts.industrial.systems.enemy.EnemyAiStatus.*;

public class EnemySystem extends GameSystem<EnemySystemEventsSubscriber> implements
		CharacterSystemEventsSubscriber,
		TurnsSystemEventsSubscriber,
		RenderSystemEventsSubscriber {
	public static final float SKILL_FLOWER_HEIGHT_RELATIVE = 1F;
	private static final int ICON_DURATION = 2;
	private static final float ICON_SPEED = 0.5F;
	private static final long AMB_SOUND_INTERVAL_MIN = 10L;
	private static final long AMB_SOUND_INTERVAL_MAX = 50L;
	private final static Vector2 auxVector2_1 = new Vector2();
	private final static Vector2 auxVector2_2 = new Vector2();
	private static final float ENEMY_HALF_FOV_ANGLE = 75F;
	private static final float MAX_SIGHT = 11;
	private static final CalculatePathRequest request = new CalculatePathRequest();
	private static final float RANGE_ATTACK_MIN_RADIUS = 1.7F;
	private static final List<MapGraphNode> auxNodesList = new ArrayList<>();
	private static final CharacterCommand auxCommand = new CharacterCommand();
	private static final int NUMBER_OF_SKILL_FLOWER_LEAF = 8;
	private final List<Entity> iconsToRemove = new ArrayList<>();
	private final List<Sounds> ambSounds = List.of(Sounds.AMB_CHAINS, Sounds.AMB_SIGH, Sounds.AMB_LAUGH);
	private final TextureRegion skillFlowerTexture;
	private final Texture iconSpottedTexture;
	private final Texture iconLookingForTexture;
	private final PathPlanHandler enemyPathPlanner;
	private ImmutableArray<Entity> enemies;
	private ImmutableArray<Entity> icons;
	private long nextAmbSoundTime;

	public EnemySystem(SystemsCommonData systemsCommonData,
					   SoundPlayer soundPlayer,
					   GameAssetsManager assetsManager,
					   GameLifeCycleHandler lifeCycleHandler) {
		super(systemsCommonData, soundPlayer, assetsManager, lifeCycleHandler);
		skillFlowerTexture = new TextureRegion(assetsManager.getTexture(UiTextures.SKILL_FLOWER_CENTER));
		iconSpottedTexture = assetsManager.getTexture(UiTextures.ICON_SPOTTED);
		enemyPathPlanner = new PathPlanHandler(getAssetsManager(), getSystemsCommonData().getMap());
		iconLookingForTexture = assetsManager.getTexture(UiTextures.ICON_LOOKING_FOR);
	}

	private void onFrameChangedOfAttack(final Entity entity, final TextureAtlas.AtlasRegion newFrame) {
		if (newFrame.index == ComponentsMapper.character.get(entity).getCharacterSpriteData().getMeleeHitFrameIndex()) {
			getSoundPlayer().playSound(ComponentsMapper.enemy.get(entity).getEnemyDefinition().getAttackSound());
		}
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
			if (spriteType == SpriteType.ATTACK) {
				onFrameChangedOfAttack(entity, newFrame);
			} else if (spriteType == SpriteType.RUN) {
				onFrameChangedOfRun(entity);
			}
		}
	}

	private boolean invokeTurnForUnplayedEnemy(final long currentTurnId) {
		for (Entity enemy : enemies) {
			int hp = character.get(enemy).getSkills().getHealthData().getHp();
			EnemyComponent enemyComponent = ComponentsMapper.enemy.get(enemy);
			if (enemyComponent.getTimeStamps().getLastTurn() < currentTurnId) {
				if (hp > 0 && enemyComponent.getAiStatus() != IDLE) {
					invokeEnemyTurn(enemy);
					return true;
				}
			}
		}
		return false;
	}

	private void invokeEnemyAttackBehaviour(final Entity enemy) {
		Vector2 enemyPosition = characterDecal.get(enemy).getNodePosition(auxVector2_1);
		Entity target = character.get(enemy).getTarget();
		MapGraphNode enemyNode = getSystemsCommonData().getMap().getNode(enemyPosition);
		EnemyComponent enemyComponent = ComponentsMapper.enemy.get(enemy);
		Enemies enemyDefinition = enemyComponent.getEnemyDefinition();
		if (!considerPrimaryAttack(enemy, enemyComponent, enemyDefinition, enemyComponent.getSkill() - 1)) {
			calculatePathAndApplyGoToMelee(enemy, enemyNode, target);
		}
	}

	public boolean calculatePathToCharacter(MapGraphNode sourceNode,
											Entity character,
											boolean avoidCharactersInCalculation,
											MapGraphConnectionCosts maxCostPerNodeConn) {
		enemyPathPlanner.getCurrentPath().clear();
		CharacterDecalComponent characterDecalComponent = characterDecal.get(character);
		Vector2 cellPosition = characterDecalComponent.getNodePosition(auxVector2_1);
		MapGraphNode destNode = getSystemsCommonData().getMap().getNode((int) cellPosition.x, (int) cellPosition.y);
		initializePathPlanRequest(sourceNode, destNode, maxCostPerNodeConn, avoidCharactersInCalculation, character);
		return enemyPathPlanner.getPathFinder().searchNodePathBeforeCommand(enemyPathPlanner.getHeuristic(), request);
	}

	@Override
	public void onCharacterCommandDone(final Entity character, final CharacterCommand executedCommand) {
		if (ComponentsMapper.enemy.has(character)) {
			EnemyComponent enemyComponent = ComponentsMapper.enemy.get(character);
			long currentTurnId = getSystemsCommonData().getCurrentTurnId();
			if (executedCommand != null && executedCommand.getType() == CharacterCommandsTypes.ATTACK_PRIMARY) {
				enemyComponent.getTimeStamps().setLastPrimaryAttack(currentTurnId);
			}
			enemyComponent.getTimeStamps().setLastTurn(currentTurnId);
			onEnemyTurn(currentTurnId);
		}
	}

	@Override
	public void onDestinationReached(Entity character) {
		CharacterSystemEventsSubscriber.super.onDestinationReached(character);
	}

	private void calculatePathAndApplyGoToMelee(final Entity enemy,
												final MapGraphNode enemyNode,
												final Entity target) {
		boolean pathCalculated = calculatePathToCharacter(enemyNode, target, true, CLEAN)
				|| calculatePathToCharacter(enemyNode, target, false, CLEAN)
				|| calculatePathToCharacter(enemyNode, target, false, HEIGHT_DIFF);
		if (pathCalculated) {
			applyGoToMelee(enemy);
		} else {
			onCharacterCommandDone(enemy, null);
		}
	}

	private void applyGoToMelee(final Entity enemy) {
		MapGraphPath currentPath = enemyPathPlanner.getCurrentPath();
		currentPath.nodes.removeIndex(currentPath.getCount() - 1);
		applyCommand(enemy, CharacterCommandsTypes.GO_TO_MELEE);
	}

	private float calculateDistanceToTarget(final Entity enemy) {
		Entity target = character.get(enemy).getTarget();
		Vector3 targetPosition = characterDecal.get(target).getDecal().getPosition();
		Vector3 position = characterDecal.get(enemy).getDecal().getPosition();
		return position.dst(targetPosition);
	}

	private boolean checkIfPrimaryAttackIsReady(final EnemyComponent enemyComponent, final int turnsDiff) {
		long currentTurnId = getSystemsCommonData().getCurrentTurnId();
		long lastPrimaryAttack = enemyComponent.getTimeStamps().getLastPrimaryAttack();
		return lastPrimaryAttack < 0 || currentTurnId - lastPrimaryAttack > turnsDiff;
	}

	private boolean considerPrimaryAttack(final Entity enemy,
										  final EnemyComponent enemyCom,
										  final Enemies def,
										  final int skillIndex) {
		Accuracy[] accuracy = def.getAccuracy();
		if (accuracy != null && accuracy[skillIndex] != NONE && def.getRange().get(skillIndex) != Range.NONE) {
			float disToTarget = calculateDistanceToTarget(enemy);
			if (disToTarget <= def.getRange().get(skillIndex).getMaxDistance() && disToTarget > RANGE_ATTACK_MIN_RADIUS) {
				int turnsDiff = def.getReloadTime().get(skillIndex).getNumberOfTurns();
				if (checkIfPrimaryAttackIsReady(enemyCom, turnsDiff) && !checkIfWayIsClearToTarget(enemy)) {
					applyCommand(enemy, CharacterCommandsTypes.ATTACK_PRIMARY);
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkIfFloorNodesContainsEnemy(final Array<GridPoint2> nodes, Entity enemyToCheckFor) {
		boolean result = false;
		for (GridPoint2 point : nodes) {
			MapGraph map = getSystemsCommonData().getMap();
			Entity enemy = map.getAliveEnemyFromNode(map.getNode(point.x, point.y));
			if (enemy != null && enemy != enemyToCheckFor) {
				result = true;
				break;
			}
		}
		return result;
	}

	private boolean checkIfWayIsClearToTarget(final Entity enemy) {
		Array<GridPoint2> nodes = GameUtils.findAllNodesToTarget(enemy);
		boolean blocked = checkIfFloorNodesBlockSightToTarget(enemy, nodes);
		if (!blocked) {
			blocked = checkIfFloorNodesContainsEnemy(nodes, enemy);
		}
		return blocked;
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
		if (enemyComponent.getAiStatus() == RUNNING_TO_LAST_SEEN_POSITION) {
			createSkillFlowerIcon(simpleDecal.get(enemy).getDecal(), iconLookingForTexture);
		}
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

	private void refreshSkillFlower(final Entity entity) {
		List<RelatedDecal> relatedDecals = ComponentsMapper.simpleDecal.get(entity).getRelatedDecals();
		CharacterHealthData healthData = ComponentsMapper.character.get(entity).getSkills().getHealthData();
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

	private void applyCommand(final Entity enemy, final CharacterCommandsTypes attackPrimary) {
		auxCommand.init(attackPrimary, enemyPathPlanner.getCurrentPath(), enemy);
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
		if (GameUtils.calculatePath(request, enemyPathPlanner.getPathFinder(), enemyPathPlanner.getHeuristic())) {
			applyCommand(enemy, CharacterCommandsTypes.GO_TO_MELEE);
		} else {
			tryToPlanThroughHeightDiff(enemy, characterDecalComp, targetLastVisibleNode);
		}
	}

	private void tryToPlanThroughHeightDiff(Entity enemy,
											CharacterDecalComponent characterDecalComp,
											MapGraphNode targetLastVisibleNode) {
		boolean foundPath;
		initializePathPlanRequest(targetLastVisibleNode, characterDecalComp, HEIGHT_DIFF, enemy);
		foundPath = GameUtils.calculatePath(request, enemyPathPlanner.getPathFinder(), enemyPathPlanner.getHeuristic());
		if (foundPath) {
			applyCommand(enemy, CharacterCommandsTypes.GO_TO_MELEE);
		}
	}

	@Override
	public void onEnemyTurn(final long currentTurnId) {
		if (invokeTurnForUnplayedEnemy(currentTurnId)) return;
		enemiesFinishedTurn();
	}

	private void enemiesFinishedTurn( ) {
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
		getSoundPlayer().playSound(ComponentsMapper.enemy.get(enemy).getEnemyDefinition().getAwakeSound());
		Decal flowerDecal = simpleDecal.get(enemy).getDecal();
		flowerDecal.setTextureRegion(skillFlowerTexture);
		createSkillFlowerIcon(flowerDecal, iconSpottedTexture);
		for (EnemySystemEventsSubscriber subscriber : subscribers) {
			subscriber.onEnemyAwaken(enemy);
		}
	}

	private void createSkillFlowerIcon(final Decal flowerDecal, final Texture iconTexture) {
		EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
				.addSimpleDecalComponent(flowerDecal.getPosition(), iconTexture, true, true)
				.addFlowerSkillIconComponent()
				.finishAndAddToEngine();
	}

	@Override
	public void onCharacterNodeChanged(final Entity entity, final MapGraphNode oldNode, final MapGraphNode newNode) {
		if (player.has(entity)) {
			for (Entity enemy : enemies) {
				EnemyComponent enemyComponent = ComponentsMapper.enemy.get(enemy);
				if (enemyComponent.getAiStatus() != ATTACKING) {
					awakeEnemyIfTargetSpotted(enemy);
				} else if (!isTargetInFov(enemy) || checkIfFloorNodesBlockSightToTarget(enemy)) {
					enemyComponent.setAiStatus(RUNNING_TO_LAST_SEEN_POSITION);
					enemyComponent.setTargetLastVisibleNode(oldNode);
				}
			}
		}
	}

	@Override
	public void addedToEngine(Engine engine) {
		super.addedToEngine(engine);
		enemies = engine.getEntitiesFor(Family.all(EnemyComponent.class).get());
		icons = engine.getEntitiesFor(Family.all(FlowerSkillIconComponent.class).get());
	}

	@Override
	public void update(final float deltaTime) {
		super.update(deltaTime);
		handleRoamSounds();
		handleFlowerSkills(deltaTime);
		if (millis() > nextAmbSoundTime) {
			getSoundPlayer().playSound(ambSounds.get(MathUtils.random(0, ambSounds.size() - 1)));
			resetNextAmbSound();
		}
	}

	private void handleFlowerSkills(float deltaTime) {
		iconsToRemove.clear();
		for (Entity flowerIcon : icons) {
			if (timeSinceMillis(flowerSkillIcon.get(flowerIcon).getTimeOfCreation()) >= ICON_DURATION * 1000F) {
				iconsToRemove.add(flowerIcon);
			} else {
				simpleDecal.get(flowerIcon).getDecal().getPosition().add(0, deltaTime * ICON_SPEED, 0);
			}
		}
		for (Entity icon : iconsToRemove) {
			getEngine().removeEntity(icon);
		}
	}

	private void handleRoamSounds( ) {
		for (Entity enemy : enemies) {
			EnemyComponent enemyComp = ComponentsMapper.enemy.get(enemy);
			if (enemyComp.getAiStatus() != IDLE && timeSinceMillis(enemyComp.getNextRoamSound()) >= 0) {
				if (enemyComp.getNextRoamSound() != 0) {
					getSoundPlayer().playSound(enemyComp.getEnemyDefinition().getRoamSound());
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

	}

	@Override
	public void dispose( ) {

	}
}
