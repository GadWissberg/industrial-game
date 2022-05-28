package com.gadarts.industrial.components;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class BulletComponent implements GameComponent {

	@Getter(AccessLevel.NONE)
	private final Vector2 initialPosition = new Vector2();

	@Getter(AccessLevel.NONE)
	private final Vector3 direction = new Vector3();

	private Entity owner;
	private Integer damage;

	public Vector3 getDirection(final Vector3 output) {
		return output.set(direction);
	}

	public Vector2 getInitialPosition(final Vector2 output) {
		return output.set(initialPosition);
	}

	@Override
	public void reset( ) {

	}

	public void init(final Vector2 initialPosition,
					 final Vector3 direction,
					 final Entity owner,
					 final Integer damagePoints) {
		this.initialPosition.set(initialPosition);
		this.direction.set(direction);
		this.owner = owner;
		this.damage = damagePoints;
	}
}
