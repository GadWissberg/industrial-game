package com.gadarts.industrial.systems.ui.window;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.ui.storage.ItemSelectionHandler;
import com.gadarts.industrial.systems.ui.UserInterfaceSystemEventsSubscriber;
import lombok.Data;

import java.util.List;

@Data
public class WindowEventParameters {
	private GameWindowEvent windowEvent;
	private SoundPlayer soundPlayer;
	private ItemSelectionHandler selectedItem;
	private Table target;
	private SystemsCommonData systemsCommonData;
	private List<UserInterfaceSystemEventsSubscriber> subscribers;
}
