package com.gadarts.industrial.systems.character.commands;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

public class PickupItemCommand extends CharacterCommand {

	private static final List<Entity> auxEntityList = new ArrayList<>();
	private static final int PICKUP_ACTION_POINT_CONSUME = 1;

	@Override
	public boolean update(SystemsCommonData systemsCommonData,
						  Entity character,
						  TextureAtlas.AtlasRegion newFrame,
						  List<CharacterSystemEventsSubscriber> subscribers) {
		if (ComponentsMapper.animation.get(character).getAnimation().getPlayMode() != PlayMode.REVERSED) return false;

		if (newFrame.index == 1) {
			MapGraph map = systemsCommonData.getMap();
			val characterNode = map.getNode(ComponentsMapper.characterDecal.get(character).getDecal().getPosition());
			List<Entity> pickups = map.fetchPickupsFromNode(characterNode, auxEntityList);
			for (CharacterSystemEventsSubscriber subscriber : subscribers) {
				Entity itemPickedUp = pickups.get(0);
				subscriber.onItemPickedUp(itemPickedUp);
				consumeActionPoints(character, PICKUP_ACTION_POINT_CONSUME, subscribers);
			}
		}
		return false;
	}

	@Override
	public void reset( ) {

	}

	@Override
	public boolean initialize(Entity character,
							  SystemsCommonData commonData) {
		if (ComponentsMapper.character.get(character).getAttributes().getActionPoints() < PICKUP_ACTION_POINT_CONSUME)
			return true;

		Vector3 position = ComponentsMapper.characterDecal.get(character).getDecal().getPosition();
		return !commonData.getMap().getNode(position).equals(path.nodes.get(path.getCount() - 1));
	}
}
