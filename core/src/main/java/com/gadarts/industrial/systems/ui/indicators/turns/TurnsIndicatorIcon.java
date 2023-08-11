package com.gadarts.industrial.systems.ui.indicators.turns;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

public class TurnsIndicatorIcon extends Table {
	private final Image icon;
	private final Image border;

	public TurnsIndicatorIcon(Texture texture, Texture borderTexture) {
		super();
		setBackground(new TextureRegionDrawable(texture));
		setSize(texture.getWidth(), texture.getHeight());
		icon = new Image();
		icon.setScaling(Scaling.none);
		border = new Image(borderTexture);
		Stack stack = new Stack(icon, border);
		add(stack);
		border.setVisible(false);
	}

	public void applyIcon(TextureRegionDrawable icon) {
		this.icon.setDrawable(icon);
	}

	public void setBorderVisibility(boolean borderVisibility) {
		border.setVisible(borderVisibility);
	}
}
