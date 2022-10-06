package com.gadarts.industrial.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class LightComponent implements GameComponent {

	@Getter(AccessLevel.NONE)
	private final Color color = new Color(Color.WHITE);
	@Getter(AccessLevel.NONE)
	private final Vector3 position = new Vector3();
	@Setter
	private float intensity;
	@Setter
	private float radius;

	public Vector3 getPosition(Vector3 output) {
		return output.set(position);
	}

	public void setPosition(Vector3 position) {
		this.position.set(position);
	}

	public void applyColor(final Color color) {
		this.color.set(color);
	}

	public Color getColor(Color output) {
		return output.set(color);
	}

	void init(Vector3 position, float intensity, float radius) {
		init(position, intensity, radius, Color.WHITE);
	}

	void init(Vector3 position, float intensity, float radius, Color color) {
		this.position.set(position);
		this.intensity = intensity;
		this.radius = radius;
		this.color.set(color);
	}
}
