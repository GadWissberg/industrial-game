package com.gadarts.industrial.systems.character;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.components.player.WeaponAmmo;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.systems.SystemEventsSubscriber;

public interface CharacterSystemEventsSubscriber extends SystemEventsSubscriber {
	default void onCharacterCommandDone(Entity character) {

	}

	default void onCharacterNodeChanged(Entity entity, MapGraphNode oldNode, MapGraphNode newNode) {

	}

	default void onItemPickedUp(final Entity itemPickedUp) {

	}

	default void onDestinationReached(Entity character) {

	}

	default void onCharacterDies(Entity character) {

	}

	default void onCharacterGotDamage(Entity character, int originalValue) {

	}

	default void onCharacterEngagesPrimaryAttack(Entity character, Vector3 direction, Vector3 positionNodeCenterPosition) {

	}

	default void onCharacterFinishedTurn( ) {

	}

	default void onCharacterConsumedActionPoint(Entity character, int newValue) {

	}

	default void onCharacterReload(Entity character, WeaponAmmo weaponAmmo) {
	}
}
