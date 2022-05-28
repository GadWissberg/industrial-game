package com.gadarts.industrial.systems.character.actions;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.map.MapGraph;

public interface ToDoAfterDestinationReached {
	void run(Entity character, MapGraph map, SoundPlayer soundPlayer, Object additionalData);
}
