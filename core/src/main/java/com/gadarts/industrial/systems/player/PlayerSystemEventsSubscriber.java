package com.gadarts.industrial.systems.player;

import com.badlogic.gdx.utils.Queue;
import com.gadarts.industrial.components.player.Item;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.systems.SystemEventsSubscriber;
import com.gadarts.industrial.systems.character.commands.CharacterCommand;

public interface PlayerSystemEventsSubscriber extends SystemEventsSubscriber {
	default void onPlayerPathCreated(MapGraphNode destination) {

	}

	default void onPlayerAppliedCommand(Queue<CharacterCommand> commands) {

	}

	default void onItemAddedToStorage(Item item) {

	}

	default void onPlayerFinishedTurn( ) {

	}

	default void onPlayerStatusChanged(boolean status) {

	}
}
