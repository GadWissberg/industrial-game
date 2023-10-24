package com.gadarts.industrial.utils;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pools;
import com.gadarts.industrial.components.*;
import com.gadarts.industrial.components.animation.AnimationComponent;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.*;
import com.gadarts.industrial.components.collision.CollisionComponent;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import com.gadarts.industrial.components.floor.FloorComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
import com.gadarts.industrial.components.player.Item;
import com.gadarts.industrial.components.player.PlayerComponent;
import com.gadarts.industrial.components.player.Weapon;
import com.gadarts.industrial.components.sd.SimpleDecalComponent;
import com.gadarts.industrial.components.sll.ShadowlessLightComponent;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.declarations.EnvironmentObjectDeclaration;
import com.gadarts.industrial.shared.assets.declarations.characters.enemies.EnemyDeclaration;
import com.gadarts.industrial.shared.assets.declarations.pickups.ItemDeclaration;
import com.gadarts.industrial.shared.assets.declarations.pickups.weapons.PlayerWeaponDeclaration;
import com.gadarts.industrial.shared.assets.declarations.pickups.weapons.WeaponDeclaration;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import com.gadarts.industrial.shared.model.env.door.DoorsDefinitions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

import static com.gadarts.industrial.shared.model.characters.CharacterTypes.BILLBOARD_SCALE;

/**
 * A builder to create an entity instance and append it components.
 */
public class EntityBuilder {
	public static final String MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST = "Call beginBuildingEntity() first!";

	@Getter
	private static final EntityBuilder instance = new EntityBuilder();
	private static final Vector2 auxVector2 = new Vector2();
	@Setter(AccessLevel.PRIVATE)
	private PooledEngine engine;

	@Getter
	private Entity currentEntity;

	public static EntityBuilder beginBuildingEntity(final PooledEngine engine) {
		instance.init(engine);
		return instance;
	}

