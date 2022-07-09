package com.gadarts.industrial.systems.projectiles;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.components.BulletComponent;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.collision.CollisionComponent;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.player.Weapon;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.shared.model.characters.attributes.Accuracy;
import com.gadarts.industrial.shared.model.map.MapNodesTypes;
import com.gadarts.industrial.shared.model.pickups.WeaponsDefinitions;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.utils.EntityBuilder;

import java.util.HashMap;
import java.util.Map;

public class BulletSystem extends GameSystem<BulletSystemEventsSubscriber> implements CharacterSystemEventsSubscriber {
	private static final float BULLET_SPEED = 0.2f;
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final Vector2 auxVector2_2 = new Vector2();
	private static final Vector2 auxVector2_3 = new Vector2();
	private static final Vector2 auxVector2_4 = new Vector2();
	private static final Vector2 auxVector2_5 = new Vector2();
	private static final Vector2 auxVector2_6 = new Vector2();
	private static final float BULLET_MAX_DISTANCE = 14;
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final Vector3 auxVector3_2 = new Vector3();
	private static final Vector3 auxVector3_3 = new Vector3();
	private static final Vector3 auxVector3_4 = new Vector3();
	private static final Bresenham2 bresenham = new Bresenham2();
	private static final float HIT_SCAN_MAX_DISTANCE = 10F;
	private final static float HITSCAN_COL_LIGHT_INTENSITY = 0.1F;
	private final static float HITSCAN_COL_LIGHT_RADIUS = 1.2F;
	private final static Color HITSCAN_COL_LIGHT_COLOR = Color.YELLOW;
	private final static float HITSCAN_COL_LIGHT_DURATION = 0.1F;
	private final static float PROJ_LIGHT_INTENSITY = 0.05F;
	private final static float PROJ_LIGHT_RADIUS = 1F;
	private final static Color PROJ_LIGHT_COLOR = Color.valueOf("#8396FF");
	private static final Quaternion auxQuat = new Quaternion();
	private static final Matrix4 auxMatrix = new Matrix4();
	private final Map<Assets.Models, Pool<GameModelInstance>> pooledBulletModels = new HashMap<>();
	private ParticleEffect bulletRicochetEffect;
	private ImmutableArray<Entity> bullets;
	private ImmutableArray<Entity> collidables;

	public BulletSystem(SystemsCommonData systemsCommonData,
						SoundPlayer soundPlayer,
						GameAssetsManager assetsManager,
						GameLifeCycleHandler lifeCycleHandler) {
		super(systemsCommonData, soundPlayer, assetsManager, lifeCycleHandler);
	}

	@Override
	public void onCharacterEngagesPrimaryAttack(final Entity character,
												final Vector3 direction,
												final Vector3 charPos) {

		if (ComponentsMapper.enemy.has(character)) {
			enemyEngagesPrimaryAttack(character, direction, charPos);
		} else {
			playerEngagesPrimaryAttack(character, direction);
		}
	}

	private void playerEngagesPrimaryAttack(final Entity character, final Vector3 direction) {
		Weapon selectedWeapon = getSystemsCommonData().getStorage().getSelectedWeapon();
		if (selectedWeapon.isHitScan()) {
			playerEngagesHitScanAttack(character, direction, selectedWeapon);
		}
	}

	private void playerEngagesHitScanAttack(final Entity character, final Vector3 direction, final Weapon selectedWeapon) {
		MapGraph map = getSystemsCommonData().getMap();
		MapGraphNode posNode = map.getNode(ComponentsMapper.characterDecal.get(character).getNodePosition(auxVector3_1));
		Vector3 posNodeCenterPos = posNode.getCenterPosition(auxVector3_4);
		affectAimByAccuracy(character, direction);
		Vector3 maxRangePos = calculateHitScanMaxPosition(direction, posNodeCenterPos);
		Array<GridPoint2> nodes = findAllNodesOnTheWayOfTheHitScan(posNodeCenterPos, maxRangePos);
		for (GridPoint2 node : nodes) {
			if (applyHitScanThroughNodes(selectedWeapon, map, posNodeCenterPos, node, maxRangePos)) {
				return;
			}
		}
	}

