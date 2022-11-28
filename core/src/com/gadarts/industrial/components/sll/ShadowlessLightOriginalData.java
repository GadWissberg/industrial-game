package com.gadarts.industrial.components.sll;

import lombok.Getter;

@Getter
public class ShadowlessLightOriginalData {
	private float originalIntensity;
	private float originalRadius;

	public void set(float intensity, float radius) {
		this.originalIntensity = intensity;
		this.originalRadius = radius;
	}
}
