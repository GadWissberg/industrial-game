package com.gadarts.industrial.components.sll;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import com.gadarts.industrial.components.LightComponent;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ShadowlessLightComponent extends LightComponent {


	private final ShadowlessLightOriginalData shadowlessLightOriginalData = new ShadowlessLightOriginalData();
	private float duration;
	private long beginTime;
	private boolean flicker;
	@Setter
	private long nextFlicker;
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
		this.shadowlessLightOriginalData.set(intensity, radius);
		this.flicker = flicker;
		this.parent = parent;
		duration = -1L;
	}


}
