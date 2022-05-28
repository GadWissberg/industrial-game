package com.gadarts.industrial.systems.ui.storage;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.gadarts.industrial.systems.ui.window.GameWindowEvent;
import com.gadarts.industrial.systems.ui.window.GameWindowEventType;

public abstract class ItemsTable extends Table {
	protected final ItemSelectionHandler itemSelectionHandler;

	public ItemsTable(final ItemSelectionHandler itemSelectionHandler) {
		this.itemSelectionHandler = itemSelectionHandler;
	}

	protected void onRightClick() {
		fire(new GameWindowEvent(this, GameWindowEventType.CLICK_RIGHT));
	}

	public abstract void removeItem(ItemDisplay item);
}
