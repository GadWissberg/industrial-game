package com.gadarts.industrial.components.sd;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import lombok.Getter;
import lombok.Setter;

import static com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA;
import static com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA;
import static com.badlogic.gdx.graphics.g3d.decals.DecalMaterial.NO_BLEND;

@Setter
@Getter
public class RelatedDecal extends Decal {
	private boolean visible;

	public static RelatedDecal newDecal(final TextureRegion textureRegion,
										final boolean hasTransparency) {
		RelatedDecal decal = new RelatedDecal();
		decal.setTextureRegion(textureRegion);
		decal.setBlending(hasTransparency ? GL_SRC_ALPHA : NO_BLEND,
				hasTransparency ? GL_ONE_MINUS_SRC_ALPHA : NO_BLEND);
		decal.setDimensions(textureRegion.getRegionWidth(), textureRegion.getRegionHeight());
		decal.setColor(1, 1, 1, 1);
		decal.setVisible(true);
		return decal;
	}
}
