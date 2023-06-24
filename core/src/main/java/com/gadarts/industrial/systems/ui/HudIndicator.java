package com.gadarts.industrial.systems.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public abstract class HudIndicator extends Table {
	public HudIndicator(Texture borderTexture) {
		setBackground(new TextureRegionDrawable(borderTexture));
		setSize(borderTexture.getWidth(), borderTexture.getHeight());
	}
}
