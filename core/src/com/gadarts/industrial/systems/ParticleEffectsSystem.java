package com.gadarts.industrial.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.g3d.particles.ParticleController;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.emitters.RegularEmitter;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.FlyingParticleComponent;
import com.gadarts.industrial.components.ParticleEffectComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.GameAssetsManager;

import java.util.ArrayList;

public class ParticleEffectsSystem extends GameSystem<SystemEventsSubscriber> {
	public static final int BULLET_JACKET_TIME_TO_LEAVE = 5000;
	public static final float FLYING_PART_ROTATION_STEP = 64F;
	public static final float GRAVITY_COEFF = 1.05F;
	private static final Matrix4 auxMatrix = new Matrix4();
	private static final Vector3 auxVector_1 = new Vector3();
	private static final Vector3 auxVector_2 = new Vector3();
	private static final Vector3 auxVector_3 = new Vector3();
	private final ArrayList<Entity> particleEntitiesToRemove = new ArrayList<>();
	private final ArrayList<ParticleEffect> particleEffectsToFollow = new ArrayList<>();
	private PointSpriteParticleBatch pointSpriteBatch;
	private ImmutableArray<Entity> particleEffectsEntities;
	private ImmutableArray<Entity> flyingParticlesEntities;

	public ParticleEffectsSystem(GameAssetsManager assetsManager,
								 GameLifeCycleHandler lifeCycleHandler) {
		super(assetsManager, lifeCycleHandler);
		pointSpriteBatch = new PointSpriteParticleBatch();
		getAssetsManager().loadParticleEffects(pointSpriteBatch);
	}

	@Override
	public void onSystemReset(SystemsCommonData systemsCommonData) {
		super.onSystemReset(systemsCommonData);
		particleEffectsEntities = getEngine().getEntitiesFor(Family.all(ParticleEffectComponent.class).get());
		flyingParticlesEntities = getEngine().getEntitiesFor(Family.all(FlyingParticleComponent.class).get());
		particleEntitiesToRemove.clear();
		particleEffectsToFollow.clear();
		getEngine().addEntityListener(new EntityListener() {
			@Override
			public void entityAdded(final Entity entity) {
				if (ComponentsMapper.particleEffect.has(entity)) {
					ParticleEffect effect = ComponentsMapper.particleEffect.get(entity).getParticleEffect();
					effect.init();
					effect.start();
					systemsCommonData.getParticleSystem().add(effect);
				}
			}

			@Override
			public void entityRemoved(final Entity entity) {
				if (ComponentsMapper.particleParent.has(entity)) {
					Array<Entity> children = ComponentsMapper.particleParent.get(entity).getChildren();
					for (Entity child : children) {
						finalizeEffect(child);
					}
				} else if (ComponentsMapper.particleEffect.has(entity)) {
					finalizeEffect(entity);
					particleEffectsToFollow.add(ComponentsMapper.particleEffect.get(entity).getParticleEffect());
				}
			}
		});
	}

	private void finalizeEffect(final Entity effect) {
		for (ParticleController con : ComponentsMapper.particleEffect.get(effect).getParticleEffect().getControllers()) {
			RegularEmitter emitter = (RegularEmitter) con.emitter;
			emitter.setContinuous(false);
		}
	}

	private void updatesEffectsWithParentsAccordingly( ) {
		for (Entity particleEntity : particleEffectsEntities) {
			ParticleEffectComponent particleEffectComponent = ComponentsMapper.particleEffect.get(particleEntity);
			ParticleEffect particleEffect = particleEffectComponent.getParticleEffect();
			if (particleEffectComponent.getParent() != null && ComponentsMapper.modelInstance.has(particleEffectComponent.getParent())) {
				particleEffect.setTransform(auxMatrix.idt());
				ModelInstanceComponent miComponent = ComponentsMapper.modelInstance.get(particleEffectComponent.getParent());
				particleEffect.translate(miComponent.getModelInstance().transform.getTranslation(auxVector_1));
			}
		}
	}

	private void handleCompletedParticleEffects( ) {
		particleEntitiesToRemove.clear();
		addCompleteToRemove();
		removeParticleEffectsMarkedToBeRemoved();
		removeFollowedEffectsThatComplete();
	}

	private void removeFollowedEffectsThatComplete( ) {
		particleEntitiesToRemove.clear();
		for (int i = 0; i < particleEffectsToFollow.size(); i++) {
			ParticleEffect effect = particleEffectsToFollow.remove(i);
			if (effect.isComplete()) {
				getSystemsCommonData().getParticleSystem().remove(effect);
				i--;
			}
		}
	}