	private Vector2 findNodeSegmentIntersection(final Vector3 posNodeCenterPos,
												final Vector3 maxRangePos,
												final GridPoint2 node) {
		Vector2 src = auxVector2_1.set(posNodeCenterPos.x, posNodeCenterPos.z);
		Vector2 dst = auxVector2_2.set(maxRangePos.x, maxRangePos.z);
		Vector2 closest = auxVector2_5.setZero();
		float min = Integer.MAX_VALUE;
		min = intersectSegments(posNodeCenterPos, src, dst, closest, min, auxVector2_3.set(node.x, node.y), auxVector2_4.set(node.x + 1F, node.y));
		min = intersectSegments(posNodeCenterPos, src, dst, closest, min, auxVector2_3.set(auxVector2_4), auxVector2_4.set(node.x + 1F, node.y + 1F));
		min = intersectSegments(posNodeCenterPos, src, dst, closest, min, auxVector2_3.set(auxVector2_4), auxVector2_4.set(node.x, node.y + 1F));
		intersectSegments(posNodeCenterPos, src, dst, closest, min, auxVector2_3.set(auxVector2_4), auxVector2_4.set(node.x, node.y));
		return closest;
	}

	private float intersectSegments(final Vector3 posNodeCenterPos,
									final Vector2 src,
									final Vector2 dst,
									final Vector2 closest,
									final float min,
									final Vector2 lineVertex1, final Vector2 lineVertex2) {
		Vector2 candidate = auxVector2_6;
		Intersector.intersectSegments(src, dst, lineVertex1, lineVertex2, candidate.set(closest));
		if (!candidate.isZero()) {
			float distance = posNodeCenterPos.dst(candidate.x, posNodeCenterPos.y, candidate.y);
			if (distance < min) {
				closest.set(candidate);
				return distance;
			}
		}
		return min;
	}

	private boolean applyHitScanThroughNodes(final Weapon selectedWeapon,
											 final MapGraph map,
											 final Vector3 posNodeCenterPos,
											 final GridPoint2 n,
											 final Vector3 maxRangePos) {
		MapGraphNode node = map.getNode(n.x, n.y);
		if (node.getHeight() > map.getNode((int) posNodeCenterPos.x, (int) posNodeCenterPos.z).getHeight() + 1) {
			Vector2 intersectionPos = findNodeSegmentIntersection(posNodeCenterPos, maxRangePos, n);
			if (!intersectionPos.equals(Vector2.Zero)) {
				auxVector3_1.set(intersectionPos.x, posNodeCenterPos.y + 1F, intersectionPos.y);
				Vector3 position = auxVector3_1.set(intersectionPos.x, posNodeCenterPos.y + 1F, intersectionPos.y);
				EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
						.addParticleEffectComponent((PooledEngine) getEngine(), bulletRicochetEffect, position)
						.addShadowlessLightComponent(position,
								HITSCAN_COL_LIGHT_INTENSITY,
								HITSCAN_COL_LIGHT_RADIUS,
								HITSCAN_COL_LIGHT_COLOR,
								HITSCAN_COL_LIGHT_DURATION)
						.finishAndAddToEngine();
				return true;
			} else {
				return false;
			}
		}
		Entity enemy = map.getAliveEnemyFromNode(node);
		if (enemy != null) {
			onHitScanCollisionWithAnotherEntity((WeaponsDefinitions) selectedWeapon.getDefinition(), enemy);
			Vector3 position = auxVector3_1.set(ComponentsMapper.characterDecal.get(enemy).getDecal().getPosition());
			EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
					.addParticleEffectComponent((PooledEngine) getEngine(), bulletRicochetEffect, position)
					.addShadowlessLightComponent(position,
							HITSCAN_COL_LIGHT_INTENSITY,
							HITSCAN_COL_LIGHT_RADIUS,
							HITSCAN_COL_LIGHT_COLOR,
							HITSCAN_COL_LIGHT_DURATION)
					.finishAndAddToEngine();
			return true;
		}
		return false;
	}

	private void onHitScanCollisionWithAnotherEntity(final WeaponsDefinitions definition, final Entity collidable) {
		for (BulletSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onHitScanCollisionWithAnotherEntity(definition, collidable);
		}
	}

