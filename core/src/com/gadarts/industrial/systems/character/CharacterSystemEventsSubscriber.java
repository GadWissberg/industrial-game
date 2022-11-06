package com.gadarts.industrial.systems.character;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.systems.SystemEventsSubscriber;
import com.gadarts.industrial.systems.character.commands.CharacterCommand;

public interface CharacterSystemEventsSubscriber extends SystemEventsSubscriber {
	default void onCharacterCommandDone(final Entity character, final CharacterCommand lastCommand) {

	}

	default void onCharacterRotated(Entity character) {

	}

	default void onCharacterNodeChanged(Entity entity, MapGraphNode oldNode, MapGraphNode newNode) {

	}

	default void onItemPickedUp(final Entity itemPickedUp) {

	}

	default void onDestinationReached(Entity character) {

	}

	default void onCharacterDies(Entity character) {

	}

	default void onCharacterGotDamage(Entity character) {

	}

	default void onCharacterEngagesPrimaryAttack(Entity character, Vector3 direction, Vector3 positionNodeCenterPosition) {

	}

	default void onCommandInitialized(Entity character, CharacterCommand currentCommand) {

	}

	default void onCharacterStillHasTime(Entity character) {

	}

	default void onCharacterFinishedTurn(Entity character) {

	}
}
