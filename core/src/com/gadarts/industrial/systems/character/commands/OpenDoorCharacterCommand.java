package com.gadarts.industrial.systems.character.commands;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;

import java.util.List;

public class OpenDoorCharacterCommand extends CharacterCommand {

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
									  List<CharacterSystemEventsSubscriber> subscribers) {
		return false;
	}

}
