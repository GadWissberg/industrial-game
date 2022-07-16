package com.gadarts.industrial.systems.character.actions;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterCommandContext;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;

import java.util.List;

public class OpenDoorCharacterCommand implements CharacterCommandImplementation {

	@Override
	public void initialize(Entity character,
						   SystemsCommonData commonData,
						   Object additionalData,
						   List<CharacterSystemEventsSubscriber> subscribers) {
//		path = additionalData
//		ComponentsMapper.door.get(pathFinalNode.getDoor()).setState(DoorComponent.DoorStates.OPENING);
//		subscribers.forEach(s -> s.onCharacterOpenedDoor(pathFinalNode));
	}

	@Override
	public boolean reactToFrameChange(SystemsCommonData systemsCommonData,
									  Entity character,
									  TextureAtlas.AtlasRegion newFrame,
									  List<CharacterSystemEventsSubscriber> subscribers, CharacterCommandContext currentCommand) {
		return false;
	}
}
