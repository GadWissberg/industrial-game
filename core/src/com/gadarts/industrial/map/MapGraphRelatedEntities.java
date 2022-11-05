package com.gadarts.industrial.map;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.gadarts.industrial.components.EnvironmentObjectComponent;
import com.gadarts.industrial.components.PickUpComponent;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import lombok.Getter;

@Getter
public class MapGraphRelatedEntities {
	private ImmutableArray<Entity> pickupEntities;
	private ImmutableArray<Entity> enemiesEntities;
	private ImmutableArray<Entity> characterEntities;
	private ImmutableArray<Entity> environmentObjectsEntities;

	public void init(PooledEngine engine) {
		this.characterEntities = engine.getEntitiesFor(Family.all(CharacterComponent.class).get());
		this.environmentObjectsEntities = engine.getEntitiesFor(Family.all(EnvironmentObjectComponent.class).get());
		this.enemiesEntities = engine.getEntitiesFor(Family.all(EnemyComponent.class).get());
		this.pickupEntities = engine.getEntitiesFor(Family.all(PickUpComponent.class).get());

	}
}
