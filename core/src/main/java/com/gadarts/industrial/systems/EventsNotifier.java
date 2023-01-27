package com.gadarts.industrial.systems;

public interface EventsNotifier<T> {
	void subscribeForEvents(T sub);
}
