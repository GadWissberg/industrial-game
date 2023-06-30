package com.gadarts.industrial.systems.ui.indicators;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

public class AmmoIndicator extends HudIndicator {
	private static final String FORMAT = "%s/%s";
	private final Label label;
	private int loaded;
	private int total;

	public AmmoIndicator(Texture borderTexture, BitmapFont font) {
		super(borderTexture);
		Label.LabelStyle style = new Label.LabelStyle(font, FONT_COLOR_GOOD);
		this.label = new Label(String.format(FORMAT, loaded, total), style);
		add(label);
	}

	public void setValues(int loaded, int total) {
		this.loaded = loaded;
		this.total = total;
	}
}
