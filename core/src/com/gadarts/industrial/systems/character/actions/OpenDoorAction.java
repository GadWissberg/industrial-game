package com.gadarts.industrial.systems.character.actions;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;

import java.util.List;

public class OpenDoorAction implements ToDoAfterDestinationReached {
	@Override
	public void run(Entity character,
					MapGraph map,
					SoundPlayer soundPlayer,
					Object additionalData,
					MapGraphNode pathFinalNode,
					List<CharacterSystemEventsSubscriber> subscribers) {
		ComponentsMapper.door.get(pathFinalNode.getDoor()).setState(DoorComponent.DoorStates.OPENING);
		subscribers.forEach(s -> s.onCharacterOpenedDoor(pathFinalNode));
	}
}
