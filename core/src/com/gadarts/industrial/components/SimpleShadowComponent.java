package com.gadarts.industrial.components;

import lombok.Getter;

@Getter
public class SimpleShadowComponent implements GameComponent {
	private float radius;

	@Override
	public void reset( ) {

	}

	public void init(float radius) {
		this.radius = radius;
	}
}
