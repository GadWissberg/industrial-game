package com.gadarts.industrial.systems.enemy;

import com.badlogic.ashley.core.Entity;

public record PrimaryAttackValidation(PrimaryAttackPredicate predicate,
									  PrimaryAttackValidationAlternative alternative) {
	public boolean validate(Entity enemy, EnemySystem enemySystem) {
		return predicate.test(enemy, enemySystem);
	}
}
