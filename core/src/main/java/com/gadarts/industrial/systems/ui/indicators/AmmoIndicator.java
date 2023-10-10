package com.gadarts.industrial.systems.ui.indicators;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
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
	private static final String LABEL_RELOAD = "RELOAD";
	public static final float RELOAD_FLICKER_DURATION = 1F;
	private final Label ammoLabel;
	private final Label reloadLabel;
	private final Image ammoTypeImage = new Image();
	private final Map<PlayerWeaponDeclaration, Drawable> ammoTypeDrawables = new HashMap<>();

	public AmmoIndicator(ButtonStyle buttonStyle,
						 BitmapFont font,
						 GameAssetManager assetsManager,
						 NoiseEffectHandler noiseEffectHandler) {
		super(buttonStyle, noiseEffectHandler);
		Label.LabelStyle style = new Label.LabelStyle(font, new Color(FONT_COLOR_GOOD));
		var labelsStack = new Stack();
		ammoLabel = addLabel("-", style, labelsStack);
		reloadLabel = addLabel(LABEL_RELOAD, style, labelsStack);
		reloadLabel.setVisible(false);
		add(labelsStack).pad(0F, 0F, 0F, 0F);
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

	private Label addLabel(String text, Label.LabelStyle style, Stack labelsStack) {
		final Label label;
		label = new Label(text, style);
		label.setAlignment(Align.center);
		labelsStack.add(label);
		return label;
	}


	public void setValues(WeaponAmmo weaponAmmo) {
		int loaded = weaponAmmo.getLoaded();
		this.ammoLabel.setText(String.format(FORMAT, loaded, weaponAmmo.getTotal()));
		float badThreshold = weaponAmmo.getPlayerWeaponDeclaration().magazineSize() * LOW_VALUE_THRESHOLD;
		ammoLabel.getStyle().fontColor.set(loaded <= badThreshold ? FONT_COLOR_BAD : FONT_COLOR_GOOD);
		ammoTypeImage.setDrawable(ammoTypeDrawables.get(weaponAmmo.getPlayerWeaponDeclaration()));
		boolean hasActions = ammoLabel.hasActions();
		if (loaded <= 0 && !hasActions) {
			ammoLabel.addAction(Actions.forever(Actions.sequence(
					Actions.delay(RELOAD_FLICKER_DURATION, Actions.visible(false)),
					Actions.delay(RELOAD_FLICKER_DURATION, Actions.visible(true)))));
			reloadLabel.addAction(Actions.forever(Actions.sequence(
					Actions.delay(RELOAD_FLICKER_DURATION, Actions.visible(true)),
					Actions.delay(RELOAD_FLICKER_DURATION, Actions.visible(false)))));
		} else if (hasActions) {
			ammoLabel.clearActions();
			reloadLabel.clearActions();
			ammoLabel.setVisible(true);
			reloadLabel.setVisible(false);
		}
	}
}
