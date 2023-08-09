package com.gadarts.industrial.systems.ui.indicators.turns;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class TurnsIndicatorIcon extends Table {
	private final Image icon;

	public TurnsIndicatorIcon(Texture texture) {
		super();
		setBackground(new TextureRegionDrawable(texture));
		setSize(texture.getWidth(), texture.getHeight());
		icon = new Image();
		add(icon);
	}

	public void applyIcon(TextureRegionDrawable icon) {
		this.icon.setDrawable(icon);
	}
}
