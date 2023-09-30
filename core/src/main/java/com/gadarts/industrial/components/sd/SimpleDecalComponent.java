package com.gadarts.industrial.components.sd;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.utils.Pools;
import com.gadarts.industrial.components.GameComponent;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SimpleDecalComponent implements GameComponent {
	@Getter
	private final List<RelatedDecal> relatedDecals = new ArrayList<>();
	private Decal decal;
	private boolean billboard;
	private boolean animatedByAnimationComponent;
	@Setter
	private boolean visible;

	@Override
	public void reset( ) {

	}

	public void init(final Texture texture, final boolean visible, final boolean billboard) {
		init(new TextureRegion(texture), visible, billboard, animatedByAnimationComponent);
	}

	public void init(final TextureRegion textureRegion,
					 final boolean visible,
					 final boolean billboard,
					 final boolean animatedByAnimationComponent) {
		this.animatedByAnimationComponent = animatedByAnimationComponent;
		decal = Decal.newDecal(textureRegion, true);
		this.visible = visible;
		this.billboard = billboard;
		if (!relatedDecals.isEmpty()) {
			for (Decal decal : relatedDecals) {
				Pools.get(Decal.class).free(decal);
			}
			relatedDecals.clear();
		}
	}
}
