package com.gadarts.industrial.systems.ui.indicators;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.gadarts.industrial.components.player.Ammo;

public class AmmoIndicator extends HudIndicator {
	private static final String FORMAT = "%s/%s";
	private final Label label;
	private Ammo ammo;

	public AmmoIndicator(Texture borderTexture, BitmapFont font) {
		super(borderTexture);
		Label.LabelStyle style = new Label.LabelStyle(font, FONT_COLOR_GOOD);
		this.label = new Label(String.format(FORMAT, ammo.loaded(), ammo.total()), style);
		add(label);
	}

	public void setValues(Ammo ammo) {
		this.ammo = ammo;
	}
}
