package com.gadarts.industrial.systems.character.commands;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Pool;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.character.CharacterAttributes;
import com.gadarts.industrial.map.MapGraphPath;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public abstract class CharacterCommand implements Pool.Poolable {
	final MapGraphPath path = new MapGraphPath();
	private CharacterCommandsDefinitions definition;
	private Entity character;
	@Setter
	private CommandStates state;
	@Setter
	private int nextNodeIndex = -1;

	protected void consumeActionPoints(Entity character,
									   int pointsConsumption,
									   List<CharacterSystemEventsSubscriber> subscribers) {
		CharacterAttributes skills = ComponentsMapper.character.get(character).getAttributes();
		int newValue = skills.getActionPoints() - pointsConsumption;
		skills.setActionPoints(newValue);
		subscribers.forEach(sub -> sub.onCharacterConsumedActionPoint(character, newValue));
	}

	@Override
	public void reset( ) {
		state = CommandStates.READY;
		definition = null;
		character = null;
		nextNodeIndex = -1;
	}

	public abstract boolean initialize(Entity character,
									   SystemsCommonData commonData);

	public abstract boolean update(SystemsCommonData systemsCommonData,
								   Entity character,
								   TextureAtlas.AtlasRegion newFrame,
								   List<CharacterSystemEventsSubscriber> subscribers);

	public CharacterCommand reset(CharacterCommandsDefinitions type,
								  Entity character,
								  MapGraphPath outputPath) {
		this.definition = type;
		this.character = character;
		this.path.set(outputPath);
		this.nextNodeIndex = 0;
		this.state = CommandStates.READY;
		return this;
	}

	@Override
	public String toString( ) {
		return definition.name();
	}

}
