package com.gadarts.industrial.systems.ui.menu;

import com.gadarts.industrial.systems.ui.UserInterfaceSystemEventsSubscriber;

import java.util.List;

public interface MenuOptionAction {
	void run(MenuHandler menuHandler, List<UserInterfaceSystemEventsSubscriber> uiSystemEventsSubscribers);
}
