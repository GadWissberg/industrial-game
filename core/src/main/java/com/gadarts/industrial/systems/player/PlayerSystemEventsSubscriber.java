package com.gadarts.industrial.systems.player;

import com.gadarts.industrial.components.player.WeaponAmmo;
import com.gadarts.industrial.components.player.Item;
import com.gadarts.industrial.systems.SystemEventsSubscriber;

public interface PlayerSystemEventsSubscriber extends SystemEventsSubscriber {



	default void onItemAddedToStorage(Item item, boolean firstTime) {

	}

	default void onPlayerFinishedTurn( ) {

	}


	default void onPlayerConsumedAmmo(WeaponAmmo weaponAmmo){

	}
}
