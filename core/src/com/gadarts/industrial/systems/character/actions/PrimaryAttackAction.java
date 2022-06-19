package com.gadarts.industrial.systems.character.actions;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.character.CharacterMotivation;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;

import java.util.List;

public class PrimaryAttackAction implements ToDoAfterDestinationReached {

	@Override
	public void run(final Entity character,
					final MapGraph map,
					final SoundPlayer soundPlayer,
					final Object itemToPickup, MapGraphNode pathFinalNode, List<CharacterSystemEventsSubscriber> subscribers) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		Entity target = characterComponent.getTarget();
		if (target != null) {
			characterComponent.getRotationData().setRotating(true);
			characterComponent.setMotivation(CharacterMotivation.TO_ATTACK, CharacterMotivation.USE_PRIMARY);
		}
	}
}
