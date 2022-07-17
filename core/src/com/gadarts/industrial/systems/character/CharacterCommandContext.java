package com.gadarts.industrial.systems.character;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.map.MapGraphNode;
import lombok.Getter;
import lombok.Setter;

@Getter
public class CharacterCommandContext {
	private CharacterCommandsDefinitions definition;
	private Entity character;
	private Object additionalData;

	@Setter
	private MapGraphNode destinationNode;
	@Setter
	private boolean started;

	public CharacterCommandContext init(CharacterCommandsDefinitions type,
										Entity character,
										Object additionalData,
										MapGraphNode destinationNode) {
		this.definition = type;
		this.character = character;
		this.additionalData = additionalData;
		this.destinationNode = destinationNode;
		return this;
	}

	public CharacterCommandContext init(CharacterCommandContext command) {
		return init(command.definition, command.character, command.additionalData, command.destinationNode);
	}
}
