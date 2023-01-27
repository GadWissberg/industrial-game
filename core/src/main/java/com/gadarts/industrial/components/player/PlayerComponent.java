package com.gadarts.industrial.components.player;

import com.gadarts.industrial.components.GameComponent;
import com.gadarts.industrial.components.character.CharacterAnimations;
import lombok.Getter;
import lombok.Setter;

@Getter
public class PlayerComponent implements GameComponent {
	public static final float PLAYER_HEIGHT = 1;
	public static final float PLAYER_AGILITY = 1F;
	private CharacterAnimations generalAnimations;

	@Setter
	private boolean disabled;

	@Override
	public void reset( ) {
		
	}

	public void init(final CharacterAnimations general) {
		this.generalAnimations = general;
	}
}
