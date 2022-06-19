package com.gadarts.industrial.systems.character;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.map.MapGraphPath;
import lombok.Getter;
import lombok.Setter;

@Getter
public class CharacterCommand {
	private CharacterCommandsTypes type;
	private Entity character;
	private Object additionalData;

	@Setter
	private boolean started;

	private final MapGraphPath path = new MapGraphPath();

	public CharacterCommand init(final CharacterCommandsTypes type,
								 final MapGraphPath path,
								 final Entity character) {
		return init(type, path, character, null);
	}

	public CharacterCommand init(final CharacterCommandsTypes type,
								 final MapGraphPath path,
								 final Entity character,
								 final Object additionalData) {
		this.type = type;
		this.path.set(path);
		this.character = character;
		this.additionalData = additionalData;
		return this;
	}

	public CharacterCommand init(CharacterCommand command) {
		return init(command.type, command.getPath(), command.character, command.additionalData);
	}
}
