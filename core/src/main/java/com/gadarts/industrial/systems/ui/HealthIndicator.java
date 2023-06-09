package com.gadarts.industrial.systems.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

public class HealthIndicator extends Table {

	public static final Color FONT_COLOR_HEALTHY = Color.valueOf("#48b416");
	public static final Color FONT_COLOR_DEAD = Color.valueOf("#c81313");
	public static final float LABEL_PADDING_RIGHT = 25F;
	private final HudHeart heart;
	private final Label label;

	public HealthIndicator(Texture borderTexture, BitmapFont font, int hp, Texture heartTexture) {
		setBackground(new TextureRegionDrawable(borderTexture));
		setSize(borderTexture.getWidth(), borderTexture.getHeight());
		String hpString = Integer.toString(hp);
		GlyphLayout layout = new GlyphLayout();
		layout.setText(font, hpString);
		label = new Label(hpString, new Label.LabelStyle(font, FONT_COLOR_HEALTHY));
		label.setAlignment(Align.center);
		add(label).size(layout.width, layout.height).expandX().pad(0F, 0F, 0F, LABEL_PADDING_RIGHT);
		heart = new HudHeart(heartTexture, borderTexture, FONT_COLOR_HEALTHY);
		addActor(heart);
	}


	public void setValue(int hp, int originalValue) {
		label.setText(hp);
		float mappedValue = MathUtils.clamp((100f - hp) / 75f, 0F, 1f);
		float r = Interpolation.linear.apply(FONT_COLOR_HEALTHY.r, FONT_COLOR_DEAD.r, mappedValue);
		float g = Interpolation.linear.apply(FONT_COLOR_HEALTHY.g, FONT_COLOR_DEAD.g, mappedValue);
		float b = Interpolation.linear.apply(FONT_COLOR_HEALTHY.b, FONT_COLOR_DEAD.b, mappedValue);
		Color color = label.getColor();
		color.set(r, g, b, 1F);
		heart.updateAnimation(hp, originalValue, color);
	}

}
