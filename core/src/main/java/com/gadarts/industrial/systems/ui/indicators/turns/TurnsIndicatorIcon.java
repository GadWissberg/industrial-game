package com.gadarts.industrial.systems.ui.indicators.turns;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.gadarts.industrial.systems.ui.NoiseEffectHandler;

import static com.badlogic.gdx.math.Interpolation.smooth2;

public class TurnsIndicatorIcon extends Table {
	public static final float FADING_DURATION = 1F;
	public static final float RELATIVE_POSITION_X = -10F;
	public static final float RELATIVE_POSITION_Y = -10F;
	private static final Vector2 auxVector = new Vector2();
	private final Image icon;
	private final Image border;
	private final ActionPointsIndicator actionPointsIndicator;
	private final NoiseEffectHandler noiseEffectHandler;

	public TurnsIndicatorIcon(Texture texture,
							  Texture borderTexture,
							  Texture actionsPointsTexture,
							  BitmapFont font,
							  int actionPoints,
							  NoiseEffectHandler noiseEffectHandler) {
		super();
		this.noiseEffectHandler = noiseEffectHandler;
		setBackground(new TextureRegionDrawable(texture));
		setSize(texture.getWidth(), texture.getHeight());
		icon = new Image();
		icon.setScaling(Scaling.none);
		border = new Image(borderTexture);
		Stack stack = new Stack(icon, border);
		add(stack);
		border.getColor().a = 0F;
		actionPointsIndicator = new ActionPointsIndicator(actionsPointsTexture, font, actionPoints, noiseEffectHandler);
		Vector2 position = localToScreenCoordinates(auxVector.setZero().add(RELATIVE_POSITION_X, RELATIVE_POSITION_Y));
		actionPointsIndicator.setPosition(position.x, position.y);
		actionPointsIndicator.getColor().a = 0;
		addActor(actionPointsIndicator);
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		noiseEffectHandler.begin(batch);
		super.draw(batch, parentAlpha);
		noiseEffectHandler.end(batch);
	}

	public void applyIcon(TextureRegionDrawable icon) {
		this.icon.setDrawable(icon);
	}

	public void setBorderVisibility(boolean borderVisibility) {
		border.addAction(borderVisibility ? Actions.fadeIn(FADING_DURATION, smooth2) : Actions.fadeOut(FADING_DURATION, smooth2));
		actionPointsIndicator.applyVisibility(borderVisibility);
	}

	public void updateActionPointsIndicator(int newValue) {
		actionPointsIndicator.updateValue(newValue);
	}
}
