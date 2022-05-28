package com.gadarts.industrial.components;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import lombok.Getter;

@Getter
public class ParticleEffectParentComponent implements GameComponent {
	private final Array<Entity> children = new Array<>();

	@Override
	public void reset() {

	}

	public void init(final Entity child) {
		children.clear();
		children.add(child);
	}
}
