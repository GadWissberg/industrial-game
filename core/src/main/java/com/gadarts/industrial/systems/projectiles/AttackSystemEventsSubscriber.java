package com.gadarts.industrial.systems.projectiles;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.declarations.weapons.WeaponDeclaration;
import com.gadarts.industrial.systems.SystemEventsSubscriber;

public interface AttackSystemEventsSubscriber extends SystemEventsSubscriber {
	default void onBulletCollisionWithWall(Entity bullet, MapGraphNode node) {

	}

	default void onProjectileCollisionWithAnotherEntity(Entity bullet, Entity collidable) {

	}

	default void onMeleeAttackAppliedOnTarget(Entity character, Entity target, WeaponDeclaration primaryAttack) {

	}

	default void onBulletSetDestroyed(Entity bullet) {

	}
}
