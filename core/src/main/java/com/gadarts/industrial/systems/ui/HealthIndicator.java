package com.gadarts.industrial.systems.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class HealthIndicator extends Table {

	public static final Color FONT_COLOR = Color.valueOf("#5cb532");
	public static final float PADDING_RIGHT = 25F;

	public HealthIndicator(Texture texture, BitmapFont font) {
		setBackground(new TextureRegionDrawable(texture));
		setSize(texture.getWidth(), texture.getHeight());
		Label label = new Label("100", new Label.LabelStyle(font, FONT_COLOR));
		add(label).expandX().pad(0F, 0F, 0F, PADDING_RIGHT);
	}
}
