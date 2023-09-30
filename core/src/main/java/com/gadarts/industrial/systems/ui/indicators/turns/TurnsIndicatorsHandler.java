package com.gadarts.industrial.systems.ui.indicators.turns;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Queue;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.model.characters.player.PlayerDeclaration;
import com.gadarts.industrial.systems.ui.GameStage;
import com.gadarts.industrial.systems.ui.NoiseEffectHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TurnsIndicatorsHandler {
	public static final float ICON_FADING_DURATION = 0.5F;
	private static final Vector2 auxVector = new Vector2();
	private static final float PADDING_ICON_TOP = 10F;
	private static final float PADDING_ICON_RIGHT = 50F;
	private static final float PADDING_FIRST_ICON_TOP = 100F;
	private final TurnsIndicatorsHandlerTextures textures;
	private final BitmapFont font;
	private final NoiseEffectHandler noiseEffectHandler;
	private final GameStage stage;
	private final HashMap<String, TextureRegionDrawable> charactersIcons;
	private final TurnsIndicatorsHandlerState state = new TurnsIndicatorsHandlerState();

	public TurnsIndicatorsHandler(GameAssetManager assetsManager,
								  HashMap<String, TextureRegionDrawable> iconsMap,
								  NoiseEffectHandler noiseEffectHandler,
								  GameStage stage) {
		this.textures = new TurnsIndicatorsHandlerTextures(
				assetsManager.getTexture(Assets.UiTextures.HUD_ICON_CIRCLE_GREEN),
				assetsManager.getTexture(Assets.UiTextures.HUD_ICON_CIRCLE_RED),
				assetsManager.getTexture(Assets.UiTextures.HUD_ICON_CIRCLE_BORDER),
				assetsManager.getTexture(Assets.UiTextures.HUD_ACTION_POINTS_INDICATOR));
		this.charactersIcons = iconsMap;
		this.font = assetsManager.getFont(Assets.Fonts.HUD_SMALL);
		this.noiseEffectHandler = noiseEffectHandler;
		this.stage = stage;
	}

	public void applyCombatMode(Queue<Entity> turnsQueue) {
		turnsQueue.forEach(this::addIcon);
		turnsQueue.forEach(character -> {
			if (turnsQueue.first().equals(character)) {
				applyBorderForNewTurn(character);
			}
		});
		List<TurnsIndicatorIcon> entries = new ArrayList<>(state.getIconsMap().values());
		for (int i = 0; i < entries.size(); i++) {
			initPositionForIcon(i, entries.get(i));
		}
	}

	private void initPositionForIcon(int i, TurnsIndicatorIcon icon) {
		float iconPrefHeight = icon.getPrefHeight();
		auxVector.set(stage.getWidth() - PADDING_ICON_RIGHT, stage.getHeight() - PADDING_FIRST_ICON_TOP - iconPrefHeight);
		auxVector.x = auxVector.x - icon.getPrefWidth() / 2F;
		float interval = i * (iconPrefHeight + PADDING_ICON_TOP);
		auxVector.y = auxVector.y - iconPrefHeight / 2F - interval;
		icon.setPosition(auxVector.x, auxVector.y);
	}

	private void addIcon(Entity entity) {
		if (state.getIconsMap().containsKey(entity) || !ComponentsMapper.character.has(entity)) return;

		boolean isPlayer = ComponentsMapper.player.has(entity);
		Texture circleTexture = isPlayer ? textures.greenIconTexture() : textures.redIconTexture();
		int actionPoints = ComponentsMapper.character.get(entity).getAttributes().getActionPoints();
		TurnsIndicatorIconTextures textures = new TurnsIndicatorIconTextures(
				circleTexture,
				this.textures.borderTexture(),
				this.textures.actionsPointsTexture());
		TurnsIndicatorIcon icon = new TurnsIndicatorIcon(
				textures,
				font,
				actionPoints,
				noiseEffectHandler,
				entity);
		String playerId = PlayerDeclaration.getInstance().id();
		icon.applyIcon(charactersIcons.get(isPlayer ? playerId : ComponentsMapper.enemy.get(entity).getEnemyDeclaration().id()));
		icon.getColor().a = 0F;
		icon.addAction(Actions.fadeIn(ICON_FADING_DURATION, Interpolation.smoother));
		state.getIconsMap().put(entity, icon);
		state.getIconsList().add(icon);
		stage.addActor(icon);
	}

	public void applyBorderForNewTurn(Entity entity) {
		Map<Entity, TurnsIndicatorIcon> iconsMap = state.getIconsMap();
		if (!iconsMap.containsKey(entity)) return;

		TurnsIndicatorIcon current = iconsMap.get(state.getCurrentBorder());
		if (current != null) {
			current.setBorderVisibility(false);
		}
		TurnsIndicatorIcon turnsIndicatorIcon = iconsMap.get(entity);
		turnsIndicatorIcon.setBorderVisibility(true);
		turnsIndicatorIcon.updateActionPointsIndicator(ComponentsMapper.character.get(entity).getAttributes().getActionPoints());
		state.setCurrentBorder(entity);
	}

	public void addCharacter(Entity enemy) {
		Map<Entity, TurnsIndicatorIcon> iconsMap = state.getIconsMap();
		if (iconsMap.containsKey(enemy)) return;

		addIcon(enemy);
		initPositionForIcon(iconsMap.size() - 1, iconsMap.get(enemy));
	}

	public void removeCharacter(Entity character) {
		Map<Entity, TurnsIndicatorIcon> iconsMap = state.getIconsMap();
		TurnsIndicatorIcon icon = iconsMap.get(character);
		icon.addAction(Actions.sequence(Actions.fadeOut(ICON_FADING_DURATION), Actions.removeActor()));
		List<TurnsIndicatorIcon> iconsList = state.getIconsList();
		int removedIndex = iconsList.indexOf(iconsMap.get(character));
		iconsMap.remove(character);
		iconsList.remove(icon);
		if (iconsMap.size() <= 1) {
			turnOffCombatMode();
		} else {
			for (int i = removedIndex; i < iconsList.size(); i++) {
				icon = iconsList.get(i);
				MoveByAction action = Actions.moveBy(
						0F,
						icon.getPrefHeight() + PADDING_ICON_TOP,
						1F,
						Interpolation.slowFast);
				iconsMap.get(icon.getCharacter()).addAction(action);
			}
		}
	}

	private void turnOffCombatMode( ) {
		Map<Entity, TurnsIndicatorIcon> iconsMap = state.getIconsMap();
		iconsMap.values().forEach(Actor::remove);
		iconsMap.clear();
	}

	public void updateCurrentActionPointsIndicator(Entity character, int newValue) {
		Map<Entity, TurnsIndicatorIcon> iconsMap = state.getIconsMap();
		if (!iconsMap.containsKey(character)) return;

		iconsMap.get(character).updateActionPointsIndicator(newValue);
	}

	public void applyDamageEffect(Entity character) {
		Map<Entity, TurnsIndicatorIcon> iconsMap = state.getIconsMap();
		if (!iconsMap.containsKey(character)) return;

		iconsMap.get(character).applyDamageEffect();
	}
}
