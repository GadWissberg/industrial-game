package com.gadarts.industrial.systems.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.RepeatAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

public class HealthIndicator extends Table {

	public static final Color FONT_COLOR_HEALTHY = Color.valueOf("#5fd02a");
	public static final Color FONT_COLOR_DEAD = Color.valueOf("#e51f1f");
	public static final float HEART_PADDING_RIGHT = 20F;
	public static final float HEART_ANIMATION_SIZE_BY = 5F;
	public static final float HEART_ANIMATION_INTERVAL_3 = 0.3F;
	public static final float HEART_ANIMATION_INTERVAL_2 = 0.2F;
	public static final float HEART_ANIMATION_INTERVAL_1 = 0.1F;
	public static final float HEART_ANIMATION_INTERVAL_0 = 0.5F;
	public static final float LABEL_PADDING_RIGHT = 10F;
	public static final float LABEL_PADDING_LEFT = 30F;
	public static final int MAX_HEALTH = 100;
	private final Label label;
	private final Array<RepeatAction> heartAnimationsArray;
	private final Image heart;
	private int updateHeartAnimation = -1;
	GlyphLayout layout = new GlyphLayout();

	public HealthIndicator(Texture texture, BitmapFont font, int hp, Texture heartTexture) {
		setBackground(new TextureRegionDrawable(texture));
		setSize(texture.getWidth(), texture.getHeight());
		String hpString = Integer.toString(hp);
		layout.setText(font, hpString);
		label = new Label(hpString, new Label.LabelStyle(font, FONT_COLOR_HEALTHY));
		label.setAlignment(Align.center);
		add(label).size(layout.width, layout.height).expandX().pad(0F, LABEL_PADDING_LEFT, 0F, LABEL_PADDING_RIGHT);
		heart = new Image(heartTexture);
		add(heart).pad(0F, 0F, 0F, HEART_PADDING_RIGHT);
		heart.setOrigin(Align.center);
		pack();
		Vector2 heartOriginalPosition = heart.localToStageCoordinates(new Vector2());
		RepeatAction heartAnimation_0 = createHeartAnimation(HEART_ANIMATION_INTERVAL_0, heartOriginalPosition);
		RepeatAction heartAnimation_1 = createHeartAnimation(HEART_ANIMATION_INTERVAL_1, heartOriginalPosition);
		RepeatAction heartAnimation_2 = createHeartAnimation(HEART_ANIMATION_INTERVAL_2, heartOriginalPosition);
		RepeatAction heartAnimation_3 = createHeartAnimation(HEART_ANIMATION_INTERVAL_3, heartOriginalPosition);
		heartAnimationsArray = Array.with(
				heartAnimation_0,
				heartAnimation_1,
				heartAnimation_2,
				heartAnimation_3);
		heart.addAction(heartAnimation_3);
	}

	private RepeatAction createHeartAnimation(float interval, Vector2 heartOriginalPosition) {
		return Actions.forever(
				Actions.sequence(
						Actions.moveTo(heartOriginalPosition.x, heartOriginalPosition.y),
						Actions.parallel(
								Actions.sequence(
										Actions.delay(interval),
										Actions.sizeBy(-HEART_ANIMATION_SIZE_BY, -HEART_ANIMATION_SIZE_BY),
										Actions.delay(interval),
										Actions.sizeBy(-HEART_ANIMATION_SIZE_BY, -HEART_ANIMATION_SIZE_BY),
										Actions.delay(interval),
										Actions.sizeBy(HEART_ANIMATION_SIZE_BY, HEART_ANIMATION_SIZE_BY),
										Actions.delay(interval),
										Actions.sizeBy(HEART_ANIMATION_SIZE_BY, HEART_ANIMATION_SIZE_BY)
								), Actions.sequence(
										Actions.delay(interval),
										Actions.moveBy(HEART_ANIMATION_SIZE_BY / 2F, HEART_ANIMATION_SIZE_BY / 2F),
										Actions.delay(interval),
										Actions.moveBy(HEART_ANIMATION_SIZE_BY / 2F, HEART_ANIMATION_SIZE_BY / 2F),
										Actions.delay(interval),
										Actions.moveBy(-HEART_ANIMATION_SIZE_BY / 2F, -HEART_ANIMATION_SIZE_BY / 2F),
										Actions.delay(interval),
										Actions.moveBy(-HEART_ANIMATION_SIZE_BY, -HEART_ANIMATION_SIZE_BY / 2F)
								))));
	}

	@Override
	public void act(float delta) {
		super.act(delta);
		if (updateHeartAnimation >= 0) {
			clearActions();
			int i = updateHeartAnimation / healthDivisions();
			heart.addAction(heartAnimationsArray.get(Math.min(i, heartAnimationsArray.size - 1)));
			updateHeartAnimation = -1;
		}
	}

	private int healthDivisions( ) {
		return MAX_HEALTH / heartAnimationsArray.size + 1;
	}

	public void setValue(int hp, int originalValue) {
		label.setText(hp);
		float mappedValue = MathUtils.clamp((100f - hp) / 75f, 0F, 1f);
		float r = Interpolation.linear.apply(FONT_COLOR_HEALTHY.r, FONT_COLOR_DEAD.r, mappedValue);
		float g = Interpolation.linear.apply(FONT_COLOR_HEALTHY.g, FONT_COLOR_DEAD.g, mappedValue);
		float b = Interpolation.linear.apply(FONT_COLOR_HEALTHY.b, FONT_COLOR_DEAD.b, mappedValue);
		label.getColor().set(r, g, b, 1F);
		if (originalValue / healthDivisions() != hp / healthDivisions()) {
			updateHeartAnimation = hp;
		}
	}

}
