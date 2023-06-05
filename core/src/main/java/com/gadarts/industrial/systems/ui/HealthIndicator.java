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
	private final Label label;

	public HealthIndicator(Texture texture, BitmapFont font, int hp) {
		setBackground(new TextureRegionDrawable(texture));
		setSize(texture.getWidth(), texture.getHeight());
		label = new Label(Integer.toString(hp), new Label.LabelStyle(font, FONT_COLOR));
		add(label).expandX().pad(0F, 0F, 0F, PADDING_RIGHT);
	}

	public void setValue(int hp) {
		label.setText(hp);
	}
}
