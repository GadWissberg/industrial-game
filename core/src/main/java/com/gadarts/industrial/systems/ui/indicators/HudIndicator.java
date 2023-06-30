package com.gadarts.industrial.systems.ui.indicators;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public abstract class HudIndicator extends Table {
	public static final Color FONT_COLOR_GOOD = Color.valueOf("#48b416");
	public static final Color FONT_COLOR_OK = Color.valueOf("#bdc724");
	public static final Color FONT_COLOR_BAD = Color.valueOf("#d23333");

	public HudIndicator(Texture borderTexture) {
		setBackground(new TextureRegionDrawable(borderTexture));
		setSize(borderTexture.getWidth(), borderTexture.getHeight());
	}
}