	public EntityBuilder addBulletComponent(Vector3 initialPos,
											Vector3 direction,
											Entity owner,
											Integer damagePoints,
											WeaponDeclaration weaponDeclaration) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		BulletComponent bulletComp = engine.createComponent(BulletComponent.class);
		bulletComp.init(auxVector2.set(initialPos.x, initialPos.z), direction, owner, damagePoints, weaponDeclaration);
		currentEntity.add(bulletComp);
		return instance;
	}

	public EntityBuilder addAnimationComponent(final float frameDuration,
											   final Animation<TextureAtlas.AtlasRegion> animation) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		AnimationComponent animComponent = engine.createComponent(AnimationComponent.class);
		currentEntity.add(animComponent);
		Optional.ofNullable(animation).ifPresent(a -> animComponent.init(frameDuration, animation));
		return instance;
	}

	public void addShadowlessLightComponent(final Vector3 position,
											final float intensity,
											final float radius,
											final Color color) {
		addShadowlessLightComponent(position, intensity, radius, color, 0F, false);
	}

	public EntityBuilder addShadowlessLightComponent(final Vector3 position,
													 final float intensity,
													 final float radius,
													 final Color color,
													 final float duration) {
		return addShadowlessLightComponent(position, intensity, radius, color, duration, false);
	}

	public EntityBuilder addShadowlessLightComponent(final Vector3 position,
													 final float intensity,
													 final float radius,
													 final Color color,
													 final float duration,
													 final boolean flicker) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		ShadowlessLightComponent lightComponent = engine.createComponent(ShadowlessLightComponent.class);
		lightComponent.init(position, intensity, radius, currentEntity, flicker);
		if (color != null) {
			lightComponent.applyColor(color);
		}
		lightComponent.applyDuration(duration);
		currentEntity.add(lightComponent);
		return instance;
	}

	public void addEnvironmentObjectComponent(Vector2 topLeft,
											  Vector2 bottomRight,
											  EnvironmentObjectDeclaration type) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		EnvironmentObjectComponent environmentObjectComponent = engine.createComponent(EnvironmentObjectComponent.class);
		environmentObjectComponent.init(topLeft, bottomRight, type);
		currentEntity.add(environmentObjectComponent);
	}

	public EntityBuilder addModelInstanceComponent(GameModelInstance modelInstance) {
		return addModelInstanceComponent(modelInstance, true);
	}

	public EntityBuilder addModelInstanceComponent(GameModelInstance modelInstance,
												   boolean visible) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		ModelInstanceComponent component = engine.createComponent(ModelInstanceComponent.class);
		component.init(modelInstance, visible);
		currentEntity.add(component);
		component.getModelInstance().userData = currentEntity;
		return instance;
	}

	public EntityBuilder addFloorComponent(MapGraphNode node, Assets.SurfaceTextures definition) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		FloorComponent floorComponent = engine.createComponent(FloorComponent.class);
		floorComponent.init(node, definition);
		currentEntity.add(floorComponent);
		return instance;
	}

	public EntityBuilder addPickUpComponentAsWeapon(PlayerWeaponDeclaration declaration,
													Texture displayImage,
													TextureAtlas.AtlasRegion bulletRegion) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		Weapon weapon = (Weapon) addPickUpComponent(Weapon.class, declaration, displayImage);
		weapon.setBulletTextureRegion(bulletRegion);
		return instance;
	}

	public EntityBuilder addWallComponent(final MapGraphNode parentNode) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		WallComponent component = engine.createComponent(WallComponent.class);
		component.init(parentNode);
		currentEntity.add(component);
		return instance;
	}

	public Entity finishAndAddToEngine( ) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		engine.addEntity(currentEntity);
		return finish();
	}

	public Entity finish( ) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		Entity result = currentEntity;
		instance.reset();
		return result;
	}

	public EntityBuilder addSimpleDecalComponent(final Vector3 position,
												 final TextureRegion textureRegion,
												 final boolean billboard,
												 final boolean animatedByAnimationComponent) {
		return addSimpleDecalComponent(position, textureRegion, Vector3.Zero, billboard, animatedByAnimationComponent);
	}

	public EntityBuilder addSimpleDecalComponent(final Vector3 position,
												 final TextureRegion textureRegion,
												 final Vector3 rotationAroundAxis,
												 final boolean billboard,
												 final boolean animatedByAnimationComponent) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		SimpleDecalComponent simpleDecalComponent = engine.createComponent(SimpleDecalComponent.class);
		simpleDecalComponent.init(textureRegion, true, billboard, animatedByAnimationComponent);
		Decal decal = simpleDecalComponent.getDecal();
		initializeSimpleDecal(position, rotationAroundAxis, decal);
		currentEntity.add(simpleDecalComponent);
		return instance;
	}

	public void addAnimationComponent( ) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		AnimationComponent animComponent = engine.createComponent(AnimationComponent.class);
		currentEntity.add(animComponent);
	}

	public EntityBuilder addCollisionComponent( ) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		CollisionComponent collisionComponent = engine.createComponent(CollisionComponent.class);
		currentEntity.add(collisionComponent);
		return instance;
	}

	public EntityBuilder addCharacterDecalComponent(final CharacterAnimations animations,
													final SpriteType spriteType,
													final Direction direction,
													final Vector3 position) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		CharacterDecalComponent characterDecalComponent = engine.createComponent(CharacterDecalComponent.class);
		characterDecalComponent.init(animations, spriteType, direction, position);
		currentEntity.add(characterDecalComponent);
		return instance;
	}

	public EntityBuilder addParticleEffectComponent(ParticleEffect originalEffect,
													Vector3 position) {
		return addParticleEffectComponent(engine, originalEffect, position, null);
	}

	public EntityBuilder addParticleEffectComponent(final PooledEngine engine,
													final ParticleEffect originalEffect,
													final Vector3 position,
													final Entity parent) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		ParticleEffect effect = originalEffect.copy();
		addParticleEffectParentComponentIfNeeded(engine, parent);
		ParticleEffectComponent particleEffectComponent = engine.createComponent(ParticleEffectComponent.class);
		particleEffectComponent.init(effect, parent);
		effect.translate(position);
		currentEntity.add(particleEffectComponent);
		return instance;
	}

	public EntityBuilder addEnemyComponent(final EnemyDeclaration enemyDefinition,
										   final Animation<TextureAtlas.AtlasRegion> bulletRegions) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		EnemyComponent component = engine.createComponent(EnemyComponent.class);
		component.init(enemyDefinition, bulletRegions);
		currentEntity.add(component);
		return instance;
	}

	public EntityBuilder addCharacterComponent(CharacterSpriteData characterSpriteData,
											   CharacterSoundData characterSoundData,
											   CharacterSkillsParameters skills,
											   WeaponDeclaration primaryAttack,
											   Direction direction) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		CharacterComponent charComponent = engine.createComponent(CharacterComponent.class);
		charComponent.init(characterSpriteData, characterSoundData, skills, primaryAttack, direction);
		currentEntity.add(charComponent);
		return instance;
	}

	public EntityBuilder addPlayerComponent(CharacterAnimations general) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		PlayerComponent playerComponent = engine.createComponent(PlayerComponent.class);
		playerComponent.init(general);
		currentEntity.add(playerComponent);
		return instance;
	}

	public EntityBuilder addStaticLightComponent(Vector3 position, float intensity, float radius) {
		return addStaticLightComponent(position, intensity, radius, Color.WHITE);
	}

	public EntityBuilder addStaticLightComponent(Vector3 position, float intensity, float radius, Color color) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		StaticLightComponent lightComponent = engine.createComponent(StaticLightComponent.class);
		lightComponent.init(position, intensity, radius, color);
		currentEntity.add(lightComponent);
		return instance;
	}

	public void addAppendixModelInstanceComponent(GameModelInstance modelInstance) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		AppendixModelInstanceComponent component = engine.createComponent(AppendixModelInstanceComponent.class);
		component.init(modelInstance);
		component.getModelInstance().userData = currentEntity;
		currentEntity.add(component);
	}

	public void addDoorComponent(MapGraphNode node, DoorsDefinitions type) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		DoorComponent doorComponent = engine.createComponent(DoorComponent.class);
		doorComponent.init(node, type);
		currentEntity.add(doorComponent);
	}

	public EntityBuilder addFlyingParticleComponent(float nodeHeight,
													float strength,
													float deceleration,
													float minimumDegree,
													float maxDegreeToAdd) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		FlyingParticleComponent component = engine.createComponent(FlyingParticleComponent.class);
		component.init(nodeHeight, strength, deceleration, minimumDegree, maxDegreeToAdd);
		currentEntity.add(component);
		return instance;
	}

	public EntityBuilder addSimpleShadowComponent(float radius) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		SimpleShadowComponent component = engine.createComponent(SimpleShadowComponent.class);
		component.init(radius);
		currentEntity.add(component);
		return instance;
	}

	public EntityBuilder addCursorComponent( ) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		CursorComponent component = engine.createComponent(CursorComponent.class);
		currentEntity.add(component);
		return instance;
	}

	private void init(final PooledEngine engine) {
		this.engine = engine;
		this.currentEntity = engine.createEntity();
	}

	private Item addPickUpComponent(Class<? extends Item> type,
									ItemDeclaration declaration,
									Texture displayImage) {
		if (currentEntity == null) throw new RuntimeException(MSG_FAIL_CALL_BEGIN_BUILDING_ENTITY_FIRST);
		Item pickup = Pools.obtain(type);
		pickup.init(declaration, 0, 0, displayImage);
		PickUpComponent pickupComponent = engine.createComponent(PickUpComponent.class);
		pickupComponent.setItem(pickup);
		currentEntity.add(pickupComponent);
		return pickup;
	}

	private void reset( ) {
		engine = null;
		currentEntity = null;
	}

	private void initializeSimpleDecal(final Vector3 position, final Vector3 rotationAroundAxis, final Decal decal) {
		decal.setPosition(position);
		decal.setScale(BILLBOARD_SCALE);
		rotateSimpleDecal(decal, rotationAroundAxis);
	}

	private void rotateSimpleDecal(final Decal decal, final Vector3 rotationAroundAxis) {
		if (!rotationAroundAxis.isZero()) {
			decal.setRotation(rotationAroundAxis.y, rotationAroundAxis.x, rotationAroundAxis.z);
		}
	}

	private void addParticleEffectParentComponentIfNeeded(PooledEngine engine, Entity parent) {
		if (parent != null) {
			ParticleEffectParentComponent particleComponentParent;
			if (!ComponentsMapper.particleParent.has(parent)) {
				particleComponentParent = engine.createComponent(ParticleEffectParentComponent.class);
			} else {
				particleComponentParent = ComponentsMapper.particleParent.get(parent);
			}
			particleComponentParent.getChildren().add(currentEntity);
		}
	}


}
