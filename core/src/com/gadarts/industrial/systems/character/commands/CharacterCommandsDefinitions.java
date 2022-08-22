package com.gadarts.industrial.systems.character.commands;

import com.gadarts.industrial.shared.model.characters.SpriteType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum CharacterCommandsDefinitions {
	RUN(RunCharacterCommand.class, SpriteType.RUN),
	DODGE(RunCharacterCommand.class, SpriteType.RUN, true),
	OPEN_DOOR(OpenDoorCharacterCommand.class, SpriteType.IDLE),
	PICKUP(PickUpCharacterCommand.class, SpriteType.PICKUP),
	ATTACK_PRIMARY(PrimaryAttackCharacterCommand.class, SpriteType.ATTACK_PRIMARY);

	private final Class<? extends CharacterCommand> characterCommandImplementation;
	private final SpriteType spriteType;
	private final boolean rotationForbidden;

	CharacterCommandsDefinitions(Class<? extends CharacterCommand> commandClass, SpriteType spriteType) {
		this(commandClass, spriteType, false);
	}
}
