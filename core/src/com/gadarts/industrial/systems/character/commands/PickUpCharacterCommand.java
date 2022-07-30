package com.gadarts.industrial.systems.character.commands;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;

import java.util.List;

public class PickUpCharacterCommand extends CharacterCommand {

	private final static Vector2 auxVector2 = new Vector2();

	@Override
	public void initialize(Entity character,
						   SystemsCommonData commonData,
						   Object additionalData,
						   List<CharacterSystemEventsSubscriber> subscribers) {
		Vector2 charPos = ComponentsMapper.characterDecal.get(character).getNodePosition(auxVector2);
		MapGraph map = commonData.getMap();
		Entity pickup = map.getPickupFromNode(map.getNode(charPos));
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		if (pickup != null) {
			characterComponent.getRotationData().setRotating(true);
		}
	}

	@Override
	public boolean reactToFrameChange(SystemsCommonData systemsCommonData,
									  Entity character,
									  TextureAtlas.AtlasRegion newFrame,
									  List<CharacterSystemEventsSubscriber> subscribers) {
		return false;
	}
}
