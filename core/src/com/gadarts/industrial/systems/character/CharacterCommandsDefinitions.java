package com.gadarts.industrial.systems.character;

import com.gadarts.industrial.shared.model.characters.SpriteType;
import com.gadarts.industrial.systems.character.actions.CharacterCommandImplementation;
import com.gadarts.industrial.systems.character.actions.RunCharacterCommand;
import com.gadarts.industrial.systems.character.actions.OpenDoorCharacterCommand;
import com.gadarts.industrial.systems.character.actions.PrimaryAttackCharacterCommand;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum CharacterCommandsDefinitions {
	RUN(new RunCharacterCommand(), SpriteType.RUN),
	OPEN_DOOR(new OpenDoorCharacterCommand(), SpriteType.IDLE),
	PICKUP(new PickUpCharacterCommand(), SpriteType.PICKUP),
	ATTACK_PRIMARY(new PrimaryAttackCharacterCommand(), SpriteType.ATTACK_PRIMARY);

	private final CharacterCommandImplementation characterCommandImplementation;
	private final SpriteType spriteType;


}
