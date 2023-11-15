package com.gadarts.industrial.systems.ui.menu;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.screens.GameLifeCycleManager;
import com.gadarts.industrial.shared.assets.Assets;

import static com.gadarts.industrial.systems.ui.menu.MenuHandler.FONT_COLOR_HOVER;
import static com.gadarts.industrial.systems.ui.menu.MenuHandler.FONT_COLOR_REGULAR;

public class MenuOption extends Label {

	public MenuOption(MenuOptionDefinition definition,
					  LabelStyle optionStyle,
					  SoundPlayer soundPlayer,
					  MenuHandler menuHandler,
					  GameLifeCycleManager gameLifeCycleManager) {
		super(definition.getLabel(), new LabelStyle(optionStyle));
		addListener(createClickListener(definition, menuHandler, soundPlayer, gameLifeCycleManager));
	}

	private EventListener createClickListener(MenuOptionDefinition definition,
											  MenuHandler menuHandler,
											  SoundPlayer soundPlayer,
											  GameLifeCycleManager gameLifeCycleManager) {
		return new ClickListener() {
			@Override
			public void enter(final InputEvent event,
							  final float x,
							  final float y,
							  final int pointer,
							  final Actor fromActor) {
				super.enter(event, x, y, pointer, fromActor);
				getStyle().fontColor = FONT_COLOR_HOVER;
				soundPlayer.playSound(Assets.Sounds.MENU_HOVER);
			}

			@Override
			public void clicked(final InputEvent event, final float x, final float y) {
				super.clicked(event, x, y);
				if (definition.getAction() != null) {
					definition.getAction().run(menuHandler, gameLifeCycleManager);
				} else {
					MenuOptionDefinition[] subOptions = definition.getSubOptions();
					if (subOptions != null) {
						menuHandler.applyMenuOptions(subOptions, true);
					}
				}
				soundPlayer.playSound(Assets.Sounds.MENU_CLICK);
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
