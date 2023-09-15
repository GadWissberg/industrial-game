package com.gadarts.industrial.components.player;

import com.gadarts.industrial.shared.assets.declarations.pickups.weapons.PlayerWeaponDeclaration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class WeaponAmmo {
	private int loaded;
	private int total;
	private PlayerWeaponDeclaration playerWeaponDeclaration;

}
