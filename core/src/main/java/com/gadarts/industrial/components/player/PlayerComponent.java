package com.gadarts.industrial.components.player;

import com.gadarts.industrial.components.GameComponent;
import com.gadarts.industrial.components.character.CharacterAnimations;
import com.gadarts.industrial.shared.assets.declarations.Agility;
import lombok.Getter;
import lombok.Setter;

@Getter
public class PlayerComponent implements GameComponent {
	public static final float PLAYER_HEIGHT = 1;
	public static final Agility PLAYER_AGILITY = new Agility(4, 5);
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