	private void removeParticleEffectsMarkedToBeRemoved( ) {
		for (Entity entity : particleEntitiesToRemove) {
			if (ComponentsMapper.particleEffect.has(entity)) {
				ParticleSystem particleSystem = getSystemsCommonData().getParticleSystem();
				particleSystem.remove(ComponentsMapper.particleEffect.get(entity).getParticleEffect());
			}
			getEngine().removeEntity(entity);
		}
	}

	private void addCompleteToRemove( ) {
		for (Entity entity : particleEffectsEntities) {
			ParticleEffect particleEffect = ComponentsMapper.particleEffect.get(entity).getParticleEffect();
			if (particleEffect.isComplete()) {
				particleEntitiesToRemove.add(entity);
			}
		}
	}

	@Override
	public void update(final float deltaTime) {
		updatesEffectsWithParentsAccordingly();
		hideParticlesInFow();
		updateSystem(deltaTime);
		handleCompletedParticleEffects();
		updateFlyingParticles();
	}

	private void hideParticlesInFow( ) {
		for (Entity particleEntity : particleEffectsEntities) {
			ParticleEffectComponent particleEffectComponent = ComponentsMapper.particleEffect.get(particleEntity);
			ParticleEffect particleEffect = particleEffectComponent.getParticleEffect();
			Vector3 position = particleEffect.getControllers().get(0).transform.getTranslation(auxVector_1);
			MapGraphNode node = getSystemsCommonData().getMap().getNode(position);
			if (node != null && node.getEntity() != null && !ComponentsMapper.floor.get(node.getEntity()).isRevealed()) {
				particleEffect.end();
			}
		}
	}

	private void updateSystem(float deltaTime) {
		ParticleSystem particleSystem = getSystemsCommonData().getParticleSystem();
		particleSystem.update(deltaTime);
		particleSystem.begin();
		particleSystem.draw();
		particleSystem.end();
	}

	private void updateFlyingParticles( ) {
		particleEntitiesToRemove.clear();
		for (Entity flyingPart : flyingParticlesEntities) {
			updateFlyingParticle(flyingPart);
		}
		removeParticleEffectsMarkedToBeRemoved();
	}

	private void updateFlyingParticle(Entity flyingPart) {
		GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(flyingPart).getModelInstance();
		FlyingParticleComponent flyingPartComponent = ComponentsMapper.flyingParticles.get(flyingPart);
		if (flyingPartComponent.getDestroyTime() < 0) {
			takeStepForFlyingParticle(flyingPart, modelInstance);
			modelInstance.transform.rotate(Vector3.Z, FLYING_PART_ROTATION_STEP);
			startCountDownForFlyingParticleWhenReachesGround(modelInstance, flyingPartComponent);
		} else if (TimeUtils.timeSinceMillis(flyingPartComponent.getDestroyTime()) > 0) {
			flyingPartComponent.setDestroyTime(0);
			particleEntitiesToRemove.add(flyingPart);
		}
	}

	private void startCountDownForFlyingParticleWhenReachesGround(GameModelInstance modelInstance,
																  FlyingParticleComponent flyingPartComponent) {
		if (modelInstance.transform.getTranslation(auxVector_1).y <= flyingPartComponent.getNodeHeight()) {
			modelInstance.transform.setTranslation(auxVector_1.x, auxVector_1.y + 0.1F, auxVector_1.z);
			flyingPartComponent.setDestroyTime(TimeUtils.millis() + BULLET_JACKET_TIME_TO_LEAVE);
		}
	}

	private void takeStepForFlyingParticle(Entity flyingParticle,
										   GameModelInstance modelInstance) {
		FlyingParticleComponent flyingParticleComponent = ComponentsMapper.flyingParticles.get(flyingParticle);
		Vector3 step = flyingParticleComponent.getFlyAwayForce(auxVector_2);
		modelInstance.transform.trn(auxVector_3.set(step));
		step.scl(flyingParticleComponent.getDeceleration());
		flyingParticleComponent.setFlyAwayForce(step);
		Vector3 gravityForce = flyingParticleComponent.getGravityForce(auxVector_2);
		modelInstance.transform.trn(gravityForce);
		flyingParticleComponent.setGravityForce(gravityForce.scl(GRAVITY_COEFF));
	}

	@Override
	public Class<SystemEventsSubscriber> getEventsSubscriberClass( ) {
		return null;
	}

	@Override
	public void initializeData( ) {
		ParticleSystem particleSystem = new ParticleSystem();
		getSystemsCommonData().setParticleSystem(particleSystem);
		particleSystem.add(pointSpriteBatch);
		pointSpriteBatch.setCamera(getSystemsCommonData().getCamera());
	}

	@Override
	public void dispose( ) {
	}
}
