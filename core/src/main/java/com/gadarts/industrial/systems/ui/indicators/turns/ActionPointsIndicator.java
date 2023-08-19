package com.gadarts.industrial.systems.ui.indicators.turns;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import static com.badlogic.gdx.math.Interpolation.smooth2;

public class ActionPointsIndicator extends Table {
	public static final float FADING_DURATION = 1F;
	private final Label label;

	public ActionPointsIndicator(Texture actionsPointsTexture, BitmapFont font, int actionPoints) {
		super();
		setBackground(new TextureRegionDrawable(actionsPointsTexture));
		setSize(actionsPointsTexture.getWidth(), actionsPointsTexture.getHeight());
		label = new Label(actionPoints + "", new Label.LabelStyle(font, Color.WHITE));
		add(label);
	}

	public void applyVisibility(boolean visible) {
		addAction(visible ? Actions.fadeIn(FADING_DURATION, smooth2) : Actions.fadeOut(FADING_DURATION, smooth2));
		label.setVisible(visible);
	}

	public void updateValue(int newValue) {
		label.setText(newValue);
	}
}
