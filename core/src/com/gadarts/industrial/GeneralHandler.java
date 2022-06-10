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
import com.badlogic.gdx.utils.Disposable;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import com.gadarts.industrial.components.character.CharacterAnimation;
import com.gadarts.industrial.components.character.CharacterAnimations;
import com.gadarts.industrial.console.Console;
import com.gadarts.industrial.console.ConsoleImpl;
import com.gadarts.industrial.map.MapBuilder;
import com.gadarts.industrial.systems.*;
import com.gadarts.industrial.systems.ui.UserInterfaceSystem;
import com.gadarts.industrial.systems.ui.UserInterfaceSystemEventsSubscriber;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.gadarts.industrial.systems.ui.menu.NewGameMenuOptions.OFFICE;


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
	private GameAssetsManager assetsManager;
	private SoundPlayer soundPlayer;
	private MapBuilder mapBuilder;
	private SystemsCommonData systemsCommonData;
	private boolean inGame;
	private Console console;

	private void addSystems(SystemsCommonData systemsCommonData) {
		Arrays.stream(Systems.values()).forEach(systemDefinition -> {
			try {
				Object system = systemDefinition.getSystemClass().getConstructors()[0].newInstance(
						systemsCommonData,
						soundPlayer,
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

	private CharacterAnimations createCharacterAnimations(final Assets.Atlases zealot) {
		CharacterAnimations animations = new CharacterAnimations();
		TextureAtlas atlas = assetsManager.getAtlas(zealot);
		Arrays.stream(SpriteType.values()).forEach(spriteType -> {
			if (spriteType.isSingleAnimation()) {
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
		systemsCommonData = new SystemsCommonData(versionName, versionNumber);
		createAndSetMap(mapName);
	}

	private void createAndSetEngine( ) {
		Optional.ofNullable(engine).ifPresent(e -> {
			e.clearPools();
			e.removeAllEntities();
			e.removeAllSystems();
		});
		this.engine = new PooledEngine();
	}

	private void initializeAssets( ) {
		assetsManager = new GameAssetsManager();
		assetsManager.loadGameFiles();
		generateCharactersAnimations();
		applyAlphaOnModels();
		generateModelsBoundingBoxes();
		assetsManager.applyRepeatWrapOnAllTextures();
	}

	@SuppressWarnings("unchecked")
	private void initializeSystems( ) {
		addSystems(systemsCommonData);
		ImmutableArray<EntitySystem> systems = engine.getSystems();
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
	}

	@Override
	public void onNewGameSelectedInMenu( ) {
		startNewGame(OFFICE.name());
	}

	public void init( ) {
		initializeAssets();
		soundPlayer = new SoundPlayer(assetsManager);
		createAndSetEngine();
		initializeSystemsCommonData("office");
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
		String spriteTypeName = spriteType.name().toLowerCase();
		String name = (spriteType.isSingleAnimation()) ? spriteTypeName : spriteTypeName + "_" + dir.name().toLowerCase();
		CharacterAnimation a = createAnimation(atlas, spriteType, name, dir);
		if (a.getKeyFrames().length > 0) {
			animations.put(spriteType, dir, a);
		}
	}

	private CharacterAnimation createAnimation(final TextureAtlas atlas,
											   final SpriteType spriteType,
											   final String name,
											   final Direction dir) {
		return new CharacterAnimation(
				spriteType.getAnimationDuration(),
				atlas.findRegions(name),
				spriteType.getPlayMode(),
				dir);
	}

	public void update(float delta) {
		engine.update(delta);
	}

	@Override
	public boolean isInGame( ) {
		return inGame;
	}
}
