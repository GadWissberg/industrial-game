package com.gadarts.industrial.systems.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class HealthIndicator extends Table {

	public static final Color FONT_COLOR_HEALTHY = Color.valueOf("#5fd02a");
	public static final Color FONT_COLOR_DEAD = Color.valueOf("#e51f1f");
	public static final float PADDING_RIGHT = 25F;
	private final Label label;

	public HealthIndicator(Texture texture, BitmapFont font, int hp) {
		setBackground(new TextureRegionDrawable(texture));
		setSize(texture.getWidth(), texture.getHeight());
		label = new Label(Integer.toString(hp), new Label.LabelStyle(font, FONT_COLOR_HEALTHY));
		add(label).expandX().pad(0F, 0F, 0F, PADDING_RIGHT);
	}

	public void setValue(int hp) {
		label.setText(hp);
		float mappedValue = MathUtils.clamp((100f - hp) / 75f, 0F, 1f);
		float r = Interpolation.linear.apply(FONT_COLOR_HEALTHY.r, FONT_COLOR_DEAD.r, mappedValue);
		float g = Interpolation.linear.apply(FONT_COLOR_HEALTHY.g, FONT_COLOR_DEAD.g, mappedValue);
		float b = Interpolation.linear.apply(FONT_COLOR_HEALTHY.b, FONT_COLOR_DEAD.b, mappedValue);
		label.getColor().set(r, g, b, 1F);
	}
}
