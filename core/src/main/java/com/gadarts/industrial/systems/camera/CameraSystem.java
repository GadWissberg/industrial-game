package com.gadarts.industrial.systems.camera;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.utils.CameraUtils;
import com.gadarts.industrial.shared.utils.GeneralUtils;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.input.InputSystemEventsSubscriber;

import static com.gadarts.industrial.DebugSettings.FULL_SCREEN;
import static com.gadarts.industrial.Industrial.*;
import static com.gadarts.industrial.shared.utils.CameraUtils.*;

public class CameraSystem extends GameSystem<CameraSystemEventsSubscriber> implements InputSystemEventsSubscriber {
	private static final float FAR = 100f;
	private static final float NEAR = 0.01f;
	private static final float EXTRA_LEVEL_PADDING = 16;
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final Vector3 auxVector3_2 = new Vector3();
	private static final Vector3 auxVector3_3 = new Vector3();
	private static final float MENU_CAMERA_ROTATION = 0.1F;
	private static final Vector2 auxVector2_1 = new Vector2();
	private final Vector2 lastMousePosition = new Vector2();
	private final Vector2 lastRightPressMousePosition = new Vector2();

	public CameraSystem(GameAssetManager assetsManager,
						GameLifeCycleHandler lifeCycleHandler) {
		super(assetsManager, lifeCycleHandler);
	}

	@Override
	public void mouseMoved(int screenX, int screenY) {
		lastMousePosition.set(screenX, screenY);
	}

	private void clampCameraPosition(final Vector3 pos) {
		MapGraph map = getSystemsCommonData().getMap();
		pos.x = MathUtils.clamp(pos.x, -EXTRA_LEVEL_PADDING, map.getWidth() + EXTRA_LEVEL_PADDING);
		pos.z = MathUtils.clamp(pos.z, -EXTRA_LEVEL_PADDING, map.getDepth() + EXTRA_LEVEL_PADDING);
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		if (shouldCameraFollow()) {
			handleCameraFollow();
		}
		handleMenuRotation();
		systemsCommonData.getCamera().update();
	}

	private void handleMenuRotation( ) {
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		Entity player = systemsCommonData.getPlayer();
		if (ComponentsMapper.player.get(player).isDisabled()) {
			Decal decal = ComponentsMapper.characterDecal.get(player).getDecal();
			systemsCommonData.getCamera().rotateAround(decal.getPosition(), Vector3.Y, MENU_CAMERA_ROTATION);
		}
	}

	private boolean shouldCameraFollow( ) {
		return !getSystemsCommonData().getUiStage().hasOpenWindows()
				&& !getSystemsCommonData().isCameraIsRotating()
				&& !getSystemsCommonData().getMenuTable().isVisible();
	}

	private void handleCameraFollow( ) {
		Entity player = getSystemsCommonData().getPlayer();
		Vector3 playerPos = ComponentsMapper.characterDecal.get(player).getDecal().getPosition();
		Camera camera = getSystemsCommonData().getCamera();
		Vector3 rotationPoint = GeneralUtils.defineRotationPoint(auxVector3_1, camera, -playerPos.y);
		Vector3 diff = auxVector3_2.set(playerPos).sub(rotationPoint);
		Vector3 cameraPosDest = auxVector3_3.set(camera.position).add(diff.x, 0F, diff.z);
		cameraPosDest.y = playerPos.y + CAMERA_HEIGHT;
		camera.position.interpolate(cameraPosDest, 0.1F, Interpolation.bounce);
	}

	@Override
	public void touchDragged(final int screenX, final int screenY) {
		if (getSystemsCommonData().isCameraIsRotating() && !getSystemsCommonData().getMenuTable().isVisible()) {
			Entity player = getSystemsCommonData().getPlayer();
			Vector3 rotationPoint = ComponentsMapper.characterDecal.get(player).getDecal().getPosition();
			Camera camera = getSystemsCommonData().getCamera();
			camera.rotateAround(rotationPoint, Vector3.Y, (lastRightPressMousePosition.x - screenX) / 2f);
			clampCameraPosition(camera.position);
			lastRightPressMousePosition.set(screenX, screenY);
		}
	}

	@Override
	public void touchDown(final int screenX, final int screenY, final int button) {
		if (button == Input.Buttons.RIGHT && !getSystemsCommonData().getMenuTable().isVisible()) {
			getSystemsCommonData().setCameraIsRotating(true);
			lastRightPressMousePosition.set(screenX, screenY);
		}
	}

	@Override
	public Class<CameraSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return CameraSystemEventsSubscriber.class;
	}

	@Override
	public void initializeData( ) {
		int viewportWidth = (FULL_SCREEN ? FULL_SCREEN_RESOLUTION_WIDTH : WINDOWED_RESOLUTION_WIDTH) / 75;
		int viewportHeight = (FULL_SCREEN ? FULL_SCREEN_RESOLUTION_HEIGHT : WINDOWED_RESOLUTION_HEIGHT) / 75;
		OrthographicCamera cam = new OrthographicCamera(viewportWidth, viewportHeight);
		cam.near = NEAR;
		cam.far = FAR;
		cam.update();
		getSystemsCommonData().setCamera(cam);
		initCamera(cam);
	}

	@Override
	public void touchUp(final int screenX, final int screenY, final int button) {
		if (button == Input.Buttons.RIGHT) {
			getSystemsCommonData().setCameraIsRotating(false);
		}
	}

	private void initCamera(OrthographicCamera cam) {
		Entity player = getSystemsCommonData().getPlayer();
		Vector2 nodePosition = ComponentsMapper.characterDecal.get(player).getNodePosition(auxVector2_1);
		cam.position.set(nodePosition.x + START_OFFSET_X, CAMERA_HEIGHT, nodePosition.y + START_OFFSET_Z);
		CameraUtils.initializeCameraAngle(cam);
		cam.update();
	}


	@Override
	public void dispose( ) {

	}
}
