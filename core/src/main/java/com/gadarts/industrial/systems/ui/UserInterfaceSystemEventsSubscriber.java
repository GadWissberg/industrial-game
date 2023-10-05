package com.gadarts.industrial.systems.ui;

import com.gadarts.industrial.components.player.Weapon;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.systems.SystemEventsSubscriber;

public interface UserInterfaceSystemEventsSubscriber extends SystemEventsSubscriber {
	default void onMenuToggled(boolean active) {

	}

	default void onSelectedWeaponChanged(Weapon selectedWeapon) {

	}

	default void onUserSelectedNodeToApplyTurn(MapGraphNode node) {

	}

	default void onNewGameSelectedInMenu( ) {

	}

	default void onUserLeftClickedThePlayer(MapGraphNode protagonistNode) {

	}

	default void onUserRequestsToReload( ) {

	}
}
