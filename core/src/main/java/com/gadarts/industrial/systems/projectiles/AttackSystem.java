package com.gadarts.industrial.systems.projectiles;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.components.BulletComponent;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.character.OnGoingAttack;
import com.gadarts.industrial.components.collision.CollisionComponent;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import com.gadarts.industrial.components.mi.AdditionalRenderData;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.player.Weapon;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.screens.GameLifeCycleManager;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.assets.declarations.characters.CharacterDeclaration;
import com.gadarts.industrial.shared.assets.declarations.pickups.weapons.PlayerWeaponDeclaration;
import com.gadarts.industrial.shared.assets.declarations.pickups.weapons.WeaponDeclaration;
import com.gadarts.industrial.shared.model.characters.player.PlayerDeclaration;
import com.gadarts.industrial.shared.model.map.MapNodesTypes;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.ModelInstancePools;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.utils.EntityBuilder;

import static com.gadarts.industrial.components.DoorComponent.DoorStates;
import static com.gadarts.industrial.components.player.PlayerComponent.PLAYER_HEIGHT;

public class AttackSystem extends GameSystem<AttackSystemEventsSubscriber> implements CharacterSystemEventsSubscriber {
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final float BULLET_MAX_DISTANCE = 14;
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final Vector3 auxVector3_2 = new Vector3();
	private final static float PROJ_LIGHT_INTENSITY = 0.05F;
	private final static float PROJ_LIGHT_RADIUS = 1F;
	private static final float BULLET_ENGAGE_LIGHT_DURATION = 0.1F;
	private static final float JACKET_FLY_AWAY_STRENGTH = 0.1F;
	private static final float JACKET_FLY_AWAY_MIN_DEGREE = 45F;
	private static final float JACKET_FLY_AWAY_MAX_DEGREE_TO_ADD = 90F;
	private static final float JACKET_FLY_AWAY_DEC = 0.9F;
	private static final BoundingBox auxBoundingBox1 = new BoundingBox();
	private static final BoundingBox auxBoundingBox2 = new BoundingBox();
	private static final float BULLET_BOUNDING_BOX_SIZE = 0.1F;
	private final static Quaternion rotationQuaternion = new Quaternion();
	private ImmutableArray<Entity> bullets;
	private ImmutableArray<Entity> collidables;

	public AttackSystem(GameAssetManager assetsManager, GameLifeCycleManager gameLifeCycleManager) {
		super(assetsManager, gameLifeCycleManager);
	}

