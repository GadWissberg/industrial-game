package com.gadarts.industrial.systems.ui.menu;

import com.gadarts.industrial.screens.GameLifeCycleManager;

public interface MenuOptionAction {
	void run(MenuHandler menuHandler, GameLifeCycleManager gameLifeCycleManager);
}
