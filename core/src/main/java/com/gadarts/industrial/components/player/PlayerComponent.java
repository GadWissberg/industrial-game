package com.gadarts.industrial.components.player;

import com.gadarts.industrial.components.GameComponent;
import com.gadarts.industrial.components.character.CharacterAnimations;
import com.gadarts.industrial.shared.assets.declarations.Agility;
import com.gadarts.industrial.shared.assets.declarations.pickups.weapons.PlayerWeaponDeclaration;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class PlayerComponent implements GameComponent {
	public static final float PLAYER_HEIGHT = 1;
	public static final Agility PLAYER_AGILITY = new Agility(4, 5);
	private CharacterAnimations generalAnimations;
	private final Map<PlayerWeaponDeclaration, WeaponAmmo> ammo = new HashMap<>();

	@Override
	public void reset( ) {
	}

	public void init(CharacterAnimations general) {
		this.generalAnimations = general;
		ammo.clear();
	}
}
