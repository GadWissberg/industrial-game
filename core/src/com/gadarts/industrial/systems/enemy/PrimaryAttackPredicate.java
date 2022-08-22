package com.gadarts.industrial.systems.enemy;

import com.badlogic.ashley.core.Entity;

public interface PrimaryAttackPredicate {
	boolean test(Entity enemy, EnemySystem enemySystem);
}
