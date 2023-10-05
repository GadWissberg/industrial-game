package com.gadarts.industrial.systems.character.commands;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;

import java.util.List;

public class ReloadCommand extends CharacterCommand {

	private static final int POINTS_CONSUME = 2;
	public static final int FRAME_TO_APPLY_RESULT = 6;

	@Override
	public boolean initialize(Entity character,
							  SystemsCommonData commonData) {
		commonData.getSoundPlayer().playSound(Assets.Sounds.WEAPON_GLOCK_RELOAD);
		return ComponentsMapper.character.get(character).getAttributes().getActionPoints() < POINTS_CONSUME;
	}

	@Override
	public boolean reactToFrameChange(SystemsCommonData systemsCommonData,
									  Entity character,
									  TextureAtlas.AtlasRegion newFrame,
									  List<CharacterSystemEventsSubscriber> subscribers) {
		if (newFrame.index == FRAME_TO_APPLY_RESULT) {
			consumeActionPoints(character, POINTS_CONSUME, subscribers);
			for (CharacterSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onCharacterReload(character);
			}
		}
		return false;
	}

	@Override
	public void reset( ) {

	}
}
