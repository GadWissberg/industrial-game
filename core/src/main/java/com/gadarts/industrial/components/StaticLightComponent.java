package com.gadarts.industrial.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.systems.render.GameFrameBufferCubeMap;
import lombok.Getter;
import lombok.Setter;

@Getter
public class StaticLightComponent extends LightComponent {

	@Setter
	private GameFrameBufferCubeMap shadowFrameBuffer;

	@Override
	public void init(Vector3 position, float intensity, float radius, Color color) {
		super.init(position, intensity, radius, color);
	}

	@Override
	public void reset( ) {

	}
}
