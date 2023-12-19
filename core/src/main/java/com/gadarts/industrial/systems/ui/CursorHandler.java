package com.gadarts.industrial.systems.ui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Disposable;
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.floor.FloorComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.model.map.MapNodesTypes;
import com.gadarts.industrial.shared.utils.CameraUtils;
import com.gadarts.industrial.systems.SystemsCommonData;
import lombok.Getter;
import squidpony.squidmath.Coord3D;

import java.util.ArrayDeque;

import static com.gadarts.industrial.components.ComponentsMapper.modelInstance;

@Getter
public class CursorHandler implements Disposable, InputProcessor {
	public static final Color CURSOR_REGULAR = Color.YELLOW;
	public static final Color CURSOR_UNAVAILABLE = Color.DARK_GRAY;
	public static final Color CURSOR_ATTACK = Color.RED;
	public static final String POSITION_LABEL_FORMAT = "Row: %s , Col: %s";
	public static final Color POSITION_LABEL_COLOR = Color.WHITE;
	public static final float POSITION_LABEL_Y = 10F;
	private static final float CURSOR_FLICKER_STEP = 1.5f;
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final Vector3 auxVector3_2 = new Vector3();
	private final SystemsCommonData systemsCommonData;
	private final BitmapFont cursorCellPositionLabelFont;
	private final Label cursorCellPositionLabel;
	private ModelInstance cursorModelInstance;
	private float cursorFlickerChange = CURSOR_FLICKER_STEP;
	private Image mouse;
	private ToolTipHandler toolTipHandler;

	public CursorHandler(SystemsCommonData systemsCommonData) {
		this.systemsCommonData = systemsCommonData;
		if (DebugSettings.DISPLAY_CURSOR_POSITION) {
			cursorCellPositionLabelFont = new BitmapFont();
			Label.LabelStyle style = new Label.LabelStyle(cursorCellPositionLabelFont, POSITION_LABEL_COLOR);
			cursorCellPositionLabel = new Label(null, style);
			systemsCommonData.getUiStage().addActor(cursorCellPositionLabel);
			cursorCellPositionLabel.setPosition(0, POSITION_LABEL_Y);
		}
	}

	MapGraphNode getCursorNode( ) {
		Vector3 dest = getCursorModelInstance().transform.getTranslation(auxVector3_1);
		return systemsCommonData.getMap().getNode((int) dest.x, (int) dest.z);
	}

	@SuppressWarnings("SameParameterValue")
	private void setCursorColor(final Color color) {
		Material material = cursorModelInstance.materials.get(0);
		ColorAttribute colorAttribute = (ColorAttribute) material.get(ColorAttribute.Diffuse);
		colorAttribute.color.set(color);
	}

	public void handleCursorFlicker(final float deltaTime) {
		Material mat = modelInstance.get(systemsCommonData.getCursor()).getModelInstance().materials.get(0);
		if (((ColorAttribute) mat.get(ColorAttribute.Diffuse)).color.equals(CURSOR_ATTACK)) {
			BlendingAttribute blend = (BlendingAttribute) mat.get(BlendingAttribute.Type);
			if (blend.opacity > 0.9) {
				cursorFlickerChange = -CURSOR_FLICKER_STEP;
			} else if (blend.opacity < 0.1) {
				cursorFlickerChange = CURSOR_FLICKER_STEP;
			}
			setCursorOpacity(blend.opacity + cursorFlickerChange * deltaTime);
		}
		toolTipHandler.handleToolTip(systemsCommonData.getMap(), getCursorNode());
	}

	void colorizeCursor(MapGraphNode node) {
		Entity nodeEntity = node.getEntity();
		boolean hasModel = modelInstance.has(nodeEntity);
		if (hasModel && modelInstance.get(nodeEntity).getFlatColor() == null) {
			setCursorOpacity(1F);
			setCursorColor(CURSOR_REGULAR);
		} else {
			setCursorColor(CURSOR_UNAVAILABLE);
		}
	}

	private void setCursorOpacity(final float opacity) {
		Material material = cursorModelInstance.materials.get(0);
		BlendingAttribute blend = (BlendingAttribute) material.get(BlendingAttribute.Type);
		blend.opacity = opacity;
		material.set(blend);
	}

	public void init(Texture texture) {
		toolTipHandler = new ToolTipHandler(getSystemsCommonData().getUiStage());
		toolTipHandler.addToolTipTable();
		cursorModelInstance = modelInstance.get(systemsCommonData.getCursor()).getModelInstance();
		mouse = new Image(texture);
		systemsCommonData.getUiStage().addActor(mouse);
		InputMultiplexer multiplexer = (InputMultiplexer) Gdx.input.getInputProcessor();
		multiplexer.addProcessor(this);
	}

	@Override
	public void dispose( ) {
		cursorCellPositionLabelFont.dispose();
		toolTipHandler.dispose();
	}


	@Override
	public boolean keyDown(int keycode) {
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	private MapGraphNode calculateNewNode(int screenX, int screenY) {
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		MapGraph map = systemsCommonData.getMap();
		Camera camera = systemsCommonData.getCamera();
		ArrayDeque<Coord3D> nodes = CameraUtils.findAllCoordsOnRay(screenX, screenY, camera);
		return findNearestNodeOnCameraLineOfSight(map, nodes);
	}

	private MapGraphNode findNearestNodeOnCameraLineOfSight(MapGraph map,
															ArrayDeque<Coord3D> nodes) {
		for (Coord3D coord : nodes) {
			MapGraphNode node = map.getNode(coord.x, coord.z);
			if (node != null && (coord.getY() <= node.getHeight() || coord.y == 0) && node.getEntity() != null) {
				FloorComponent floorComponent = ComponentsMapper.floor.get(node.getEntity());
				MapNodesTypes nodeType = floorComponent.getNode().getType();
				if (floorComponent.getNode().isReachable() && nodeType == MapNodesTypes.PASSABLE_NODE) {
					return node;
				}
			}
		}
		return null;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		mouse.setPosition(screenX - mouse.getWidth() / 2F, Gdx.graphics.getHeight() - screenY - mouse.getHeight());
		MapGraph map = getSystemsCommonData().getMap();
		MapGraphNode newNode = calculateNewNode(screenX, screenY);
		ModelInstance cursorModelInstance = getCursorModelInstance();
		MapGraphNode oldNode = map.getNode(cursorModelInstance.transform.getTranslation(auxVector3_2));
		if (newNode != null && !newNode.equals(oldNode)) {
			int col = newNode.getCol();
			int row = newNode.getRow();
			getCursorModelInstance().transform.setTranslation(col + 0.5f, newNode.getHeight(), row + 0.5f);
			colorizeCursor(newNode);
			if (cursorCellPositionLabel != null) {
				cursorCellPositionLabel.setText(String.format(POSITION_LABEL_FORMAT, row, col));
			}
			toolTipHandler.onMouseEnteredNewNode();
		}
		return true;
	}

	@Override
	public boolean scrolled(float amountX, float amountY) {
		return false;
	}
}
