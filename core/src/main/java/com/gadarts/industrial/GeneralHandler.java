package com.gadarts.industrial;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.gadarts.industrial.components.character.CharacterAnimation;
import com.gadarts.industrial.components.character.CharacterAnimations;
import com.gadarts.industrial.console.Console;
import com.gadarts.industrial.console.ConsoleImpl;
import com.gadarts.industrial.map.MapBuilder;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import com.gadarts.industrial.systems.*;
import com.gadarts.industrial.systems.ui.UserInterfaceSystem;
import com.gadarts.industrial.systems.ui.UserInterfaceSystemEventsSubscriber;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;


@RequiredArgsConstructor
public class GeneralHandler implements
		GameLifeCycleHandler,
		Disposable,
		UserInterfaceSystemEventsSubscriber {
	public static final String BOUNDING_BOX_PREFIX = "box_";
	private final Map<
			Class<? extends SystemEventsSubscriber>,
			GameSystem<? extends SystemEventsSubscriber>> subscribersInterfaces = new HashMap<>();
	private final String versionName;
	private final int versionNumber;
	private PooledEngine engine;
	private GameAssetManager assetsManager;
	private SoundPlayer soundPlayer;
	private MapBuilder mapBuilder;
	private SystemsCommonData systemsCommonData;
	private boolean inGame;
	private Console console;
	private boolean restartGame;

	private void addSystems( ) {
		Arrays.stream(Systems.values()).forEach(systemDefinition -> {
			try {
				Object system = systemDefinition.getSystemClass().getConstructors()[0].newInstance(
						assetsManager,
						this);
				engine.addSystem((GameSystem<? extends SystemEventsSubscriber>) system);
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
		});
	}

	public void createAndSetMap(String mapName) {
		if (mapBuilder == null) {
			mapBuilder = new MapBuilder(engine, assetsManager);
		} else {
			mapBuilder.reset(engine);
		}
		systemsCommonData.setMap(mapBuilder.inflateTestMap(mapName));
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
		Arrays.stream(SpriteType.values()).forEach(spriteType -> {
			if (spriteType.isSingleDirection()) {
				inflateCharacterAnimation(animations, atlas, spriteType, Direction.SOUTH);
			} else {
				Direction[] directions = Direction.values();
				Arrays.stream(directions).forEach(dir -> inflateCharacterAnimation(animations, atlas, spriteType, dir));
			}
		});
		return animations;
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

	public void resetSystems( ) {
		engine.getSystems().forEach(system -> ((GameSystem<? extends SystemEventsSubscriber>) system).reset());
		initializeSystems();
	}

	public void startNewGame(String mapName) {
		createAndSetEngine();
		inGame = true;
		initializeSystemsCommonData(mapName);
		resetSystems();
		createConsole();
		engine.getSystem(UserInterfaceSystem.class).getMenuHandler().toggleMenu(false);
	}

	private void initializeSystemsCommonData(String mapName) {
		Optional.ofNullable(systemsCommonData).ifPresent(SystemsCommonData::dispose);
		systemsCommonData = new SystemsCommonData(versionName, versionNumber, soundPlayer);
		createAndSetMap(mapName);
	}

	private void createAndSetEngine( ) {
		if (engine != null) {
			engine.getSystems().forEach(s -> {
				if (s instanceof Disposable) {
					((Disposable) s).dispose();
				}
			});
			engine.clearPools();
//			engine.getSystems().forEach(system -> system.setProcessing(false));
			engine.removeAllEntities();
		} else {
			this.engine = new PooledEngine();
		}
	}

	private void initializeAssets( ) {
		assetsManager = new GameAssetManager();
		assetsManager.loadGameFiles();
		generateCharactersAnimations();
		applyAlphaOnModels();
		generateModelsBoundingBoxes();
		assetsManager.applyRepeatWrapOnAllTextures();
	}

	@SuppressWarnings("unchecked")
	private void initializeSystems( ) {
		if (engine.getSystems().size() == 0) {
			addSystems();
		}
		ImmutableArray<EntitySystem> systems = engine.getSystems();
		systems.forEach(system -> ((GameSystem<? extends SystemEventsSubscriber>) system).onSystemReset(systemsCommonData));
		systems.forEach(system -> ((GameSystem<? extends SystemEventsSubscriber>) system).initializeData());
		systems.forEach(system -> {
			GameSystem<? extends SystemEventsSubscriber> sys = (GameSystem<? extends SystemEventsSubscriber>) system;
			if (sys.getEventsSubscriberClass() != null) {
				subscribersInterfaces.put(sys.getEventsSubscriberClass(), sys);
			}
		});
		systems.forEach(system -> Arrays.stream(system.getClass().getInterfaces()).forEach(i -> {
			if (subscribersInterfaces.containsKey(i)) {
				EventsNotifier<SystemEventsSubscriber> s = engine.getSystem(subscribersInterfaces.get(i).getClass());
				s.subscribeForEvents((SystemEventsSubscriber) system);
			}
		}));
		Arrays.stream(getClass().getInterfaces()).forEach(i -> {
			if (subscribersInterfaces.containsKey(i)) {
				EventsNotifier<SystemEventsSubscriber> s = engine.getSystem(subscribersInterfaces.get(i).getClass());
				s.subscribeForEvents(this);
			}
		});
		engine.getSystems().forEach(system -> system.setProcessing(true));
	}

	@Override
	public void onNewGameSelectedInMenu( ) {
		startNewGame(DebugSettings.TEST_LEVEL);
	}

	public void init( ) {
		initializeAssets();
		soundPlayer = new SoundPlayer(assetsManager);
		createAndSetEngine();
		initializeSystemsCommonData(DebugSettings.TEST_LEVEL);
		initializeSystems();
		createConsole();
	}

	private void createConsole( ) {
		Optional.ofNullable(console).ifPresent(Console::dispose);
		ConsoleImpl console = new ConsoleImpl();
		initializeConsole(console);
		this.console = console;
	}

	private void initializeConsole(ConsoleImpl console) {
		ImmutableArray<EntitySystem> systems = engine.getSystems();
		addSubscribersForConsole(console, systems);
		console.init(assetsManager, systemsCommonData);
		systemsCommonData.getUiStage().addActor(console);
		InputMultiplexer multiplexer = (InputMultiplexer) Gdx.input.getInputProcessor();
		multiplexer.addProcessor(console);
	}

	private void addSubscribersForConsole(ConsoleImpl console, ImmutableArray<EntitySystem> systems) {
		systems.forEach(system -> console.subscribeForEvents((GameSystem<? extends SystemEventsSubscriber>) system));
		console.subscribeForEvents(soundPlayer);
	}

	@Override
	public void dispose( ) {
		engine.getSystems().forEach(system -> ((GameSystem<? extends SystemEventsSubscriber>) system).dispose());
		assetsManager.dispose();
		mapBuilder.dispose();
		console.dispose();
		systemsCommonData.dispose();
	}

	private void inflateCharacterAnimation(final CharacterAnimations animations,
										   final TextureAtlas atlas,
										   final SpriteType spriteType,
										   final Direction dir) {
		String sprTypeName = Optional.ofNullable(spriteType.getRegionName()).orElse(spriteType.name()).toLowerCase();
		int vars = spriteType.getVariations();
		IntStream.range(0, vars).forEach(variationIndex -> {
			String name = formatNameForVariation(dir, sprTypeName, vars, variationIndex, spriteType.isSingleDirection());
			CharacterAnimation a = createAnimation(atlas, spriteType, name, dir);
			if (a != null && a.getKeyFrames().length > 0) {
				animations.put(spriteType, variationIndex, dir, a);
			}
		});
	}

	private static String formatNameForVariation(Direction dir,
												 String sprTypeName,
												 int vars,
												 int variationIndex,
												 boolean singleDirection) {
		return String.format("%s%s%s",
				sprTypeName,
				vars > 1 ? "_" + variationIndex : "",
				singleDirection ? "" : "_" + dir.name().toLowerCase());
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

	public void update(float delta) {
		if (restartGame) {
			restartGame = false;
			onNewGameSelectedInMenu();
		}
		engine.update(delta);
	}

	@Override
	public boolean isInGame( ) {
		return inGame;
	}

	@Override
	public void raiseFlagToRestartGame( ) {
		restartGame = true;
	}
}
