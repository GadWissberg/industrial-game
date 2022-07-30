package com.gadarts.industrial.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.systems.player.PlayerStorage;
import com.gadarts.industrial.systems.render.DrawFlags;
import com.gadarts.industrial.systems.ui.GameStage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
public class SystemsCommonData implements Disposable {
	public static final int CAMERA_LIGHT_FAR = 30;
	public static final String TABLE_NAME_HUD = "hud";
	private final ModelInstancePools pooledModelInstances = new ModelInstancePools();

	@Setter(AccessLevel.NONE)
	private final String versionName;
	private final int versionNumber;
	private final PlayerStorage storage = new PlayerStorage();
	private final com.badlogic.gdx.utils.Queue<Entity> turnsQueue = new com.badlogic.gdx.utils.Queue<>();
	private final SoundPlayer soundPlayer;
	private DrawFlags drawFlags;
	private ParticleSystem particleSystem;
	private Entity cursor;
	private Camera camera;
	private MapGraph map;
	private Entity player;
	private int numberOfVisible;
	private GameStage uiStage;
	private boolean cameraIsRotating;
	private Entity currentHighLightedPickup;
	private Entity itemToPickup;
	private long currentTurnId;
	private Table menuTable;

	@Override
	public void dispose( ) {
		uiStage.clear();
		uiStage.dispose();
	}
}
