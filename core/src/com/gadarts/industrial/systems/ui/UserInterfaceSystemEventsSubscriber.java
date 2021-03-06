package com.gadarts.industrial.systems.ui;

import com.gadarts.industrial.components.player.Weapon;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.systems.SystemEventsSubscriber;

public interface UserInterfaceSystemEventsSubscriber extends SystemEventsSubscriber {
	default void onMenuToggled(boolean active) {

	}

	default void onSelectedWeaponChanged(Weapon selectedWeapon) {

	}

	default void onUserSelectedNodeToApplyTurn(final MapGraphNode node, AttackNodesHandler attackNodesHandler) {

	}

	default void onNewGameSelectedInMenu() {
		
	}
}
