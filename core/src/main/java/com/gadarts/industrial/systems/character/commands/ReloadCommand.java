package com.gadarts.industrial.systems.character.commands;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.player.Weapon;
import com.gadarts.industrial.components.player.WeaponAmmo;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.declarations.pickups.weapons.PlayerWeaponDeclaration;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;

import java.util.List;

import static com.gadarts.industrial.components.ComponentsMapper.player;

public class ReloadCommand extends CharacterCommand {

	private static final int POINTS_CONSUME = 2;
	public static final int FRAME_TO_APPLY_RESULT = 6;

	@Override
	public boolean initialize(Entity character,
							  SystemsCommonData commonData) {
		Weapon weapon = commonData.getStorage().getSelectedWeapon();
		PlayerWeaponDeclaration selectedWeapon = (PlayerWeaponDeclaration) weapon.getDeclaration();
		WeaponAmmo weaponAmmo = player.get(character).getAmmo().get(selectedWeapon);
		int actionPoints = ComponentsMapper.character.get(character).getAttributes().getActionPoints();
		boolean cancelCommand = actionPoints < POINTS_CONSUME
				|| weaponAmmo.getLoaded() >= selectedWeapon.magazineSize()
				|| weaponAmmo.getTotal() <= 0;
		if (!cancelCommand) {
			commonData.getSoundPlayer().playSound(Assets.Sounds.WEAPON_GLOCK_RELOAD);
		}
		return cancelCommand;
	}

	@Override
	public boolean reactToFrameChange(SystemsCommonData systemsCommonData,
									  Entity character,
									  TextureAtlas.AtlasRegion newFrame,
									  List<CharacterSystemEventsSubscriber> subscribers) {
		if (newFrame.index == FRAME_TO_APPLY_RESULT) {
			consumeActionPoints(character, POINTS_CONSUME, subscribers);
			Weapon weapon = systemsCommonData.getStorage().getSelectedWeapon();
			PlayerWeaponDeclaration selectedWeapon = (PlayerWeaponDeclaration) weapon.getDeclaration();
			WeaponAmmo weaponAmmo = player.get(character).getAmmo().get(selectedWeapon);
			int loadedIntoGun = Math.min(selectedWeapon.magazineSize() - weaponAmmo.getLoaded(), weaponAmmo.getTotal());
			weaponAmmo.setLoaded(weaponAmmo.getLoaded() + loadedIntoGun);
			weaponAmmo.setTotal(weaponAmmo.getTotal() - loadedIntoGun);
			for (CharacterSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onCharacterReload(character, weaponAmmo);
			}
		}
		return false;
	}

	@Override
	public void reset( ) {

	}
}
