package com.gadarts.industrial.components.character;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OnGoingAttack {
	private CharacterComponent.AttackType type;

	@Getter
	private int bulletsToShoot;

	public void initialize(CharacterComponent.AttackType type, int bulletsToShoot) {
		this.type = type;
		this.bulletsToShoot = bulletsToShoot;
	}

	public void bulletShot( ) {
		bulletsToShoot--;
	}

	public boolean isDone( ) {
		return bulletsToShoot <= 0;
	}
}
