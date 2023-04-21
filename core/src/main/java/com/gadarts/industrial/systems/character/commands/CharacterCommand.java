package com.gadarts.industrial.systems.character.commands;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Pool;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.map.MapGraphPath;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public abstract class CharacterCommand implements Pool.Poolable {
	private CharacterCommandsDefinitions definition;
	private Entity character;
	@Setter
	private MapGraphNode destinationNode;
	@Setter
	private CommandStates state;
	@Setter
	private MapGraphNode nextNode;
	final MapGraphPath path = new MapGraphPath();

	@Override
	public void reset( ) {
		destinationNode = null;
		state = CommandStates.READY;
		definition = null;
		character = null;
		nextNode = null;
	}

	public abstract void initialize(Entity character,
									SystemsCommonData commonData,
									List<CharacterSystemEventsSubscriber> subscribers);

	public abstract boolean reactToFrameChange(SystemsCommonData systemsCommonData,
											   Entity character,
											   TextureAtlas.AtlasRegion newFrame,
											   List<CharacterSystemEventsSubscriber> subscribers);

	public CharacterCommand reset(CharacterCommandsDefinitions type,
								  Entity character,
								  MapGraphPath outputPath,
								  MapGraphNode destinationNode) {
		this.definition = type;
		this.character = character;
		this.path.set(outputPath);
		this.destinationNode = destinationNode;
		this.nextNode = destinationNode;
		this.state = CommandStates.READY;
		return this;
	}

	public void onInFight( ) {

	}

	@Override
	public String toString( ) {
		return definition.name();
	}

	void consumeTurnTime(Entity character, float consume) {
		CharacterComponent characterComp = ComponentsMapper.character.get(character);
		characterComp.setTurnTimeLeft(characterComp.getTurnTimeLeft() - consume);
	}
}
