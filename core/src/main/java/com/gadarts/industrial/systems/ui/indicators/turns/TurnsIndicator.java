package com.gadarts.industrial.systems.ui.indicators.turns;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Queue;
import com.gadarts.industrial.components.ComponentsMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TurnsIndicator extends Image {
	public static final float TURNS_INDICATOR_FADING_DURATION = 1F;
	public static final float ICON_FADING_DURATION = 2F;
	private static final Vector2 auxVector = new Vector2();
	private static final float PADDING_EDGE = 120F;
	private static final float PADDING_ICON_RIGHT = 25F;
	private static final float PADDING_ICON_TOP = 10F;
	private final Texture greenIconTexture;
	private final Texture redIconTexture;
	private final HashMap<String, Texture> charactersIcons;
	private final List<TurnsIndicatorIcon> icons = new ArrayList<>();

	public TurnsIndicator(Texture barTexture, Texture greenIconTexture, Texture redIconTexture, HashMap<String, Texture> icons) {
		super(barTexture);
		this.greenIconTexture = greenIconTexture;
		this.redIconTexture = redIconTexture;
		this.charactersIcons = icons;
	}

	public void applyCombatMode(Queue<Entity> turnsQueue) {
		addAction(Actions.fadeIn(TURNS_INDICATOR_FADING_DURATION, Interpolation.swing));
		turnsQueue.forEach(character -> {
			boolean isPlayer = ComponentsMapper.player.has(character);
			TurnsIndicatorIcon icon = new TurnsIndicatorIcon(isPlayer ? greenIconTexture : redIconTexture);
			icon.getColor().a = 0F;
			icon.addAction(Actions.fadeIn(ICON_FADING_DURATION, Interpolation.smoother));
			icons.add(icon);
			getParent().addActor(icon);
		});
		for (int i = 0; i < icons.size(); i++) {
			auxVector.set(0F, PADDING_EDGE);
			TurnsIndicatorIcon icon = icons.get(i);
			float iconPrefHeight = icon.getPrefHeight();
			auxVector.x = auxVector.x - icon.getPrefWidth() / 2F - PADDING_ICON_RIGHT;
			float interval = i * (iconPrefHeight + PADDING_ICON_TOP);
			auxVector.y = auxVector.y - iconPrefHeight / 2F + interval;
			localToScreenCoordinates(auxVector);
			icon.setPosition(auxVector.x, auxVector.y);
		}
	}

}
