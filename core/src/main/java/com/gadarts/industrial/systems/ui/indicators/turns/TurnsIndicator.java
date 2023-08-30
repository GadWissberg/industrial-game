package com.gadarts.industrial.systems.ui.indicators.turns;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Queue;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.model.characters.player.PlayerDeclaration;
import com.gadarts.industrial.systems.ui.NoiseEffectHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TurnsIndicator extends Image {
	public static final float TURNS_INDICATOR_FADING_DURATION = 0.2F;
	public static final float ICON_FADING_DURATION = 0.5F;
	private static final Vector2 auxVector = new Vector2();
	private static final float PADDING_EDGE = 120F;
	private static final float PADDING_ICON_RIGHT = 25F;
	private static final float PADDING_ICON_TOP = 10F;
	private final Texture greenIconTexture;
	private final Texture redIconTexture;
	private final HashMap<String, TextureRegionDrawable> charactersIcons;
	private final Map<Entity, TurnsIndicatorIcon> icons = new HashMap<>();
	private final Texture borderTexture;
	private final Texture actionsPointsTexture;
	private final BitmapFont font;
	private final NoiseEffectHandler noiseEffectHandler;
	private Entity currentBorder;

	public TurnsIndicator(GameAssetManager assetsManager,
						  HashMap<String, TextureRegionDrawable> icons,
						  NoiseEffectHandler noiseEffectHandler) {
		super(assetsManager.getTexture(Assets.UiTextures.HUD_TURNS_INDICATOR_BAR));
		this.greenIconTexture = assetsManager.getTexture(Assets.UiTextures.HUD_ICON_CIRCLE_GREEN);
		this.redIconTexture = assetsManager.getTexture(Assets.UiTextures.HUD_ICON_CIRCLE_RED);
		this.charactersIcons = icons;
		this.borderTexture = assetsManager.getTexture(Assets.UiTextures.HUD_ICON_CIRCLE_BORDER);
		this.actionsPointsTexture = assetsManager.getTexture(Assets.UiTextures.HUD_ACTION_POINTS_INDICATOR);
		this.font = assetsManager.getFont(Assets.Fonts.HUD_SMALL);
		this.noiseEffectHandler = noiseEffectHandler;
	}

	public void applyCombatMode(Queue<Entity> turnsQueue) {
		addAction(Actions.fadeIn(TURNS_INDICATOR_FADING_DURATION, Interpolation.swing));
		turnsQueue.forEach(this::addIcon);
		turnsQueue.forEach(character -> {
			if (turnsQueue.first().equals(character)) {
				applyBorderForNewTurn(character);
			}
		});
		List<TurnsIndicatorIcon> entries = new ArrayList<>(icons.values());
		for (int i = 0; i < entries.size(); i++) {
			initPositionForIcon(i, entries.get(i));
		}
	}

	private void initPositionForIcon(int i, TurnsIndicatorIcon icon) {
		auxVector.set(0F, PADDING_EDGE);
		float iconPrefHeight = icon.getPrefHeight();
		auxVector.x = auxVector.x - icon.getPrefWidth() / 2F - PADDING_ICON_RIGHT;
		float interval = i * (iconPrefHeight + PADDING_ICON_TOP);
		auxVector.y = auxVector.y - iconPrefHeight / 2F + interval;
		localToScreenCoordinates(auxVector);
		icon.setPosition(auxVector.x, auxVector.y);
	}

	private void addIcon(Entity character) {
		if (icons.containsKey(character)) return;
		boolean isPlayer = ComponentsMapper.player.has(character);
		Texture circleTexture = isPlayer ? greenIconTexture : redIconTexture;
		int actionPoints = ComponentsMapper.character.get(character).getAttributes().getActionPoints();
		TurnsIndicatorIcon icon = new TurnsIndicatorIcon(
				circleTexture,
				borderTexture,
				actionsPointsTexture,
				font,
				actionPoints,
				noiseEffectHandler);
		String playerId = PlayerDeclaration.getInstance().id();
		icon.applyIcon(charactersIcons.get(isPlayer ? playerId : ComponentsMapper.enemy.get(character).getEnemyDeclaration().id()));
		icon.getColor().a = 0F;
		icon.addAction(Actions.fadeIn(ICON_FADING_DURATION, Interpolation.smoother));
		icons.put(character, icon);
		getParent().addActor(icon);
	}

	public void applyBorderForNewTurn(Entity entity) {
		if (!icons.containsKey(entity)) return;
		TurnsIndicatorIcon current = icons.get(currentBorder);
		if (current != null) {
			current.setBorderVisibility(false);
		}
		TurnsIndicatorIcon turnsIndicatorIcon = icons.get(entity);
		turnsIndicatorIcon.setBorderVisibility(true);
		turnsIndicatorIcon.updateActionPointsIndicator(ComponentsMapper.character.get(entity).getAttributes().getActionPoints());
		currentBorder = entity;
	}

	public void addCharacter(Entity enemy) {
		if (icons.containsKey(enemy)) return;
		addIcon(enemy);
		initPositionForIcon(icons.size() - 1, icons.get(enemy));
	}

	public void removeCharacter(Entity character) {
		if (icons.containsKey(character)) {
			TurnsIndicatorIcon icon = icons.get(character);
			icon.addAction(Actions.sequence(Actions.fadeOut(ICON_FADING_DURATION), Actions.removeActor()));
		}
		icons.remove(character);
		if (icons.size() <= 1) {
			turnOffCombatMode();
		}
	}

	private void turnOffCombatMode( ) {
		addAction(Actions.fadeOut(TURNS_INDICATOR_FADING_DURATION, Interpolation.swing));
		icons.values().forEach(Actor::remove);
		icons.clear();
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		noiseEffectHandler.begin(batch);
		super.draw(batch, parentAlpha);
		noiseEffectHandler.end(batch);
	}

	public void updateCurrentActionPointsIndicator(Entity character, int newValue) {
		if (!icons.containsKey(character)) return;

		icons.get(character).updateActionPointsIndicator(newValue);
	}

}
