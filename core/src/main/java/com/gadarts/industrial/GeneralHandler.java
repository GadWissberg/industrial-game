package com.gadarts.industrial;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.utils.Disposable;
import com.gadarts.industrial.console.Console;
import com.gadarts.industrial.console.ConsoleImpl;
import com.gadarts.industrial.map.MapBuilder;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.systems.*;
import com.gadarts.industrial.systems.ui.UserInterfaceSystemEventsSubscriber;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@RequiredArgsConstructor
public class GeneralHandler implements
		GameLifeCycleHandler,
		Disposable,
		UserInterfaceSystemEventsSubscriber {
	private final Map<
			Class<? extends SystemEventsSubscriber>,
			GameSystem<? extends SystemEventsSubscriber>> subscribersInterfaces = new HashMap<>();
	private final String versionName;
	private final int versionNumber;
	private final GameAssetManager assetsManager;
	private final SoundPlayer soundPlayer;
	private PooledEngine engine;
	@Getter
	private MapBuilder mapBuilder;
	private SystemsCommonData systemsCommonData;
	private boolean inGame;
	private Console console;
	private boolean restartGame;

	public void createAndSetMap(String mapName) {
		if (mapBuilder == null) {
			mapBuilder = new MapBuilder(engine, assetsManager);
		} else {
			mapBuilder.reset(engine);
		}
		systemsCommonData.setMap(mapBuilder.inflateTestMap(mapName));
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
	}

	@Override
	public void onNewGameSelectedInMenu( ) {
		startNewGame(DebugSettings.TEST_LEVEL);
	}

	public void init( ) {
		createAndSetEngine();
		initializeSystemsCommonData(DebugSettings.TEST_LEVEL);
		initializeSystems();
		createConsole();
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

	@Override
	public void dispose( ) {
		engine.getSystems().forEach(system -> ((GameSystem<? extends SystemEventsSubscriber>) system).dispose());
		mapBuilder.dispose();
		console.dispose();
		systemsCommonData.dispose();
	}

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
			engine.removeAllEntities();
			engine.clearPools();
		} else {
			this.engine = new PooledEngine();
		}
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


}
