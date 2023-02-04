package com.gadarts.industrial.systems.ui.storage;

import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.Assets.Declarations;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.shared.assets.declarations.weapons.WeaponsDeclarations;
import com.gadarts.industrial.systems.player.PlayerStorage;
import com.gadarts.industrial.systems.ui.UserInterfaceSystemEventsSubscriber;
import com.gadarts.industrial.systems.ui.window.GameWindowEventType;
import com.gadarts.industrial.systems.ui.window.OnEvent;
import com.gadarts.industrial.systems.ui.window.WindowEventParameters;

import java.util.List;
import java.util.Optional;

enum StorageWindowOnEvents {

	ITEM_SELECTED(GameWindowEventType.ITEM_SELECTED, (parameters, assetsManager, storage) -> {
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

	ITEM_PLACED(GameWindowEventType.ITEM_PLACED, (parameters, assetsManager, storage) -> {
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

	CLICK_RIGHT(GameWindowEventType.CLICK_RIGHT, (parameters, assetsManager, storage) -> {
		StorageWindow storageWindow = (StorageWindow) parameters.getTarget();
		return storageWindow.onRightClick();
	}),

	WINDOW_CLOSED(GameWindowEventType.WINDOW_CLOSED, (parameters, assetsManager, storage) -> {
		StorageWindow storageWindow = (StorageWindow) parameters.getTarget();
		if (parameters.getWindowEvent().getTarget() == storageWindow) {
			PlayerLayout playerLayout = storageWindow.getPlayerLayout();
			if (playerLayout.getWeaponChoice() == null) {
				StorageGrid storageGrid = storageWindow.getStorageGrid();
				WeaponsDeclarations declarations = (WeaponsDeclarations) assetsManager.getDeclaration(Declarations.WEAPONS);
				Optional.ofNullable(storageGrid.findItemDisplay(storage.getIndices().get(declarations.parsePlayerWeaponDeclaration("glc")))).ifPresent(itemDisplay -> {
							List<UserInterfaceSystemEventsSubscriber> subscribers = parameters.getSubscribers();
							playerLayout.applySelectionToSelectedWeapon(storageGrid, itemDisplay, subscribers);
						}
				);
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

	public static boolean execute(WindowEventParameters windowEventParameters,
								  GameAssetsManager assetsManager,
								  PlayerStorage storage) {
		StorageWindowOnEvents[] values = values();
		for (StorageWindowOnEvents e : values) {
			if (e.type == windowEventParameters.getWindowEvent().getType()) {
				return e.onEvent.execute(windowEventParameters, assetsManager, storage);
			}
		}
		return false;
	}
}