	private Array<GridPoint2> findAllNodesOnTheWayOfTheHitScan(final Vector3 posNodeCenterPos,
															   final Vector3 maxRangePos) {
		return bresenham.line(
				(int) posNodeCenterPos.x, (int) posNodeCenterPos.z,
				(int) maxRangePos.x, (int) maxRangePos.z);
	}

	private Vector3 calculateHitScanMaxPosition(final Vector3 direction, final Vector3 posNodeCenterPosition) {
		Vector3 step = auxVector3_3.setZero().add(direction.setLength(HIT_SCAN_MAX_DISTANCE));
		return auxVector3_2.set(posNodeCenterPosition).add(step);
	}

	private void affectAimByAccuracy(final Entity character, final Vector3 direction) {
		int maxAngle = ComponentsMapper.character.get(character).getSkills().getAccuracy().getMaxAngle();
		direction.rotate(Vector3.Y, MathUtils.random(-maxAngle, maxAngle));
	}

	private void enemyEngagesPrimaryAttack(final Entity character, final Vector3 direction, final Vector3 charPos) {
		EnemyComponent enemyComponent = ComponentsMapper.enemy.get(character);
		Accuracy accuracy = enemyComponent.getEnemyDefinition().getAccuracy();
//		direction.rotate(Vector3.Y, MathUtils.random(-accuracy.getMaxAngle(), accuracy.getMaxAngle()));
//		direction.rotate(Vector3.X, MathUtils.random(-accuracy.getMaxAngle(), accuracy.getMaxAngle()));
		EnemyComponent enemyComp = ComponentsMapper.enemy.get(character);
		getSoundPlayer().playSound(Assets.Sounds.ATTACK_ENERGY_BALL);
		createEnemyBullet(character, direction, charPos, enemyComp);
	}

	private void createEnemyBullet(Entity character,
								   Vector3 direction,
								   Vector3 charPos,
								   EnemyComponent enemyComp) {
		charPos.y += ComponentsMapper.enemy.get(character).getEnemyDefinition().getHeight() / 2F;
		Integer damagePoints = enemyComp.getEnemyDefinition().getPrimaryAttack().getDamagePoints();
		GameAssetsManager assetsManager = getAssetsManager();
		ParticleEffect effect = assetsManager.getParticleEffect(Assets.ParticleEffects.ENERGY_BALL_TRAIL);
		if (!pooledBulletModels.containsKey(Assets.Models.LASER_BULLET)) {
			pooledBulletModels.put(Assets.Models.LASER_BULLET, new Pool<>() {
				@Override
				protected GameModelInstance newObject( ) {
					return new GameModelInstance(assetsManager.getModel(Assets.Models.LASER_BULLET));
				}
			});
		}

		GameModelInstance modelInstance = pooledBulletModels.get(Assets.Models.LASER_BULLET).obtain();
		modelInstance.transform.setToTranslation(charPos);
		modelInstance.transform.rotate(Vector3.Y, -auxVector2_1.set(direction.x, direction.z).nor().angleDeg());
		Entity bullet = EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
				.addBulletComponent(charPos, direction, character, damagePoints)
				.addModelInstanceComponent(modelInstance, true, false)
				.addShadowlessLightComponent(charPos, PROJ_LIGHT_INTENSITY, PROJ_LIGHT_RADIUS, PROJ_LIGHT_COLOR)
				.finishAndAddToEngine();
		EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
				.addParticleEffectComponent((PooledEngine) getEngine(), effect, auxVector3_1.set(charPos), bullet)
				.finishAndAddToEngine();
	}

	@Override
	public void addedToEngine(Engine engine) {
		bullets = engine.getEntitiesFor(Family.all(BulletComponent.class).get());
		collidables = engine.getEntitiesFor(Family.all(CollisionComponent.class).get());
	}

	private boolean handleCollisionsWithWalls(final Entity bullet) {
		GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(bullet).getModelInstance();
		Vector3 position = modelInstance.transform.getTranslation(auxVector3_1);
		MapGraphNode node = getSystemsCommonData().getMap().getNode(position);
		MapNodesTypes nodeType = node.getType();
		if (nodeType != MapNodesTypes.PASSABLE_NODE || node.getHeight() >= position.y) {
			onCollisionWithWall(bullet, node);
			return true;
		}
		return false;
	}

