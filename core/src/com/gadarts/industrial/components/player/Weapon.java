package com.gadarts.industrial.components.player;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gadarts.industrial.shared.model.pickups.PlayerWeaponsDefinitions;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Weapon extends Item {
	private TextureRegion bulletTextureRegion;

	@Override
	public boolean isWeapon( ) {
		return true;
	}

	public boolean isMelee( ) {
		PlayerWeaponsDefinitions definition = (PlayerWeaponsDefinitions) getDefinition();
		return definition.isMelee();
	}
}
