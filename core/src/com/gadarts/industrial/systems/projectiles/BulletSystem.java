package com.gadarts.industrial.systems.projectiles;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.BulletComponent;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.collision.CollisionComponent;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.shared.model.map.MapNodesTypes;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.utils.EntityBuilder;

import java.util.HashMap;
import java.util.Map;

public class BulletSystem extends GameSystem<BulletSystemEventsSubscriber> implements CharacterSystemEventsSubscriber {
	private static final float BULLET_SPEED = 0.2f;
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final float BULLET_MAX_DISTANCE = 14;
	private static final Vector3 auxVector3_1 = new Vector3();
	private final static float PROJ_LIGHT_INTENSITY = 0.05F;
	private final static float PROJ_LIGHT_RADIUS = 1F;
	private final static Color PROJ_LIGHT_COLOR = Color.valueOf("#8396FF");
	private final Map<Assets.Models, Pool<GameModelInstance>> pooledBulletModels = new HashMap<>();
	private ImmutableArray<Entity> bullets;
	private ImmutableArray<Entity> collidables;

	public BulletSystem(SystemsCommonData systemsCommonData,
						GameAssetsManager assetsManager,
						GameLifeCycleHandler lifeCycleHandler) {
		super(systemsCommonData, assetsManager, lifeCycleHandler);
	}

	@Override
	public void onCharacterEngagesPrimaryAttack(final Entity character,
												final Vector3 direction,
												final Vector3 charPos) {

		if (ComponentsMapper.enemy.has(character)) {
			enemyEngagesPrimaryAttack(character, direction, charPos);
		}
	}

	private void enemyEngagesPrimaryAttack(final Entity character, final Vector3 direction, final Vector3 charPos) {
		EnemyComponent enemyComp = ComponentsMapper.enemy.get(character);
		getSystemsCommonData().getSoundPlayer().playSound(Assets.Sounds.ATTACK_ENERGY_BALL);
		createEnemyBullet(character, direction, charPos, enemyComp);
	}

	private void createEnemyBullet(Entity character,
								   Vector3 direction,
								   Vector3 charPos,
								   EnemyComponent enemyComp) {
		charPos.y += ComponentsMapper.enemy.get(character).getEnemyDefinition().getHeight() / 2F;
		Integer damagePoints = enemyComp.getEnemyDefinition().getPrimaryAttack().getDamage();
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
		getSystemsCommonData().getSoundPlayer().playSound(Assets.Sounds.SMALL_EXP);
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

	}

	@Override
	public void dispose( ) {

	}

}
