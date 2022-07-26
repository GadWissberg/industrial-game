package com.gadarts.industrial.components;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import lombok.Getter;

@Getter
public class ParticleEffectComponent implements GameComponent {
	private ParticleEffect particleEffect;
	private Entity parent;

	@Override
	public void reset() {

	}

	public void init(final ParticleEffect originalEffect, final Entity parent) {
		this.particleEffect = originalEffect;
		this.parent = parent;
	}
}
