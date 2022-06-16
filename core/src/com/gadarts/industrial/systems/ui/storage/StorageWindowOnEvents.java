package com.gadarts.industrial.systems.ui.storage;

import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.model.pickups.WeaponsDefinitions;
import com.gadarts.industrial.systems.ui.UserInterfaceSystemEventsSubscriber;
import com.gadarts.industrial.systems.ui.window.GameWindowEventType;
import com.gadarts.industrial.systems.ui.window.OnEvent;
import com.gadarts.industrial.systems.ui.window.WindowEventParameters;

import java.util.List;

enum StorageWindowOnEvents {

	ITEM_SELECTED(GameWindowEventType.ITEM_SELECTED, (parameters) -> {
		parameters.getSoundPlayer().playSound(Assets.Sounds.UI_ITEM_SELECT);
		ItemDisplay target = (ItemDisplay) parameters.getWindowEvent().getTarget();
		StorageWindow storageWindow = (StorageWindow) parameters.getTarget();
		if (parameters.getSelectedItem().getSelection() != target) {
			storageWindow.applySelectedItem(target);
		} else {
			storageWindow.clearSelectedItem();
		}
		return false;
	}),

	ITEM_PLACED(GameWindowEventType.ITEM_PLACED, (parameters) -> {
		parameters.getSoundPlayer().playSound(Assets.Sounds.UI_ITEM_PLACED);
		StorageWindow storageWindow = (StorageWindow) parameters.getTarget();
		if (parameters.getWindowEvent().getTarget() instanceof PlayerLayout) {
			storageWindow.findActor(StorageGrid.NAME).notify(parameters.getWindowEvent(), false);
		} else {
			storageWindow.findActor(PlayerLayout.NAME).notify(parameters.getWindowEvent(), false);
		}
		storageWindow.clearSelectedItem();
		return true;
	}),

	CLICK_RIGHT(GameWindowEventType.CLICK_RIGHT, (parameters) -> {
		StorageWindow storageWindow = (StorageWindow) parameters.getTarget();
		return storageWindow.onRightClick();
	}),

	WINDOW_CLOSED(GameWindowEventType.WINDOW_CLOSED, (parameters) -> {
		StorageWindow storageWindow = (StorageWindow) parameters.getTarget();
		if (parameters.getWindowEvent().getTarget() == storageWindow) {
			if (storageWindow.getPlayerLayout().getWeaponChoice() == null) {
				StorageGrid storageGrid = storageWindow.getStorageGrid();
				ItemDisplay itemDisplay = storageGrid.findItemDisplay(WeaponsDefinitions.HAMMER.getId());
				List<UserInterfaceSystemEventsSubscriber> subscribers = parameters.getSubscribers();
				storageWindow.getPlayerLayout().applySelectionToSelectedWeapon(storageGrid, itemDisplay, subscribers);
			}
		}
		return false;
	});

	private final GameWindowEventType type;
	private final OnEvent onEvent;

	StorageWindowOnEvents(final GameWindowEventType type, final OnEvent onEvent) {
		this.type = type;
		this.onEvent = onEvent;
	}

	public static boolean execute(WindowEventParameters windowEventParameters) {
		StorageWindowOnEvents[] values = values();
		for (StorageWindowOnEvents e : values) {
			if (e.type == windowEventParameters.getWindowEvent().getType()) {
				return e.onEvent.execute(windowEventParameters);
			}
		}
		return false;
	}
}
