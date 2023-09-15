package com.gadarts.industrial.systems.ui.indicators.turns;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.gadarts.industrial.systems.ui.NoiseEffectHandler;

import static com.badlogic.gdx.math.Interpolation.smooth2;

public class ActionPointsIndicator extends Table {
	public static final float FADING_DURATION = 0.5F;
	private static final float SIZE_BY_EFFECT_SCALE_TO = 1.2F;
	private static final float SIZE_BY_EFFECT_DURATION = 0.05F;
	private final Label label;
	private final NoiseEffectHandler noiseEffectHandler;

	public ActionPointsIndicator(Texture actionsPointsTexture,
								 BitmapFont font,
								 int actionPoints,
								 NoiseEffectHandler noiseEffectHandler) {
		super();
		this.noiseEffectHandler = noiseEffectHandler;
		setBackground(new TextureRegionDrawable(actionsPointsTexture));
		setSize(actionsPointsTexture.getWidth(), actionsPointsTexture.getHeight());
		label = new Label(actionPoints + "", new Label.LabelStyle(font, Color.WHITE));
		add(label);
		setOrigin(Align.center);
		setTransform(true);
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		noiseEffectHandler.begin(batch);
		super.draw(batch, parentAlpha);
		noiseEffectHandler.end(batch);
	}

	public void applyVisibility(boolean visible) {
		addAction(visible ? Actions.fadeIn(FADING_DURATION, smooth2) : Actions.fadeOut(FADING_DURATION, smooth2));
		label.setVisible(visible);
	}

	public void updateValue(int newValue) {
		label.setText(Math.max(newValue, 0));
		addAction(Actions.sequence(
				Actions.scaleTo(
						SIZE_BY_EFFECT_SCALE_TO,
						SIZE_BY_EFFECT_SCALE_TO,
						SIZE_BY_EFFECT_DURATION,
						Interpolation.swing),
				Actions.scaleTo(
						1F,
						1F,
						SIZE_BY_EFFECT_DURATION,
						Interpolation.smoother)));
	}
}
