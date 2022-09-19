package com.gadarts.industrial.systems.character.commands;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;

import java.util.List;

public class PickupItemCharacterCommand extends CharacterCommand {

	private static final float PICKUP_CONSUME_TIME = 1F;
	private Entity itemToPickup;

	@Override
	public boolean initialize(Entity character,
							  SystemsCommonData commonData,
							  Object additionalData,
							  List<CharacterSystemEventsSubscriber> subscribers) {
		itemToPickup = (Entity) additionalData;
		return false;
	}

	@Override
	public boolean reactToFrameChange(SystemsCommonData systemsCommonData,
									  Entity character,
									  TextureAtlas.AtlasRegion newFrame,
									  List<CharacterSystemEventsSubscriber> subscribers) {
		handlePickup(character, newFrame, subscribers);
		return false;
	}

	@Override
	public void free( ) {

	}

	private void handlePickup(Entity character,
							  TextureAtlas.AtlasRegion newFrame,
							  List<CharacterSystemEventsSubscriber> subscribers) {
		if (newFrame.index == 1 && ComponentsMapper.animation.get(character).getAnimation().getPlayMode() == Animation.PlayMode.REVERSED) {
			consumeTurnTime(character, PICKUP_CONSUME_TIME);
			for (CharacterSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onItemPickedUp(itemToPickup);
			}
		}
	}

	@Override
	public void reset( ) {

	}
}
