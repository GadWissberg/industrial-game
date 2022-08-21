package com.gadarts.industrial.systems.enemy;

import com.badlogic.ashley.core.Entity;

import java.util.function.Predicate;

public record PrimaryAttackValidation(Predicate<Entity> validation,
									  PrimaryAttackValidationAlternative alternative) {
	public boolean validate(Entity enemy) {
		return validation.test(enemy);
	}
}
