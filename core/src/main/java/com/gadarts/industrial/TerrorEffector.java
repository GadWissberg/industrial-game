package com.gadarts.industrial;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.gadarts.industrial.components.character.CharacterAnimation;
import com.gadarts.industrial.components.character.CharacterAnimations;
import com.gadarts.industrial.screens.MenuScreen;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.shared.model.characters.SpriteType;

import java.util.Arrays;
import java.util.stream.IntStream;

public class TerrorEffector extends Game {

	public static final int FULL_SCREEN_RESOLUTION_WIDTH = 1920;
	public static final int FULL_SCREEN_RESOLUTION_HEIGHT = 1080;
	public static final int WINDOWED_RESOLUTION_WIDTH = 1280;
	public static final int WINDOWED_RESOLUTION_HEIGHT = 960;
	public static final String BOUNDING_BOX_PREFIX = "box_";
	private final String versionName;
	private final int versionNumber;
	private final GameAssetManager assetsManager;
	private SoundPlayer soundPlayer;

	public TerrorEffector(String versionName, int versionNumber) {
		assetsManager = new GameAssetManager();
		this.versionName = versionName;
		this.versionNumber = versionNumber;
	}

	private String formatNameForVariation(Direction dir,
										  String sprTypeName,
										  int vars,
										  int variationIndex,
										  boolean singleDirection) {
		return String.format("%s%s%s",
				sprTypeName,
				vars > 1 ? "_" + variationIndex : "",
				singleDirection ? "" : "_" + dir.name().toLowerCase());
	}

	@Override
	public void create( ) {
		Gdx.input.setCursorCatched(true);
		if (DebugSettings.FULL_SCREEN) {
			Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
		} else {
			Gdx.graphics.setWindowedMode(WINDOWED_RESOLUTION_WIDTH, WINDOWED_RESOLUTION_HEIGHT);
		}
		GeneralHandler generalHandler = new GeneralHandler(versionName, versionNumber, assetsManager, soundPlayer);
		Gdx.app.setLogLevel(DebugSettings.LOG_LEVEL);
		initializeAssets();
		soundPlayer = new SoundPlayer(assetsManager);
		setScreen(new MenuScreen(assetsManager, soundPlayer, versionName));
	}

	private void generateModelsBoundingBoxes( ) {
		Arrays.stream(Assets.Models.values())
				.forEach(def -> {
					Model model = assetsManager.get(def.getFilePath(), Model.class);
					assetsManager.addAsset(
							BOUNDING_BOX_PREFIX + def.getFilePath(),
							BoundingBox.class,
							model.calculateBoundingBox(new BoundingBox()));
				});
	}

	private void generateCharactersAnimations( ) {
		Arrays.stream(Assets.Atlases.values())
				.forEach(atlas -> assetsManager.addAsset(
						atlas.name(),
						CharacterAnimations.class,
						createCharacterAnimations(atlas)));
	}

	private CharacterAnimations createCharacterAnimations(final Assets.Atlases character) {
		CharacterAnimations animations = new CharacterAnimations();
		TextureAtlas atlas = assetsManager.getAtlas(character);
		Arrays.stream(SpriteType.values())
				.filter(spriteType -> checkIfAtlasContainsSpriteType(spriteType, atlas))
				.forEach(spriteType -> {
					if (spriteType.isSingleDirection()) {
						inflateCharacterAnimation(animations, atlas, spriteType, Direction.SOUTH);
					} else {
						Direction[] directions = Direction.values();
						Arrays.stream(directions).forEach(dir -> inflateCharacterAnimation(animations, atlas, spriteType, dir));
					}
				});
		return animations;
	}

	private void inflateCharacterAnimation(final CharacterAnimations animations,
										   final TextureAtlas atlas,
										   final SpriteType spriteType,
										   final Direction dir) {
		String sprTypeName = spriteType.name().toLowerCase();
		int vars = spriteType.getVariations();
		IntStream.range(0, vars).forEach(variationIndex -> {
			String name = formatNameForVariation(dir, sprTypeName, vars, variationIndex, spriteType.isSingleDirection());
			CharacterAnimation a = createAnimation(atlas, spriteType, name, dir);
			if (a == null && variationIndex == 0) {
				name = formatNameForVariation(dir, sprTypeName, 1, 0, spriteType.isSingleDirection());
				a = createAnimation(atlas, spriteType, name, dir);
				animations.put(spriteType, 0, dir, a);
			} else if (a != null && a.getKeyFrames().length > 0) {
				animations.put(spriteType, variationIndex, dir, a);
			}
		});
	}

	private CharacterAnimation createAnimation(final TextureAtlas atlas,
											   final SpriteType spriteType,
											   final String name,
											   final Direction dir) {
		Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions(name);
		CharacterAnimation animation = null;
		if (!regions.isEmpty()) {
			animation = new CharacterAnimation(
					spriteType.getFrameDuration(),
					regions,
					spriteType.getPlayMode(),
					dir);
		}
		return animation;
	}

	private void initializeAssets( ) {
		assetsManager.loadGameFiles();
		generateCharactersAnimations();
		applyAlphaOnModels();
		generateModelsBoundingBoxes();
		assetsManager.applyRepeatWrapOnAllTextures();
	}

	private boolean checkIfAtlasContainsSpriteType(SpriteType spriteType, TextureAtlas atlas) {
		boolean result = false;
		for (TextureAtlas.AtlasRegion region : atlas.getRegions()) {
			if (region.name.startsWith(spriteType.name().toLowerCase())) {
				result = true;
				break;
			}
		}
		return result;
	}

	private void applyAlphaOnModels( ) {
		Arrays.stream(Assets.Models.values()).filter(def -> def.getAlpha() < 1.0f)
				.forEach(def -> {
					Material material = assetsManager.getModel(def).materials.get(0);
					BlendingAttribute attribute = new BlendingAttribute();
					material.set(attribute);
					attribute.opacity = def.getAlpha();
				});
	}

	@Override
	public void dispose( ) {
		assetsManager.dispose();
	}
}
