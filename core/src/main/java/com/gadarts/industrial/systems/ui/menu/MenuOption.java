package com.gadarts.industrial.systems.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.shared.assets.Assets;

public class MenuOption extends Label {
	static final Color FONT_COLOR_REGULAR = Color.RED;
	private static final Color FONT_COLOR_HOVER = Color.YELLOW;

	public MenuOption(MenuOptionDefinition definition,
					  LabelStyle optionStyle,
					  SoundPlayer soundPlayer,
					  MenuHandler menuHandler) {
		super(definition.getLabel(), new LabelStyle(optionStyle));
		addListener(createClickListener(definition, menuHandler, soundPlayer));
	}

	private EventListener createClickListener(MenuOptionDefinition definition, MenuHandler menuHandler, SoundPlayer soundPlayer) {
		return new ClickListener() {
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
				if (definition.getAction() != null) {
					definition.getAction().run(menuHandler);
				} else {
					MenuOptionDefinition[] subOptions = definition.getSubOptions();
					if (subOptions != null) {
						menuHandler.applyMenuOptions(subOptions, true);
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
		};
	}

}
