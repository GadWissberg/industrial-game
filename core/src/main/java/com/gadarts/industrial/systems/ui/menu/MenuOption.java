package com.gadarts.industrial.systems.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.systems.ui.UserInterfaceSystemEventsSubscriber;

import java.util.List;

public class MenuOption extends Label {
	static final Color FONT_COLOR_REGULAR = Color.RED;
	private static final Color FONT_COLOR_HOVER = Color.YELLOW;

	public MenuOption(MenuOptionDefinition option,
					  LabelStyle optionStyle,
					  SoundPlayer soundPlayer,
					  MenuHandler menuHandler,
					  List<UserInterfaceSystemEventsSubscriber> subscribers) {
		super(option.getLabel(), new LabelStyle(optionStyle));
		addListener(new ClickListener() {
			@Override
			public void enter(final InputEvent event,
							  final float x,
							  final float y,
							  final int pointer,
							  final Actor fromActor) {
				super.enter(event, x, y, pointer, fromActor);
				getStyle().fontColor = FONT_COLOR_HOVER;
			}

			@Override
			public void clicked(final InputEvent event, final float x, final float y) {
				super.clicked(event, x, y);
				if (option.getAction() != null) {
					option.getAction().run(menuHandler, subscribers);
				} else {
					MenuOptionDefinition[] subOptions = option.getSubOptions();
					if (subOptions != null) {
						menuHandler.applyMenuOptions(subOptions);
					}
				}
				soundPlayer.playSound(Assets.Sounds.UI_CLICK);
			}

			@Override
			public void exit(final InputEvent event,
							 final float x,
							 final float y,
							 final int pointer,
							 final Actor toActor) {
				super.exit(event, x, y, pointer, toActor);
				getStyle().fontColor = FONT_COLOR_REGULAR;
			}
		});
	}
}
