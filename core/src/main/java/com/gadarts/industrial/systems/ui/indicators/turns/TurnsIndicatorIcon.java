package com.gadarts.industrial.systems.ui.indicators.turns;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.systems.ui.NoiseEffectHandler;
import lombok.Getter;

import static com.badlogic.gdx.math.Interpolation.*;

public class TurnsIndicatorIcon extends Table {
	public static final float FADING_DURATION = 1F;
	public static final float RELATIVE_POSITION_X = -10F;
	public static final float RELATIVE_POSITION_Y = -10F;
	private static final Vector2 auxVector = new Vector2();
	private static final float DAMAGE_EFFECT_SCALE_TO = 1.2F;
	private static final float DAMAGE_EFFECT_DURATION = 0.1F;
	private final Image icon;
	private final Image border;
	private final ActionPointsIndicator actionPointsIndicator;
	private final NoiseEffectHandler noiseEffectHandler;

	@Getter
	private final Entity character;

	public TurnsIndicatorIcon(TurnsIndicatorIconTextures textures,
							  BitmapFont font,
							  int actionPoints,
							  NoiseEffectHandler noiseEffectHandler,
							  Entity character) {
		super();
		this.noiseEffectHandler = noiseEffectHandler;
		Texture circleTexture = textures.circleTexture();
		setBackground(new TextureRegionDrawable(circleTexture));
		setSize(circleTexture.getWidth(), circleTexture.getHeight());
		icon = new Image();
		icon.setScaling(Scaling.none);
		border = new Image(textures.borderTexture());
		Stack stack = new Stack(icon, border);
		add(stack);
		border.getColor().a = 0F;
		actionPointsIndicator = new ActionPointsIndicator(textures.actionsPointsTexture(), font, actionPoints, noiseEffectHandler);
		Vector2 position = localToScreenCoordinates(auxVector.setZero().add(RELATIVE_POSITION_X, RELATIVE_POSITION_Y));
		actionPointsIndicator.setPosition(position.x, position.y);
		actionPointsIndicator.getColor().a = 0;
		addActor(actionPointsIndicator);
		setOrigin(Align.center);
		setTransform(true);
		this.character = character;
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
		AlphaAction action;
		if (borderVisibility) {
			action = Actions.fadeIn(FADING_DURATION, smooth2);
		} else {
			action = Actions.fadeOut(FADING_DURATION, smooth2);
		}
		border.addAction(action);
		actionPointsIndicator.applyVisibility(borderVisibility);
	}

	public void updateActionPointsIndicator(int newValue) {
		actionPointsIndicator.updateValue(newValue);
	}

	public void applyDamageEffect( ) {
		addAction(Actions.sequence(
				Actions.scaleTo(DAMAGE_EFFECT_SCALE_TO, DAMAGE_EFFECT_SCALE_TO, DAMAGE_EFFECT_DURATION, slowFast),
				Actions.scaleTo(1F, 1F, DAMAGE_EFFECT_DURATION, sine)));
	}

	@Override
	public String toString( ) {
		return ComponentsMapper.player.has(character) ? "Player" : "Enemy";
	}
}
