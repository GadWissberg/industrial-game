package com.gadarts.industrial.systems.character;

import com.gadarts.industrial.systems.character.actions.OpenDoorAction;
import com.gadarts.industrial.systems.character.actions.PrimaryAttackAction;
import com.gadarts.industrial.systems.character.actions.ToDoAfterDestinationReached;
import lombok.Getter;

public enum CharacterCommandsTypes {
	GO_TO(),
	GO_TO_OPEN_DOOR(new OpenDoorAction()),
	GO_TO_PICKUP(new PickUpAction()),
	ATTACK_PRIMARY(new PrimaryAttackAction(), false);

	private final ToDoAfterDestinationReached toDoAfterDestinationReached;

	@Getter
	private final boolean requiresMovement;


	CharacterCommandsTypes(final ToDoAfterDestinationReached toDoAfterDestinationReached) {
		this(toDoAfterDestinationReached, true);
	}

	CharacterCommandsTypes(ToDoAfterDestinationReached toDoAfterDestinationReached,
						   boolean requiresMovement) {
		this.toDoAfterDestinationReached = toDoAfterDestinationReached;
		this.requiresMovement = requiresMovement;
	}

	CharacterCommandsTypes( ) {
		this(null, true);
	}

	public ToDoAfterDestinationReached getToDoAfterDestinationReached( ) {
		return toDoAfterDestinationReached;
	}
}
