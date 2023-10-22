package com.gadarts.industrial.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.systems.player.PlayerStorage;
import com.gadarts.industrial.systems.render.flags.DrawFlags;
import com.gadarts.industrial.systems.turns.GameMode;
import com.gadarts.industrial.systems.ui.*;
import com.gadarts.industrial.systems.ui.indicators.AmmoIndicator;
import com.gadarts.industrial.systems.ui.indicators.DamageIndicator;
import com.gadarts.industrial.systems.ui.indicators.health.HealthIndicator;
import com.gadarts.industrial.systems.ui.indicators.WeaponIndicator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
public class SystemsCommonData implements Disposable {
	public static final int CAMERA_LIGHT_FAR = 7;
	public static final String TABLE_NAME_HUD = "hud";
	public static final float MELEE_ATTACK_MAX_HEIGHT = 0.5F;
	private final ModelInstancePools pooledModelInstances = new ModelInstancePools();
	@Setter(AccessLevel.NONE)
	private final String versionName;
	private final int versionNumber;
	private final com.badlogic.gdx.utils.Queue<Entity> turnsQueue = new com.badlogic.gdx.utils.Queue<>();
	private final SoundPlayer soundPlayer;
	private PlayerStorage storage;
	private DrawFlags drawFlags;
	private ParticleSystem particleSystem;
	private Entity cursor;
	private Camera camera;
	private MapGraph map;
	private Entity player;
	private int numberOfVisible;
	private GameStage uiStage;
	private boolean cameraIsRotating;
	private long currentTurnId;
	private Table menuTable;
	private GameMode currentGameMode = GameMode.EXPLORE;
	private HealthIndicator healthIndicator;
	private DamageIndicator damageIndicator;
	private Button inventoryButton;
	private WeaponIndicator weaponIndicator;
	private AmmoIndicator ammoIndicator;

	@Override
	public void dispose( ) {
		uiStage.clear();
		uiStage.dispose();
	}
}
