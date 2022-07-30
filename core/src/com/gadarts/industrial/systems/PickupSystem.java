package com.gadarts.industrial.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.PickUpComponent;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.systems.character.commands.CharacterCommand;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.systems.input.InputSystemEventsSubscriber;
import com.gadarts.industrial.systems.player.PlayerSystemEventsSubscriber;

public class PickupSystem extends GameSystem<SystemEventsSubscriber> implements InputSystemEventsSubscriber, PlayerSystemEventsSubscriber, CharacterSystemEventsSubscriber {
	private static final float PICK_UP_ROTATION = 10;
	private final static Vector3 auxVector3_1 = new Vector3();
	private final static Vector3 auxVector3_2 = new Vector3();
	private final float[] hsvArray = new float[3];
	private ImmutableArray<Entity> pickupEntities;

	public PickupSystem(SystemsCommonData systemsCommonData,
						GameAssetsManager assetsManager,
						GameLifeCycleHandler lifeCycleHandler) {
		super(systemsCommonData, assetsManager, lifeCycleHandler);
	}

	@Override
	public void onItemPickedUp(final Entity itemPickedUp) {
		PooledEngine engine = (PooledEngine) getEngine();
		engine.removeEntity(itemPickedUp);
		getSystemsCommonData().setItemToPickup(null);
	}

	@Override
	public void onPlayerPathCreated(MapGraphNode destination) {
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		Entity currentHighLightedPickup = systemsCommonData.getCurrentHighLightedPickup();
		if (currentHighLightedPickup != null) {
			systemsCommonData.setItemToPickup(currentHighLightedPickup);
		}
	}

	@Override
	public void onPlayerAppliedCommand(CharacterCommand command) {

	}

	@Override
	public Class<SystemEventsSubscriber> getEventsSubscriberClass( ) {
		return null;
	}

	@Override
	public void addedToEngine(Engine engine) {
		super.addedToEngine(engine);
		pickupEntities = engine.getEntitiesFor(Family.all(PickUpComponent.class).get());
	}

	@Override
	public void update(final float deltaTime) {
		super.update(deltaTime);
		for (Entity pickup : pickupEntities) {
			rotatePickup(deltaTime, pickup);
			PickUpComponent pickUpComponent = ComponentsMapper.pickup.get(pickup);
			flickerPickup(pickup, pickUpComponent);
		}
	}

	private void flickerPickup(final Entity pickup, final PickUpComponent pickUpComponent) {
		float flickerValue = pickUpComponent.getFlicker();
		Color color = ComponentsMapper.modelInstance.get(pickup).getColorAttribute().color;
		color.toHsv(hsvArray);
		float value = hsvArray[2];
		if (flickerValue > 0) {
			fadeOut(pickup, value < 1, Math.min(value + flickerValue, 1), value >= 1);
		} else {
			fadeOut(pickup, value > 0, Math.max(value + flickerValue, 0), value <= 0);
		}
	}

	@Override
	public void mouseMoved(final int screenX, final int screenY) {
		boolean foundPickup = highlightPickupUnderMouse(screenX, screenY);
		SystemsCommonData commonData = getSystemsCommonData();
		commonData.setCurrentHighLightedPickup(foundPickup ? commonData.getCurrentHighLightedPickup() : null);
	}

	private boolean highlightPickupUnderMouse(final int screenX, final int screenY) {
		Ray ray = getSystemsCommonData().getCamera().getPickRay(screenX, screenY);
		boolean result = false;
		for (Entity pickup : pickupEntities) {
			result = handlePickupHighlight(ray, pickup);
			if (result) {
				getSystemsCommonData().setCurrentHighLightedPickup(pickup);
				break;
			}
		}
		return result;
	}

	private boolean handlePickupHighlight(final Ray ray, final Entity pickup) {
		ModelInstance mic = ComponentsMapper.modelInstance.get(pickup).getModelInstance();
		Vector3 cen = mic.transform.getTranslation(auxVector3_1);
		ColorAttribute attr = (ColorAttribute) mic.materials.get(0).get(ColorAttribute.Emissive);
		boolean rayCheck = Intersector.intersectRayBoundsFast(ray, cen, auxVector3_2.set(0.5f, 0.5f, 0.5f));
		int v = rayCheck ? 1 : 0;
		attr.color.set(v, v, v, v);
		return rayCheck;
	}

	private void fadeOut(final Entity pickup,
						 final boolean insideRange,
						 final float newValue,
						 final boolean reachedBound) {
		Color color = ComponentsMapper.modelInstance.get(pickup).getColorAttribute().color;
		if (insideRange) {
			hsvArray[2] = newValue;
			color.fromHsv(hsvArray);
		} else if (reachedBound) {
			PickUpComponent pickUpComponent = ComponentsMapper.pickup.get(pickup);
			pickUpComponent.setFlicker(-pickUpComponent.getFlicker());
		}
	}

	private void rotatePickup(final float deltaTime, final Entity pickup) {
		ModelInstanceComponent modelInstanceComponent = ComponentsMapper.modelInstance.get(pickup);
		ModelInstance modelInstance = modelInstanceComponent.getModelInstance();
		modelInstance.transform.rotate(Vector3.Y, deltaTime * PICK_UP_ROTATION);
	}

	@Override
	public void initializeData( ) {

	}

	@Override
	public void dispose( ) {

	}

}
