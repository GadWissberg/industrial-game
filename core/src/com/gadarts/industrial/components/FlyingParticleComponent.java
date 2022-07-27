package com.gadarts.industrial.components;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public class FlyingParticleComponent implements GameComponent {
	public static final float MINIMUM_DEGREE = 45F;
	public static final float MAX_DEGREE_TO_ADD = 90F;
	public static final float FLY_AWAY_COEFF = 0.1F;
	@Getter(AccessLevel.NONE)
	private final Vector3 flyAwayVelocity = new Vector3();
	private float nodeHeight;
	@Setter
	private long destroyTime;

	public Vector3 getFlyAwayVelocity(Vector3 output) {
		return output.set(flyAwayVelocity);
	}

	public Vector3 setFlyAwayVelocity(Vector3 velocity) {
		return flyAwayVelocity.set(velocity);
	}

	@Override
	public void reset( ) {

	}

	public void init(float nodeHeight) {
		this.nodeHeight = nodeHeight;
		this.destroyTime = -1;
		flyAwayVelocity.set(1F, 0F, 0F)
				.rotate(Vector3.Z, MINIMUM_DEGREE + MathUtils.random(MAX_DEGREE_TO_ADD))
				.rotate(Vector3.Y, MINIMUM_DEGREE + MathUtils.random(MAX_DEGREE_TO_ADD))
				.nor().scl(FLY_AWAY_COEFF);
	}
}
