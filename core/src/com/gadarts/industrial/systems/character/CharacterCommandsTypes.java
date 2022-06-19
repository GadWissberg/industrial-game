package com.gadarts.industrial.systems.character;

import com.gadarts.industrial.systems.character.actions.MeleeAction;
import com.gadarts.industrial.systems.character.actions.OpenDoorAction;
import com.gadarts.industrial.systems.character.actions.PrimaryAttackAction;
import com.gadarts.industrial.systems.character.actions.ToDoAfterDestinationReached;
import lombok.Getter;

public enum CharacterCommandsTypes {
	GO_TO,
	GO_TO_OPEN_DOOR(new OpenDoorAction()),
	GO_TO_PICKUP(new PickUpAction()),
	GO_TO_MELEE(new MeleeAction()),
	ATTACK_PRIMARY(new PrimaryAttackAction(), false);

	private final ToDoAfterDestinationReached toDoAfterDestinationReached;

	@Getter
	private final boolean requiresMovement;

	CharacterCommandsTypes( ) {
		this(null);
	}

	CharacterCommandsTypes(final ToDoAfterDestinationReached toDoAfterDestinationReached) {
		this(toDoAfterDestinationReached, true);
	}

	CharacterCommandsTypes(final ToDoAfterDestinationReached toDoAfterDestinationReached, final boolean requiresMovement) {
		this.toDoAfterDestinationReached = toDoAfterDestinationReached;
		this.requiresMovement = requiresMovement;
	}

	public ToDoAfterDestinationReached getToDoAfterDestinationReached( ) {
		return toDoAfterDestinationReached;
	}
}
