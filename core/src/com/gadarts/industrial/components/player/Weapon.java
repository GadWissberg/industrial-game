package com.gadarts.industrial.components.player;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gadarts.industrial.shared.model.pickups.WeaponsDefinitions;
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
		WeaponsDefinitions definition = (WeaponsDefinitions) getDefinition();
		return definition.isMelee();
	}

	public boolean isHitScan( ) {
		WeaponsDefinitions definition = (WeaponsDefinitions) getDefinition();
		return definition.isHitScan();
	}
}
