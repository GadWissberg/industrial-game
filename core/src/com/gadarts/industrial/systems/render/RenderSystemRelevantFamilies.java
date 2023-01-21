package com.gadarts.industrial.systems.render;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.components.PickUpComponent;
import com.gadarts.industrial.components.sll.ShadowlessLightComponent;
import com.gadarts.industrial.components.SimpleShadowComponent;
import com.gadarts.industrial.components.StaticLightComponent;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
import com.gadarts.industrial.components.sd.SimpleDecalComponent;
import lombok.Getter;

@Getter
public class RenderSystemRelevantFamilies {
	private ImmutableArray<Entity> shadowlessLightsEntities;
	private ImmutableArray<Entity> characterDecalsEntities;
	private ImmutableArray<Entity> simpleDecalsEntities;
	private ImmutableArray<Entity> staticLightsEntities;
	private ImmutableArray<Entity> modelEntitiesWithShadows;
	private ImmutableArray<Entity> modelEntities;
	private ImmutableArray<Entity> simpleShadowEntities;

	public void init(Engine engine) {
		characterDecalsEntities = engine.getEntitiesFor(Family.all(CharacterDecalComponent.class).get());
		simpleDecalsEntities = engine.getEntitiesFor(Family.all(SimpleDecalComponent.class).get());
		shadowlessLightsEntities = engine.getEntitiesFor(Family.all(ShadowlessLightComponent.class).get());
		staticLightsEntities = engine.getEntitiesFor(Family.all(StaticLightComponent.class).get());
		modelEntitiesWithShadows = engine.getEntitiesFor(Family.all(ModelInstanceComponent.class)
				.exclude(PickUpComponent.class, StaticLightComponent.class, DoorComponent.class)
				.get());
		modelEntities = engine.getEntitiesFor(Family.all(ModelInstanceComponent.class).get());
		simpleShadowEntities = engine.getEntitiesFor(Family.one(SimpleShadowComponent.class).get());
	}
}
