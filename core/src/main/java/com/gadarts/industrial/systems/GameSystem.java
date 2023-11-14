package com.gadarts.industrial.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.utils.Disposable;
import com.gadarts.industrial.console.ConsoleEventsSubscriber;
import com.gadarts.industrial.screens.GameLifeCycleManager;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter(AccessLevel.PROTECTED)
@RequiredArgsConstructor
public abstract class GameSystem<T extends SystemEventsSubscriber> extends EntitySystem implements
		Disposable,
		EventsNotifier<T>,
		ConsoleEventsSubscriber {
	protected final List<T> subscribers = new ArrayList<>();
	private final GameAssetManager assetsManager;
	private final GameLifeCycleManager gameLifeCycleManager;
	private SystemsCommonData systemsCommonData;


	public void reset( ) {

	}

	public void onSystemReset(SystemsCommonData systemsCommonData) {
		this.systemsCommonData = systemsCommonData;
	}

	public abstract Class<T> getEventsSubscriberClass( );

	@Override
	public void subscribeForEvents(final T sub) {
		if (subscribers.contains(sub)) return;
		subscribers.add(sub);
	}

	public abstract void initializeData( );
}
