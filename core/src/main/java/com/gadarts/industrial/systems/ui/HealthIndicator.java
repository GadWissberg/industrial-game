package com.gadarts.industrial.systems.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

public class HealthIndicator extends HudIndicator {

	public static final Color FONT_COLOR_HEALTHY = Color.valueOf("#48b416");
	public static final Color FONT_COLOR_DAMAGED = Color.valueOf("#bdc724");
	public static final Color FONT_COLOR_DEAD = Color.valueOf("#d23333");
	private static final Array<Color> colorScale = Array.with(FONT_COLOR_DEAD, FONT_COLOR_DAMAGED, FONT_COLOR_HEALTHY);
	public static final float LABEL_PADDING_RIGHT = 25F;
	private final HudHeart heart;
	private final Label label;

	public HealthIndicator(Texture borderTexture, BitmapFont font, int hp, Texture heartTexture) {
		super(borderTexture);
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
		Color color = label.getStyle().fontColor;
		var selectedColor = colorScale.get(MathUtils.clamp(hp / 33, 0, colorScale.size - 1));
		if (selectedColor.r != color.r || selectedColor.g != color.g || selectedColor.b != color.b) {
			heart.setColor(selectedColor);
		}
		color.set(selectedColor.r, selectedColor.g, selectedColor.b, 1);
		heart.updateAnimation(hp, originalValue);
	}

}
