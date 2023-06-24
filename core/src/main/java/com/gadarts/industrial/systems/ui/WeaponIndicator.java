package com.gadarts.industrial.systems.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.assets.declarations.weapons.PlayerWeaponDeclaration;
import com.gadarts.industrial.shared.assets.declarations.weapons.PlayerWeaponsDeclarations;

import java.util.Map;
import java.util.stream.Collectors;

public class WeaponIndicator extends HudIndicator {
	private final Image icon;
	private final Map<String, Drawable> iconsDrawables;


	public WeaponIndicator(Texture borderTexture, PlayerWeaponsDeclarations declarations, GameAssetManager assetsManager) {
		super(borderTexture);
		iconsDrawables = declarations.playerWeaponsDeclarations()
				.stream()
				.collect(Collectors.toMap(
						PlayerWeaponDeclaration::id,
						dec -> new TextureRegionDrawable(assetsManager.getTexture(dec.hudIcon()))));
		icon = new com.badlogic.gdx.scenes.scene2d.ui.Image(iconsDrawables.get("pnc"));
		add(icon);
	}

	public void setIcon(PlayerWeaponDeclaration playerWeaponDeclaration) {
		this.icon.setDrawable(iconsDrawables.get(playerWeaponDeclaration.id()));
	}
}
