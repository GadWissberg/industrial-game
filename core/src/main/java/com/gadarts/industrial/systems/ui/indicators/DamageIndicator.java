package com.gadarts.industrial.systems.ui.indicators;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.systems.ui.GameStage;

public class DamageIndicator {
	private final GameStage stage;
	private final Image left;
	private final Image right;
	private final Image bottom;
	private final Image top;

	public DamageIndicator(GameStage stage, GameAssetManager assetsManager) {
		this.stage = stage;
		Texture damageIndicatorTexture = assetsManager.getTexture(Assets.UiTextures.DAMAGE_INDICATOR);
		int stageWidth = (int) stage.getWidth();
		float stageHeight = stage.getHeight();
		left = addDamageIndicator(damageIndicatorTexture, stageHeight, 0, 0, 0);
		right = addDamageIndicator(damageIndicatorTexture, stageHeight, stageWidth, stageHeight, 180);
		bottom = addDamageIndicator(damageIndicatorTexture, stageWidth, stageWidth, 0, 90);
		top = addDamageIndicator(damageIndicatorTexture, stageWidth, 0, stageHeight, -90);
	}

	private Image addDamageIndicator(Texture damageIndicatorTexture,
									 float length,
									 float x,
									 float y,
									 float rotation) {
		int indicatorTextureWidth = damageIndicatorTexture.getWidth();
		var damageIndicator = new Image(damageIndicatorTexture);
		damageIndicator.setSize(indicatorTextureWidth, length);
		damageIndicator.setPosition(x, y);
		damageIndicator.setRotation(rotation);
		stage.addActor(damageIndicator);
		damageIndicator.setVisible(false);
		return damageIndicator;
	}

	public void show( ) {
		showPart(left);
		showPart(right);
		showPart(top);
		showPart(bottom);
	}

	private void showPart(Image part) {
		part.setVisible(true);
		part.getColor().a = 0F;
		part.addAction(Actions.sequence(Actions.fadeIn(0.3F), Actions.fadeOut(0.3F), Actions.visible(false)));
	}
}
