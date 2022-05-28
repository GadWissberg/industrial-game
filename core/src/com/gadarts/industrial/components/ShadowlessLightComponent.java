package com.gadarts.industrial.components;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ShadowlessLightComponent extends LightComponent {


	private float duration;
	private long beginTime;

	private boolean flicker;
	@Setter
	private long nextFlicker;
	private float originalIntensity;
	private float originalRadius;
	private Entity parent;

	@Override
	public void reset( ) {

	}

	public void applyDuration(final float inSeconds) {
		if (inSeconds <= 0) return;
		this.duration = inSeconds;
		this.beginTime = TimeUtils.millis();
	}

	public void init(Vector3 position, float intensity, float radius, Entity parent, boolean flicker) {
		super.init(position, intensity, radius);
		this.originalIntensity = intensity;
		this.originalRadius = radius;
		this.flicker = flicker;
		this.parent = parent;
		duration = -1L;
	}


}
