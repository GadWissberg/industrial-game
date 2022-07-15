package com.gadarts.industrial.systems.character;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.systems.character.actions.ToDoAfterDestinationReached;

import java.util.List;

public class PickUpAction implements ToDoAfterDestinationReached {

	private final static Vector2 auxVector2 = new Vector2();

	@Override
	public void run(final Entity character,
					final MapGraph map,
					final SoundPlayer soundPlayer,
					final Object itemToPickup, MapGraphNode pathFinalNode, List<CharacterSystemEventsSubscriber> subscribers) {
		Vector2 charPos = ComponentsMapper.characterDecal.get(character).getNodePosition(auxVector2);
		Entity pickup = map.getPickupFromNode(map.getNode(charPos));
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		if (pickup != null) {
			characterComponent.getRotationData().setRotating(true);
		}
	}
}
