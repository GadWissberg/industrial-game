package com.gadarts.industrial.systems.character.commands;

import com.gadarts.industrial.shared.model.characters.SpriteType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum CharacterCommandsDefinitions {
	RUN(RunCharacterCommand.class, SpriteType.RUN),
	OPEN_DOOR(OpenDoorCharacterCommand.class, SpriteType.IDLE),
	PICKUP(PickUpCharacterCommand.class, SpriteType.PICKUP),
	ATTACK_PRIMARY(PrimaryAttackCharacterCommand.class, SpriteType.ATTACK_PRIMARY);

	private final Class<? extends CharacterCommand> characterCommandImplementation;
	private final SpriteType spriteType;


}
