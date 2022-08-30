package com.gadarts.industrial.systems.player;

import com.gadarts.industrial.components.player.Item;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.systems.SystemEventsSubscriber;
import com.gadarts.industrial.systems.character.commands.CharacterCommand;

import java.util.List;

public interface PlayerSystemEventsSubscriber extends SystemEventsSubscriber {
	default void onPlayerPathCreated(MapGraphNode destination) {

	}

	default void onPlayerAppliedCommand(CharacterCommand command) {

	}

	default void onItemAddedToStorage(Item item) {

	}

	default void onPlayerFinishedTurn( ) {

	}

	default void onPlayerStatusChanged(boolean status) {

	}
}
