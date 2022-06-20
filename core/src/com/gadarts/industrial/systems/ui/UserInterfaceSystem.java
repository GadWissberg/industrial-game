package com.gadarts.industrial.systems.ui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.gadarts.industrial.DefaultGameSettings;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.player.Item;
import com.gadarts.industrial.console.commands.ConsoleCommandResult;
import com.gadarts.industrial.console.commands.ConsoleCommands;
import com.gadarts.industrial.console.commands.ConsoleCommandsList;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.input.InputSystemEventsSubscriber;
import com.gadarts.industrial.systems.player.PlayerSystemEventsSubscriber;
import com.gadarts.industrial.systems.turns.TurnsSystemEventsSubscriber;
import com.gadarts.industrial.systems.ui.menu.MenuHandler;
import com.gadarts.industrial.systems.ui.menu.MenuHandlerImpl;
import com.gadarts.industrial.utils.EntityBuilder;
import lombok.Getter;
import squidpony.squidmath.Bresenham;
import squidpony.squidmath.Coord3D;

import java.util.ArrayDeque;
import java.util.List;

import static com.badlogic.gdx.Application.LOG_DEBUG;
import static com.gadarts.industrial.DefaultGameSettings.FULL_SCREEN;
import static com.gadarts.industrial.Industrial.*;
import static com.gadarts.industrial.systems.SystemsCommonData.TABLE_NAME_HUD;

