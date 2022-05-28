package com.gadarts.industrial.components.mi;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.collision.BoundingBox;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * More data needed mainly for shader.
 */
@Getter
public class AdditionalRenderData {
	private final List<Entity> nearbyLights = new ArrayList<>();
	private final boolean affectedByLight;

	@Getter(AccessLevel.NONE)
	private final BoundingBox boundingBox = new BoundingBox();
	@Setter
	private Color colorWhenOutside;

	public AdditionalRenderData(final boolean affectedByLight,
								final BoundingBox boundingBox,
								final Color colorWhenOutside) {
		this.affectedByLight = affectedByLight;
		if (boundingBox != null) {
			this.boundingBox.set(boundingBox);
		}
		this.colorWhenOutside = colorWhenOutside;
	}

	public void setBoundingBox(final BoundingBox boundingBox) {
		this.boundingBox.set(boundingBox);
	}

	public BoundingBox getBoundingBox(final BoundingBox output) {
		return output.set(boundingBox);
	}
}

