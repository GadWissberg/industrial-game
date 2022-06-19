package com.gadarts.industrial.systems.character.actions;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;

import java.util.List;

public interface ToDoAfterDestinationReached {
	void run(Entity character,
			 MapGraph map,
			 SoundPlayer soundPlayer,
			 Object additionalData,
			 MapGraphNode pathFinalNode,
			 List<CharacterSystemEventsSubscriber> subscribers);
}
