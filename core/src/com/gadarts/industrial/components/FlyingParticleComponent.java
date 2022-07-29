package com.gadarts.industrial.components;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public class FlyingParticleComponent implements GameComponent {
	@Getter(AccessLevel.NONE)
	private final Vector3 flyAwayForce = new Vector3();
	@Getter(AccessLevel.NONE)
	private final Vector3 gravityForce = new Vector3();
	private float nodeHeight;
	private float deceleration;
	@Setter
	private long destroyTime;

	public Vector3 getFlyAwayForce(Vector3 output) {
		return output.set(flyAwayForce);
	}

	public Vector3 getGravityForce(Vector3 output) {
		return output.set(gravityForce);
	}

	public Vector3 setFlyAwayForce(Vector3 velocity) {
		return flyAwayForce.set(velocity);
	}

	public Vector3 setGravityForce(Vector3 velocity) {
		return gravityForce.set(velocity);
	}

	@Override
	public void reset( ) {

	}

	public void init(float nodeHeight, float strength, float deceleration, float minimumDegree, float maxDegreeToAdd) {
		this.nodeHeight = nodeHeight;
		this.destroyTime = -1;
		gravityForce.set(Vector3.Y).scl(-0.01F);
		flyAwayForce.set(1F, 0F, 0F)
				.rotate(Vector3.Z, minimumDegree + MathUtils.random(maxDegreeToAdd))
				.rotate(Vector3.Y, minimumDegree + MathUtils.random(maxDegreeToAdd))
				.nor().scl(strength);
		this.deceleration = deceleration;
	}
}
