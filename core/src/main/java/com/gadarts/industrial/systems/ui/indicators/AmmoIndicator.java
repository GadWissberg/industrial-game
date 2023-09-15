package com.gadarts.industrial.systems.ui.indicators;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.gadarts.industrial.components.player.WeaponAmmo;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.assets.declarations.pickups.weapons.PlayerWeaponDeclaration;
import com.gadarts.industrial.shared.assets.declarations.pickups.weapons.PlayerWeaponsDeclarations;
import com.gadarts.industrial.shared.model.pickups.AmmoTypes;
import com.gadarts.industrial.systems.ui.NoiseEffectHandler;

import java.util.HashMap;
import java.util.Map;

import static com.gadarts.industrial.shared.assets.Assets.Declarations.PLAYER_WEAPONS;

public class AmmoIndicator extends HudIndicator {
	public static final float LOW_VALUE_THRESHOLD = 0.2F;
	private static final String FORMAT = "%s/%s";
	private final Label label;
	private final Image ammoTypeImage = new Image();
	private final Map<PlayerWeaponDeclaration, Drawable> ammoTypeDrawables = new HashMap<>();

	public AmmoIndicator(ButtonStyle buttonStyle,
						 BitmapFont font,
						 GameAssetManager assetsManager,
						 NoiseEffectHandler noiseEffectHandler) {
		super(buttonStyle, noiseEffectHandler);
		Label.LabelStyle style = new Label.LabelStyle(font, new Color(FONT_COLOR_GOOD));
		this.label = new Label("-", style);
		add(label).pad(0F, 0F, 0F, 20F);
		add(ammoTypeImage);
		PlayerWeaponsDeclarations decs = (PlayerWeaponsDeclarations) assetsManager.getDeclaration(PLAYER_WEAPONS);
		decs.playerWeaponsDeclarations().forEach(dec -> {
			AmmoTypes ammoType = dec.ammoType();
			if (ammoType != null) {
				TextureRegionDrawable drawable = new TextureRegionDrawable(assetsManager.getTexture(ammoType.getHudIcon()));
				ammoTypeDrawables.put(dec, drawable);
			}
		});
	}


	public void setValues(WeaponAmmo weaponAmmo) {
		this.label.setText(String.format(FORMAT, weaponAmmo.getLoaded(), weaponAmmo.getTotal()));
		float badThreshold = weaponAmmo.getPlayerWeaponDeclaration().magazineSize() * LOW_VALUE_THRESHOLD;
		label.getStyle().fontColor.set(weaponAmmo.getLoaded() <= badThreshold ? FONT_COLOR_BAD : FONT_COLOR_GOOD);
		ammoTypeImage.setDrawable(ammoTypeDrawables.get(weaponAmmo.getPlayerWeaponDeclaration()));
	}
}
