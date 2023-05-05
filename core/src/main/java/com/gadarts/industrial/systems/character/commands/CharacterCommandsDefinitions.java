package com.gadarts.industrial.systems.character.commands;

import com.gadarts.industrial.shared.model.characters.SpriteType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum CharacterCommandsDefinitions {
	RUN(RunCharacterCommand.class, SpriteType.RUN),
	DODGE(RunCharacterCommand.class, SpriteType.RUN, true),
	PICKUP(PickupItemCharacterCommand.class, SpriteType.PICKUP, false, false),
	ATTACK_PRIMARY(AttackPrimaryCharacterCommand.class, SpriteType.ATTACK_PRIMARY);

	private final Class<? extends CharacterCommand> characterCommandImplementation;
	private final SpriteType spriteType;
	private final boolean rotationForbidden;
	private final boolean requiresMovement;

	CharacterCommandsDefinitions(Class<? extends CharacterCommand> commandClass,
								 SpriteType spriteType,
								 boolean rotationForbidden) {
		this(commandClass, spriteType, rotationForbidden, true);
	}

	CharacterCommandsDefinitions(Class<? extends CharacterCommand> commandClass, SpriteType spriteType) {
		this(commandClass, spriteType, false, true);
	}
}
