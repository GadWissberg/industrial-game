package com.gadarts.industrial.systems.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.RepeatAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;

public class HudHeart extends Image {
	public static final float HEART_ANIMATION_SIZE_BY = 5F;
	public static final float HEART_ANIMATION_INTERVAL_3 = 0.3F;
	public static final float HEART_ANIMATION_INTERVAL_2 = 0.15F;
	public static final float HEART_ANIMATION_INTERVAL_1 = 0.05F;
	public static final float HEART_ANIMATION_INTERVAL_0 = 0.6F;
	public static final int MAX_HEALTH = 100;
	public static final float PADDING_RIGHT = 62F;
	private final Array<RepeatAction> heartAnimationsArray;
	private int updateHeartAnimation = -1;

	public HudHeart(Texture heartTexture, Texture borderTexture, Color color) {
		super(heartTexture);
		setColor(color);
		Vector2 heartOriginalPosition = new Vector2(
				borderTexture.getWidth() - PADDING_RIGHT,
				borderTexture.getHeight() / 2F - heartTexture.getHeight() / 2F);
		RepeatAction heartAnimation_0 = createHeartAnimation(HEART_ANIMATION_INTERVAL_0, heartOriginalPosition, heartTexture);
		RepeatAction heartAnimation_1 = createHeartAnimation(HEART_ANIMATION_INTERVAL_1, heartOriginalPosition, heartTexture);
		RepeatAction heartAnimation_2 = createHeartAnimation(HEART_ANIMATION_INTERVAL_2, heartOriginalPosition, heartTexture);
		RepeatAction heartAnimation_3 = createHeartAnimation(HEART_ANIMATION_INTERVAL_3, heartOriginalPosition, heartTexture);
		heartAnimationsArray = Array.with(
				heartAnimation_0,
				heartAnimation_1,
				heartAnimation_2,
				heartAnimation_3);
		addAction(heartAnimation_3);
	}

	private static SequenceAction sizingSequence(float interval) {
		return Actions.sequence(
				Actions.delay(interval),
				Actions.sizeBy(-HEART_ANIMATION_SIZE_BY, -HEART_ANIMATION_SIZE_BY),
				Actions.delay(interval),
				Actions.sizeBy(-HEART_ANIMATION_SIZE_BY, -HEART_ANIMATION_SIZE_BY),
				Actions.delay(interval),
				Actions.sizeBy(HEART_ANIMATION_SIZE_BY, HEART_ANIMATION_SIZE_BY),
				Actions.delay(interval),
				Actions.sizeBy(HEART_ANIMATION_SIZE_BY, HEART_ANIMATION_SIZE_BY)
		);
	}

	private RepeatAction createHeartAnimation(float interval, Vector2 heartOriginalPosition, Texture heartTexture) {
		return Actions.forever(
				Actions.sequence(
						Actions.moveTo(heartOriginalPosition.x, heartOriginalPosition.y),
						Actions.sizeTo(heartTexture.getWidth(), heartTexture.getHeight()),
						Actions.parallel(sizingSequence(interval), centeringSequence(interval))));
	}

	private SequenceAction centeringSequence(float interval) {
		return Actions.sequence(
				Actions.delay(interval),
				Actions.moveBy(HEART_ANIMATION_SIZE_BY / 2F, HEART_ANIMATION_SIZE_BY / 2F),
				Actions.delay(interval),
				Actions.moveBy(HEART_ANIMATION_SIZE_BY / 2F, HEART_ANIMATION_SIZE_BY / 2F),
				Actions.delay(interval),
				Actions.moveBy(-HEART_ANIMATION_SIZE_BY / 2F, -HEART_ANIMATION_SIZE_BY / 2F),
				Actions.delay(interval),
				Actions.moveBy(-HEART_ANIMATION_SIZE_BY / 2F, -HEART_ANIMATION_SIZE_BY / 2F)
		);
	}

	@Override
	public void act(float delta) {
		super.act(delta);
		if (updateHeartAnimation >= 0) {
			clearActions();
			int i = updateHeartAnimation / healthDivisions();
			addAction(heartAnimationsArray.get(Math.min(i, heartAnimationsArray.size - 1)));
			updateHeartAnimation = -1;
		}
	}

	private int healthDivisions( ) {
		return MAX_HEALTH / heartAnimationsArray.size + 1;
	}

	public void updateAnimation(int hp, int originalValue) {
		if (originalValue / healthDivisions() != hp / healthDivisions()) {
			updateHeartAnimation = hp;
		}
	}

}
