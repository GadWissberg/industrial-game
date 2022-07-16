package com.gadarts.industrial.systems;

import com.badlogic.ashley.core.Engine;
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
import com.badlogic.gdx.utils.Array;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.ParticleComponent;
import com.gadarts.industrial.shared.assets.GameAssetsManager;

import java.util.ArrayList;

public class ParticleEffectsSystem extends GameSystem<SystemEventsSubscriber> {
	private static final Matrix4 auxMatrix = new Matrix4();
	private final ArrayList<Entity> particleEntitiesToRemove = new ArrayList<>();
	private final ArrayList<ParticleEffect> particleEffectsToFollow = new ArrayList<>();
	private final ArrayList<ParticleEffect> particleEffectsToRemove = new ArrayList<>();
	private PointSpriteParticleBatch pointSpriteBatch;
	private ImmutableArray<Entity> particleEntities;

	public ParticleEffectsSystem(SystemsCommonData systemsCommonData,
								 GameAssetsManager assetsManager,
								 GameLifeCycleHandler lifeCycleHandler) {
		super(systemsCommonData, assetsManager, lifeCycleHandler);
		if (pointSpriteBatch == null) {
			pointSpriteBatch = new PointSpriteParticleBatch();
		}
		getAssetsManager().loadParticleEffects(pointSpriteBatch);
	}

	@Override
	public void addedToEngine(Engine engine) {
		super.addedToEngine(engine);
		particleEntities = getEngine().getEntitiesFor(Family.all(ParticleComponent.class).get());
		getEngine().addEntityListener(new EntityListener() {
			@Override
			public void entityAdded(final Entity entity) {
				if (ComponentsMapper.particle.has(entity)) {
					ParticleEffect effect = ComponentsMapper.particle.get(entity).getParticleEffect();
					effect.init();
					effect.start();
					getSystemsCommonData().getParticleSystem().add(effect);
				}
			}

			@Override
			public void entityRemoved(final Entity entity) {
				if (ComponentsMapper.particleParent.has(entity)) {
					Array<Entity> children = ComponentsMapper.particleParent.get(entity).getChildren();
					for (Entity child : children) {
						finalizeEffect(child);
					}
				} else if (ComponentsMapper.particle.has(entity)) {
					finalizeEffect(entity);
					particleEffectsToFollow.add(ComponentsMapper.particle.get(entity).getParticleEffect());
				}
			}
		});
	}

	private void finalizeEffect(final Entity effect) {
		for (ParticleController con : ComponentsMapper.particle.get(effect).getParticleEffect().getControllers()) {
			RegularEmitter emitter = (RegularEmitter) con.emitter;
			emitter.setContinuous(false);
		}
	}

	private void updatesEffectsWithParentsAccordingly() {
		for (Entity particleEntity : particleEntities) {
			ParticleComponent particleComponent = ComponentsMapper.particle.get(particleEntity);
			ParticleEffect particleEffect = particleComponent.getParticleEffect();
			Entity parent = particleComponent.getParent();
			if (parent != null && ComponentsMapper.simpleDecal.has(parent)) {
				particleEffect.setTransform(auxMatrix.idt());
				particleEffect.translate(ComponentsMapper.simpleDecal.get(parent).getDecal().getPosition());
			}
		}
	}

	private void handleCompletedParticleEffects(final ParticleSystem particleSystem) {
		addCompleteToRemove();
		removeEffectsMarkedToBeRemoved(particleSystem);
		markToRemoveFollowedEffectsThatComplete();
		for (ParticleEffect effect : particleEffectsToRemove) {
			particleSystem.remove(effect);
		}
		particleEffectsToRemove.clear();
	}

	private void markToRemoveFollowedEffectsThatComplete() {
		particleEntitiesToRemove.clear();
		for (ParticleEffect effect : particleEffectsToFollow) {
			if (effect.isComplete()) {
				particleEffectsToRemove.add(effect);
			}
		}
	}

	private void removeEffectsMarkedToBeRemoved(ParticleSystem particleSystem) {
		for (Entity entity : particleEntitiesToRemove) {
			particleSystem.remove(ComponentsMapper.particle.get(entity).getParticleEffect());
			entity.remove(ParticleComponent.class);
			getEngine().removeEntity(entity);
		}
	}

	private void addCompleteToRemove() {
		for (Entity entity : particleEntities) {
			ParticleEffect particleEffect = ComponentsMapper.particle.get(entity).getParticleEffect();
			if (particleEffect.isComplete()) {
				particleEntitiesToRemove.add(entity);
			}
		}
	}

	@Override
	public void update(final float deltaTime) {
		updatesEffectsWithParentsAccordingly();
		ParticleSystem particleSystem = getSystemsCommonData().getParticleSystem();
		particleSystem.update(deltaTime);
		particleSystem.begin();
		particleSystem.draw();
		particleSystem.end();
		handleCompletedParticleEffects(particleSystem);
	}

	@Override
	public Class<SystemEventsSubscriber> getEventsSubscriberClass() {
		return null;
	}

	@Override
	public void initializeData() {
		ParticleSystem particleSystem = new ParticleSystem();
		getSystemsCommonData().setParticleSystem(particleSystem);
		particleSystem.add(pointSpriteBatch);
		pointSpriteBatch.setCamera(getSystemsCommonData().getCamera());
	}

	@Override
	public void dispose() {

	}

}
