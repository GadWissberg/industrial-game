package com.gadarts.industrial.systems.ui.indicators;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.gadarts.industrial.components.player.Ammo;

public class AmmoIndicator extends HudIndicator {
	private static final String FORMAT = "%s/%s";
	public static final float LOW_VALUE_THRESHOLD = 0.2F;
	private final Label label;

	public AmmoIndicator(Texture borderTexture, BitmapFont font) {
		super(borderTexture);
		Label.LabelStyle style = new Label.LabelStyle(font, new Color(FONT_COLOR_GOOD));
		this.label = new Label("-", style);
		add(label);
	}

	public void setValues(Ammo ammo) {
		this.label.setText(String.format(FORMAT, ammo.getLoaded(), ammo.getTotal()));
		float badThreshold = ammo.getPlayerWeaponDeclaration().magazineSize() * LOW_VALUE_THRESHOLD;
		label.getStyle().fontColor.set(ammo.getLoaded() <= badThreshold ? FONT_COLOR_BAD : FONT_COLOR_GOOD);
	}
}