	private void onCollisionWithWall(final Entity bullet, final MapGraphNode node) {
		for (BulletSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onBulletCollisionWithWall(bullet, node);
		}
		destroyBullet(bullet);
	}

	private void destroyBullet(final Entity bullet) {
		bullet.remove(BulletComponent.class);
		getEngine().removeEntity(bullet);
		ParticleEffect effect = getAssetsManager().getParticleEffect(Assets.ParticleEffects.ENERGY_BALL_EXPLOSION);
		GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(bullet).getModelInstance();
		Vector3 pos = auxVector3_1.set(modelInstance.transform.getTranslation(auxVector3_1));
		createExplosion(effect, pos);
	}

	private void createExplosion(final ParticleEffect effect, final Vector3 pos) {
		EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
				.addParticleEffectComponent((PooledEngine) getEngine(), effect, pos)
				.finishAndAddToEngine();
		getSoundPlayer().playSound(Assets.Sounds.SMALL_EXP);
	}

	private boolean checkCollisionWithCharacter(GameModelInstance gameModelInstance, Entity collidable) {
		Vector3 colPos = ComponentsMapper.characterDecal.get(collidable).getDecal().getPosition();
		boolean alive = ComponentsMapper.character.get(collidable).getSkills().getHealthData().getHp() > 0;
		Vector3 position = gameModelInstance.transform.getTranslation(auxVector3_1);
		float distance = auxVector2_1.set(colPos.x, colPos.z).dst(position.x, position.z);
		return alive && distance < CharacterComponent.CHAR_RAD;
	}

	private boolean checkCollision(GameModelInstance gameModelInstance, Entity collidable) {
		if (ComponentsMapper.characterDecal.has(collidable)) {
			return checkCollisionWithCharacter(gameModelInstance, collidable);
		}
		return false;
	}

	private void handleCollisionsWithOtherEntities(GameModelInstance gameModelInstance, Entity bullet) {
		for (Entity collidable : collidables) {
			if (ComponentsMapper.bullet.get(bullet).getOwner() != collidable) {
				if (checkCollision(gameModelInstance, collidable)) {
					onProjectileCollisionWithAnotherEntity(bullet, collidable);
					break;
				}
			}
		}
	}

	private void onProjectileCollisionWithAnotherEntity(final Entity bullet, final Entity collidable) {
		for (BulletSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onProjectileCollisionWithAnotherEntity(bullet, collidable);
		}
		destroyBullet(bullet);
	}

	private void handleCollisions(GameModelInstance gameModelInstance, Entity bullet) {
		if (!handleCollisionsWithWalls(bullet)) {
			handleCollisionsWithOtherEntities(gameModelInstance, bullet);
		}
	}

	@Override
	public void update(final float deltaTime) {
		super.update(deltaTime);
		for (Entity bullet : bullets) {
			GameModelInstance gameModelInstance = ComponentsMapper.modelInstance.get(bullet).getModelInstance();
			BulletComponent bulletComponent = ComponentsMapper.bullet.get(bullet);
			handleCollisions(gameModelInstance, bullet);
			handleBulletMovement(gameModelInstance, bulletComponent);
			handleBulletMaxDistance(bullet, gameModelInstance, bulletComponent);
		}
	}

	private void handleBulletMaxDistance(Entity bullet,
										 GameModelInstance gameModelInstance,
										 BulletComponent bulletComponent) {
		Vector3 position = gameModelInstance.transform.getTranslation(auxVector3_1);
		float dst = bulletComponent.getInitialPosition(auxVector2_1).dst(position.x, position.z);
		if (dst >= BULLET_MAX_DISTANCE) {
			destroyBullet(bullet);
		}
	}

	private void handleBulletMovement(GameModelInstance gameModelInstance, BulletComponent bulletComponent) {
		Vector3 velocity = bulletComponent.getDirection(auxVector3_1).nor().scl(BULLET_SPEED);
		gameModelInstance.transform.trn(velocity);
	}

	@Override
	public Class<BulletSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return BulletSystemEventsSubscriber.class;
	}

	@Override
	public void initializeData( ) {
		bulletRicochetEffect = getAssetsManager().getParticleEffect(Assets.ParticleEffects.BULLET_RICOCHET);
	}

	@Override
	public void dispose( ) {

	}

}
