package com.gadarts.industrial.systems.ui.indicators.health;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.gadarts.industrial.systems.ui.NoiseEffectHandler;
import com.gadarts.industrial.systems.ui.indicators.HudIndicator;

public class HealthIndicator extends HudIndicator {

	private static final Array<Color> colorScale = Array.with(FONT_COLOR_BAD, FONT_COLOR_OK, FONT_COLOR_GOOD);
	public static final float LABEL_PADDING_RIGHT = 25F;
	private final HudHeart heart;
	private final Label label;

	public HealthIndicator(ButtonStyle style,
						   BitmapFont font,
						   int hp,
						   Texture heartTexture, NoiseEffectHandler noiseEffectHandler) {
		super(style, noiseEffectHandler);
		String hpString = Integer.toString(hp);
		GlyphLayout layout = new GlyphLayout();
		layout.setText(font, hpString);
		label = new Label(hpString, new Label.LabelStyle(font, new Color(FONT_COLOR_GOOD)));
		label.setAlignment(Align.center);
		add(label).size(layout.width, layout.height).expandX().pad(0F, 0F, 0F, LABEL_PADDING_RIGHT);
		heart = new HudHeart(heartTexture, getPrefWidth(), getPrefHeight(), FONT_COLOR_GOOD);
		addActor(heart);
	}

	public void setValue(int hp, int originalValue) {
		label.setText(hp);
		Color color = label.getStyle().fontColor;
		var selectedColor = colorScale.get(MathUtils.clamp(hp / 33, 0, colorScale.size - 1));
		if (selectedColor.r != color.r || selectedColor.g != color.g || selectedColor.b != color.b) {
			heart.setColor(selectedColor);
		}
		color.set(selectedColor.r, selectedColor.g, selectedColor.b, 1);
		heart.updateAnimation(hp, originalValue);
	}

}