	private Vector3 transformBulletModel(Entity character,
										 Vector3 position,
										 Vector3 bulletCreationOffset,
										 GameModelInstance modelInstance) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		Entity target = characterComponent.getTarget();
		Vector3 targetPos = ComponentsMapper.characterDecal.get(target).getDecal().getPosition();
		bulletCreationOffset = auxVector3_2.set(bulletCreationOffset);
		bulletCreationOffset.rotate(auxVector3_1.set(0F, -1F, 0F), characterComponent.getFacingDirection().getDirection(auxVector2_1).angleDeg());
		var biasedPosition = modelInstance.transform.setToTranslation(position).translate(bulletCreationOffset).getTranslation(auxVector3_1);
		Vector3 bulletDirectionAfterBias = auxVector3_2.set(targetPos.x, targetPos.y, targetPos.z)
				.sub(biasedPosition.x, biasedPosition.y, biasedPosition.z)
				.nor();
		rotationQuaternion.setFromCross(Vector3.X, bulletDirectionAfterBias);
		modelInstance.transform
				.set(bulletDirectionAfterBias, rotationQuaternion)
				.setTranslation(auxVector3_1.set(biasedPosition.x, biasedPosition.y, biasedPosition.z));
		return bulletDirectionAfterBias;
	}

	@Override
	public void onCharacterEngagesPrimaryAttack(final Entity character,
												final Vector3 direction,
												final Vector3 charPos) {

		if (ComponentsMapper.enemy.has(character)) {
			enemyEngagesPrimaryAttack(character, charPos);
		} else if (ComponentsMapper.player.has(character)) {
			playerEngagesSelectedWeapon(character, charPos);
		}
	}

	private void playerEngagesSelectedWeapon(Entity character, Vector3 charPos) {
		Weapon selectedWeapon = getSystemsCommonData().getStorage().getSelectedWeapon();
		PlayerWeaponDeclaration playerWeaponDeclaration = (PlayerWeaponDeclaration) selectedWeapon.getDeclaration();
		CharacterDeclaration playerDefinition = PlayerDeclaration.getInstance();
		SoundPlayer soundPlayer = getSystemsCommonData().getSoundPlayer();
		WeaponDeclaration weaponDeclaration = playerWeaponDeclaration.declaration();
		boolean melee = weaponDeclaration.melee();
		soundPlayer.playSound(melee ? playerDefinition.getSoundMelee() : weaponDeclaration.soundEngage());
		primaryAttackEngaged(
				character,
				charPos,
				playerDefinition.getBulletCreationOffset(auxVector3_1.setZero()),
				weaponDeclaration);
	}

	private void enemyEngagesPrimaryAttack(Entity character, Vector3 charPos) {
		EnemyComponent enemyComp = ComponentsMapper.enemy.get(character);
		WeaponDeclaration primaryAttack = enemyComp.getEnemyDeclaration().attackPrimary();
		getSystemsCommonData().getSoundPlayer().playSound(primaryAttack.soundEngage());
		Vector3 bulletCreationOffset = ComponentsMapper.enemy.get(character).getEnemyDeclaration().bulletCreationOffset();
		if (bulletCreationOffset == null) {
			bulletCreationOffset = auxVector3_1.setZero().add(0F, PLAYER_HEIGHT / 2F, 0F);
		}
		primaryAttackEngaged(character, charPos, bulletCreationOffset, primaryAttack);
	}

	private void primaryAttackEngaged(Entity character,
									  Vector3 charPos,
									  Vector3 bulletCreationOffset,
									  WeaponDeclaration primaryAttack) {
		if (primaryAttack.melee()) {
			subscribers.forEach(subscriber -> {
				Entity target = ComponentsMapper.character.get(character).getTarget();
				subscriber.onMeleeAttackAppliedOnTarget(character, target, primaryAttack);
			});
		} else {
			createBullet(character, charPos, primaryAttack, bulletCreationOffset);
		}
	}

	private void createBullet(Entity character,
							  Vector3 position,
							  WeaponDeclaration weaponDeclaration,
							  Vector3 bulletCreationOffset) {
		ModelInstancePools pooledModelInstances = getSystemsCommonData().getPooledModelInstances();
		GameModelInstance modelInstance = pooledModelInstances.obtain(getAssetsManager(), weaponDeclaration.bulletModel());
		Vector3 bulletDirection = transformBulletModel(character, position, bulletCreationOffset, modelInstance);
		createBulletEntity(character, position, weaponDeclaration, modelInstance, bulletDirection);
		applyBulletLight(position, weaponDeclaration);
		applyBulletJacket(character, position, weaponDeclaration, pooledModelInstances);
	}

	private void applyBulletJacket(Entity character,
								   Vector3 position,
								   WeaponDeclaration weaponDeclaration,
								   ModelInstancePools pooledModelInstances) {
		Assets.Models bulletJacket = weaponDeclaration.bulletJacket();
		if (bulletJacket != null) {
			Vector3 nodePosition = ComponentsMapper.characterDecal.get(character).getNodePosition(auxVector3_1);
			MapGraphNode currentNode = getSystemsCommonData().getMap().getNode(nodePosition);
			GameModelInstance jacketGameModelInstance = pooledModelInstances.obtain(getAssetsManager(), bulletJacket);
			jacketGameModelInstance.transform.setToTranslation(position).rotate(Vector3.Y, MathUtils.random(360F));
			EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
					.addModelInstanceComponent(jacketGameModelInstance)
					.addFlyingParticleComponent(
							currentNode.getHeight(),
							JACKET_FLY_AWAY_STRENGTH,
							JACKET_FLY_AWAY_DEC,
							JACKET_FLY_AWAY_MIN_DEGREE, JACKET_FLY_AWAY_MAX_DEGREE_TO_ADD)
					.finishAndAddToEngine();
		}
	}

	private void applyBulletLight(Vector3 position, WeaponDeclaration weaponDeclaration) {
		if (weaponDeclaration.lightOnCreation()) {
			EntityBuilder.beginBuildingEntity((PooledEngine) getEngine()).addShadowlessLightComponent(
					position,
					PROJ_LIGHT_INTENSITY,
					PROJ_LIGHT_RADIUS,
					weaponDeclaration.bulletLightColor(),
					BULLET_ENGAGE_LIGHT_DURATION).finishAndAddToEngine();
		}
	}

	private void createBulletEntity(Entity character,
									Vector3 position,
									WeaponDeclaration declaration,
									GameModelInstance modelInstance,
									Vector3 bulletDirection) {
		EntityBuilder builder = EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
				.addBulletComponent(
						position,
						auxVector3_1.set(bulletDirection.x, bulletDirection.y, bulletDirection.z),
						character,
						declaration.damage(),
						declaration)
				.addModelInstanceComponent(modelInstance, true);
		Color color = declaration.bulletLightColor();
		if (color != null) {
			builder.addShadowlessLightComponent(position, PROJ_LIGHT_INTENSITY, PROJ_LIGHT_RADIUS, color);
		}
		builder.finishAndAddToEngine();
	}

	@Override
	public void onSystemReset(SystemsCommonData systemsCommonData) {
		super.onSystemReset(systemsCommonData);
		Engine engine = getEngine();
		bullets = engine.getEntitiesFor(Family.all(BulletComponent.class).get());
		collidables = engine.getEntitiesFor(Family.all(CollisionComponent.class).get());
	}

	private boolean handleCollisionsWithWalls(final Entity bullet) {
		GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(bullet).getModelInstance();
		Vector3 pos = modelInstance.transform.getTranslation(auxVector3_1);
		MapGraph map = getSystemsCommonData().getMap();
		if (pos.x < 0 || pos.x >= map.getWidth() || pos.z < 0 || pos.z >= map.getDepth()) return true;

		MapGraphNode node = map.getNode(pos);
		if (node.getHeight() >= pos.y) {
			onCollisionWithWall(bullet, node);
			return true;
		}
		return false;
	}

	private void onCollisionWithWall(final Entity bullet, final MapGraphNode node) {
		for (AttackSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onBulletCollisionWithWall(bullet, node);
		}
		destroyBullet(bullet);
	}

	private void destroyBullet(final Entity bullet) {
		WeaponDeclaration weaponDefinition = ComponentsMapper.bullet.get(bullet).getWeaponDeclaration();
		if (bullets.size() == 1) {
			Entity owner = ComponentsMapper.bullet.get(bullet).getOwner();
			OnGoingAttack onGoingAttack = ComponentsMapper.character.get(owner).getOnGoingAttack();
			if (onGoingAttack != null && onGoingAttack.isDone()) {
				for (AttackSystemEventsSubscriber subscriber : subscribers) {
					subscriber.onBulletSetDestroyed(bullet);
				}
			}
		}
		bullet.remove(BulletComponent.class);
		getEngine().removeEntity(bullet);
		GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(bullet).getModelInstance();
		Vector3 pos = auxVector3_1.set(modelInstance.transform.getTranslation(auxVector3_1));
		createExplosion(getAssetsManager().getParticleEffect(weaponDefinition.bulletExplosion()), pos);
	}

	private void createExplosion(final ParticleEffect effect, final Vector3 pos) {
		EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
				.addParticleEffectComponent(effect, pos)
				.finishAndAddToEngine();
		getSystemsCommonData().getSoundPlayer().playSound(Assets.Sounds.SMALL_EXP);
	}

	private boolean checkCollisionWithCharacter(GameModelInstance gameModelInstance, Entity collidable) {
		Vector3 colPos = ComponentsMapper.characterDecal.get(collidable).getDecal().getPosition();
		boolean alive = ComponentsMapper.character.get(collidable).getAttributes().getHealthData().getHp() > 0;
		Vector3 position = gameModelInstance.transform.getTranslation(auxVector3_1);
		float distance = auxVector2_1.set(colPos.x, colPos.z).dst(position.x, position.z);
		return alive && distance < CharacterComponent.CHAR_RAD;
	}

	private boolean checkCollision(GameModelInstance gameModelInstance, Entity collidable) {
		if (ComponentsMapper.characterDecal.has(collidable)) {
			return checkCollisionWithCharacter(gameModelInstance, collidable);
		} else if (ComponentsMapper.environmentObject.has(collidable)) {
			MapNodesTypes nodeType = ComponentsMapper.environmentObject.get(collidable).getType().getNodeType();
			boolean isDoor = ComponentsMapper.door.has(collidable);
			if (nodeType != MapNodesTypes.PASSABLE_NODE || (isDoor && ComponentsMapper.door.get(collidable).getState() == DoorStates.CLOSED)) {
				return checkCollisionWithEnvObject(gameModelInstance, collidable);
			}
		}
		return false;
	}

	private boolean checkCollisionWithEnvObject(GameModelInstance bulletGameModelInstance, Entity collidable) {
		GameModelInstance envModelInstance = ComponentsMapper.modelInstance.get(collidable).getModelInstance();
		AdditionalRenderData envRenderData = envModelInstance.getAdditionalRenderData();
		BoundingBox collidableBoundingBox = envRenderData.getBoundingBox(auxBoundingBox1).mul(envModelInstance.transform);
		float halfSize = BULLET_BOUNDING_BOX_SIZE / 2F;
		Vector3 min = auxVector3_1.set(-halfSize, -halfSize, -halfSize);
		Vector3 max = auxVector3_2.set(halfSize, halfSize, halfSize);
		var bulletBoundingBox = auxBoundingBox2.set(min, max).mul(bulletGameModelInstance.transform);
		return collidableBoundingBox.intersects(bulletBoundingBox);
	}

	private void handleCollisionsWithOtherEntities(GameModelInstance gameModelInstance, Entity bullet) {
		for (Entity collidable : collidables) {
			if ((!DebugSettings.DISABLE_BULLET_COLLISION_WITH_CHARACTERS || !ComponentsMapper.character.has(collidable))) {
				if (ComponentsMapper.bullet.get(bullet).getOwner() != collidable) {
					if (checkCollision(gameModelInstance, collidable)) {
						onProjectileCollisionWithAnotherEntity(bullet, collidable);
						break;
					}
				}
			}
		}
	}

	private void onProjectileCollisionWithAnotherEntity(final Entity bullet, final Entity collidable) {
		for (AttackSystemEventsSubscriber subscriber : subscribers) {
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
		float bulletSpeed = bulletComponent.getWeaponDeclaration().bulletSpeed();
		Vector3 velocity = bulletComponent.getDirection(auxVector3_1).nor().scl(bulletSpeed);
		gameModelInstance.transform.trn(velocity);
	}

	@Override
	public Class<AttackSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return AttackSystemEventsSubscriber.class;
	}

	@Override
	public void initializeData( ) {

	}

	@Override
	public void dispose( ) {

	}

}
