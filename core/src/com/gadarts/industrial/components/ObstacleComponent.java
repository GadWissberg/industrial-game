package com.gadarts.industrial.components;

import com.badlogic.gdx.math.Vector2;
import com.gadarts.industrial.shared.model.env.EnvironmentDefinitions;
import lombok.Getter;

@Getter
public class ObstacleComponent implements GameComponent {
	private int topLeftX;
	private int topLeftY;
	private int bottomRightX;
	private int bottomRightY;
	private EnvironmentDefinitions type;

	@Override
	public void reset() {

	}

	public void init(final Vector2 topLeft, final Vector2 bottomRight, final EnvironmentDefinitions type) {
		this.topLeftX = (int) topLeft.x;
		this.topLeftY = (int) topLeft.y;
		this.bottomRightX = (int) bottomRight.x;
		this.bottomRightY = (int) bottomRight.y;
		this.type = type;
	}
}