public class UserInterfaceSystem extends GameSystem<UserInterfaceSystemEventsSubscriber> implements
		InputSystemEventsSubscriber,
		TurnsSystemEventsSubscriber,
		PlayerSystemEventsSubscriber {
	private static final BoundingBox auxBoundingBox = new BoundingBox();
	private static final Vector3 auxVector3_2 = new Vector3();
	private static final String BUTTON_NAME_STORAGE = "button_storage";
	private static final float BUTTON_PADDING = 40;
	private final SoundPlayer soundPlayer;
	private final AttackNodesHandler attackNodesHandler = new AttackNodesHandler();
	private boolean showBorders = DefaultGameSettings.DISPLAY_HUD_OUTLINES;
	@Getter
	private MenuHandler menuHandler;
	private CursorHandler cursorHandler;
	private ToolTipHandler toolTipHandler;

	public UserInterfaceSystem(SystemsCommonData systemsCommonData,
							   SoundPlayer soundPlayer,
							   GameAssetsManager assetsManager,
							   GameLifeCycleHandler lifeCycleHandler) {
		super(systemsCommonData, soundPlayer, assetsManager, lifeCycleHandler);
		this.soundPlayer = soundPlayer;
		addUiStage();
		Table hudTable = addTable();
		hudTable.setName(TABLE_NAME_HUD);
		addStorageButton(hudTable);
	}

	@Override
	public boolean onCommandRun(final ConsoleCommands command, final ConsoleCommandResult consoleCommandResult) {
		if (command == ConsoleCommandsList.BORDERS) {
			this.showBorders = !showBorders;
			getSystemsCommonData().getUiStage().setDebugAll(showBorders);
			final String MESSAGE = "UI borders are %s.";
			String msg = showBorders ? String.format(MESSAGE, "displayed") : String.format(MESSAGE, "hidden");
			consoleCommandResult.setMessage(msg);
			return true;
		}
		return false;
	}

	@Override
	public void onItemAddedToStorage(Item item) {
		getSystemsCommonData().getUiStage().onItemAddedToStorage(item);
	}

	@Override
	public void onAttackModeActivated(List<MapGraphNode> availableNodes) {
		attackNodesHandler.onAttackModeActivated(availableNodes);
	}

	@Override
	public void onAttackModeDeactivated( ) {
		attackNodesHandler.onAttackModeDeactivated();
	}

	@Override
	public void onNewTurn(Entity entity) {
		Button button = getSystemsCommonData().getUiStage().getRoot().findActor(BUTTON_NAME_STORAGE);
		button.setTouchable(ComponentsMapper.player.has(entity) ? Touchable.enabled : Touchable.disabled);
	}


	private void addStorageButton(final Table table) {
		Button.ButtonStyle buttonStyle = new Button.ButtonStyle();
		GameAssetsManager assetsManager = getAssetsManager();
		Button button = createButtonStyle(buttonStyle, assetsManager);
		button.setName(BUTTON_NAME_STORAGE);
		button.addListener(new ClickListener() {
			@Override
			public void clicked(final InputEvent event, final float x, final float y) {
				super.clicked(event, x, y);
				SystemsCommonData commonData = getSystemsCommonData();
				commonData.getUiStage().openStorageWindow(assetsManager, commonData, subscribers);
				getSoundPlayer().playSound(Assets.Sounds.UI_CLICK);
			}
		});
		table.add(button).expand().left().bottom().pad(BUTTON_PADDING);
	}

	private Button createButtonStyle(Button.ButtonStyle buttonStyle, GameAssetsManager assetsManager) {
		buttonStyle.up = new TextureRegionDrawable(assetsManager.getTexture(Assets.UiTextures.BUTTON_STORAGE));
		buttonStyle.down = new TextureRegionDrawable(assetsManager.getTexture(Assets.UiTextures.BUTTON_STORAGE_DOWN));
		buttonStyle.over = new TextureRegionDrawable(assetsManager.getTexture(Assets.UiTextures.BUTTON_STORAGE_HOVER));
		return new Button(buttonStyle);
	}

	private Table addTable( ) {
		Table table = new Table();
		Stage stage = getSystemsCommonData().getUiStage();
		stage.setDebugAll(Gdx.app.getLogLevel() == LOG_DEBUG && showBorders);
		table.setFillParent(true);
		stage.addActor(table);
		return table;
	}

	@Override
	public void reset( ) {
		getSystemsCommonData().getUiStage().dispose();
	}

	private void addUiStage( ) {
		int width = FULL_SCREEN ? FULL_SCREEN_RESOLUTION_WIDTH : WINDOWED_RESOLUTION_WIDTH;
		int height = FULL_SCREEN ? FULL_SCREEN_RESOLUTION_HEIGHT : WINDOWED_RESOLUTION_HEIGHT;
		GameStage stage;
		stage = new GameStage(new FitViewport(width, height), soundPlayer);
		getSystemsCommonData().setUiStage(stage);
		stage.setDebugAll(DefaultGameSettings.DISPLAY_HUD_OUTLINES);
	}

	@Override
	public void mouseMoved(final int screenX, final int screenY) {
		if (getSystemsCommonData().getMenuTable().isVisible()) return;
		MapGraph map = getSystemsCommonData().getMap();
		MapGraphNode newNode = calculateNewNode(screenX, screenY);
		ModelInstance cursorModelInstance = cursorHandler.getCursorModelInstance();
		MapGraphNode oldNode = map.getNode(cursorModelInstance.transform.getTranslation(auxVector3_2));
		if (newNode != null && !newNode.equals(oldNode)) {
			cursorHandler.onMouseEnteredNewNode(newNode);
			toolTipHandler.onMouseEnteredNewNode();
		}
	}

	private MapGraphNode calculateNewNode(int screenX, int screenY) {
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		MapGraph map = systemsCommonData.getMap();
		Camera camera = systemsCommonData.getCamera();

		MapGraphNode lowestNode = map.getRayNode(screenX, screenY, camera);
		ArrayDeque<Coord3D> nodes = (ArrayDeque<Coord3D>) Bresenham.line3D(
				(int) camera.position.x, (int) camera.position.y, (int) camera.position.z,
				lowestNode.getCol(), 0, lowestNode.getRow());
		Coord3D lastCoord = nodes.getLast();
		MapGraphNode result = map.getNode(lastCoord.getX(), lastCoord.z);
		result = findNearestNodeOnCameraLineOfSight(map, nodes, result);
		return result;
	}

	private MapGraphNode findNearestNodeOnCameraLineOfSight(MapGraph map,
															ArrayDeque<Coord3D> nodes,
															MapGraphNode result) {
		for (Coord3D coord : nodes) {
			MapGraphNode node = map.getNode(coord.x, coord.z);
			if (node != null && coord.getY() <= node.getHeight()) {
				result = node;
				break;
			}
		}
		return result;
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		systemsCommonData.getUiStage().act();
		cursorHandler.handleCursorFlicker(deltaTime);
		toolTipHandler.handleToolTip(systemsCommonData.getMap(), cursorHandler.getCursorNode());
	}

	@Override
	public Class<UserInterfaceSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return UserInterfaceSystemEventsSubscriber.class;
	}

	@Override
	public void initializeData( ) {
		getSystemsCommonData().setCursor(createAndAdd3dCursor());
		cursorHandler = new CursorHandler(getSystemsCommonData());
		cursorHandler.init();
		attackNodesHandler.init(getEngine());
		menuHandler = new MenuHandlerImpl(getSystemsCommonData(), getSubscribers(), getAssetsManager(), getSoundPlayer());
		menuHandler.init(addTable(), getAssetsManager(), getSystemsCommonData(), getSoundPlayer());
		toolTipHandler = new ToolTipHandler(getSystemsCommonData().getUiStage());
		toolTipHandler.addToolTipTable();
	}


	@Override
	public void touchDown(final int screenX, final int screenY, final int button) {
		if (isTouchDisabled()) return;
		SystemsCommonData data = getSystemsCommonData();
		if (button == Input.Buttons.LEFT && data.getCurrentCommand() == null) {
			GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(data.getCursor()).getModelInstance();
			Vector3 cursorPos = modelInstance.transform.getTranslation(auxVector3_2);
			MapGraphNode node = data.getMap().getNode(cursorPos);
			if (node.getEntity() != null && ComponentsMapper.modelInstance.get(node.getEntity()).getFlatColor() == null) {
				onUserSelectedNodeToApplyTurn();
			}
		}
	}

	@Override
	public void keyDown(int keycode) {
		if (keycode == Input.Keys.ESCAPE) {
			Table menuTable = getSystemsCommonData().getMenuTable();
			menuHandler.toggleMenu(!menuTable.isVisible());
		}
	}

	private boolean isTouchDisabled( ) {
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		return systemsCommonData.isCameraIsRotating()
				|| systemsCommonData.getUiStage().hasOpenWindows()
				|| systemsCommonData.getMenuTable().isVisible();
	}

	private void onUserSelectedNodeToApplyTurn( ) {
		if (!ComponentsMapper.player.has(getSystemsCommonData().getTurnsQueue().first())) return;

		MapGraphNode cursorNode = cursorHandler.getCursorNode();
		for (UserInterfaceSystemEventsSubscriber sub : subscribers) {
			sub.onUserSelectedNodeToApplyTurn(cursorNode, attackNodesHandler);
		}
	}

	private Entity createAndAdd3dCursor( ) {
		Model model = getAssetsManager().getModel(Assets.Models.CURSOR);
		model.calculateBoundingBox(auxBoundingBox);
		return EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
				.addModelInstanceComponent(new GameModelInstance(model, auxBoundingBox, false), true, false)
				.finishAndAddToEngine();
	}

	@Override
	public void dispose( ) {
		cursorHandler.dispose();
		attackNodesHandler.dispose();
		toolTipHandler.dispose();
	}

}
