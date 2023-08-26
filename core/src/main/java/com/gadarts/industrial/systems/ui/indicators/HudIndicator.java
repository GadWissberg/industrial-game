package com.gadarts.industrial.systems.ui.indicators;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.gadarts.industrial.systems.ui.NoiseEffectHandler;

public abstract class HudIndicator extends Button {
	public static final Color FONT_COLOR_GOOD = Color.valueOf("#48b416");
	public static final Color FONT_COLOR_OK = Color.valueOf("#bdc724");
	public static final Color FONT_COLOR_BAD = Color.valueOf("#d23333");
	private final NoiseEffectHandler noiseEffectHandler;

	public HudIndicator(ButtonStyle style, NoiseEffectHandler noiseEffectHandler) {
		super(style);
		this.noiseEffectHandler = noiseEffectHandler;
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		noiseEffectHandler.begin(batch);
		super.draw(batch, parentAlpha);
		noiseEffectHandler.end(batch);
	}

}
