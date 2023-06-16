package com.gadarts.industrial.systems.ui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.floor.FloorComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.player.Item;
import com.gadarts.industrial.console.commands.ConsoleCommandResult;
import com.gadarts.industrial.console.commands.ConsoleCommands;
import com.gadarts.industrial.console.commands.ConsoleCommandsList;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.Assets.Fonts;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.model.map.MapNodesTypes;
import com.gadarts.industrial.shared.utils.CameraUtils;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.systems.input.InputSystemEventsSubscriber;
import com.gadarts.industrial.systems.player.PlayerSystemEventsSubscriber;
import com.gadarts.industrial.systems.turns.GameMode;
import com.gadarts.industrial.systems.turns.TurnsSystemEventsSubscriber;
import com.gadarts.industrial.systems.ui.menu.MenuHandler;
import com.gadarts.industrial.systems.ui.menu.MenuHandlerImpl;
import com.gadarts.industrial.utils.EntityBuilder;
import lombok.Getter;
import squidpony.squidmath.Coord3D;

import java.util.ArrayDeque;

import static com.badlogic.gdx.Application.LOG_DEBUG;
import static com.gadarts.industrial.DebugSettings.FULL_SCREEN;
import static com.gadarts.industrial.TerrorEffector.*;
import static com.gadarts.industrial.systems.SystemsCommonData.TABLE_NAME_HUD;

public class UserInterfaceSystem extends GameSystem<UserInterfaceSystemEventsSubscriber> implements
		InputSystemEventsSubscriber,
		TurnsSystemEventsSubscriber,
		PlayerSystemEventsSubscriber,
		CharacterSystemEventsSubscriber {
	private static final BoundingBox auxBoundingBox = new BoundingBox();
	private static final Vector3 auxVector3_2 = new Vector3();
	private static final String BUTTON_NAME_STORAGE = "button_storage";
	private static final float BUTTON_PADDING = 20;
	private boolean showBorders = DebugSettings.DISPLAY_HUD_OUTLINES;
	@Getter
	private MenuHandler menuHandler;
	private CursorHandler cursorHandler;
	private ToolTipHandler toolTipHandler;
	private HealthIndicator healthIndicator;
	private DamageIndicator damageIndicator;

	public UserInterfaceSystem(GameAssetManager assetsManager,
							   GameLifeCycleHandler lifeCycleHandler) {
		super(assetsManager, lifeCycleHandler);
	}

	@Override
	public void onCharacterGotDamage(Entity character, int originalValue) {
		if (!ComponentsMapper.player.has(character)) return;


		int hp = ComponentsMapper.character.get(getSystemsCommonData().getPlayer()).getAttributes().getHealthData().getHp();
		healthIndicator.setValue(hp, originalValue);
		damageIndicator.show();
	}

	@Override
	public void onSystemReset(SystemsCommonData systemsCommonData) {
		super.onSystemReset(systemsCommonData);
		GameStage stage = addUiStage();
		damageIndicator = new DamageIndicator(stage, getAssetsManager());
		Table hudTable = addTable();
		hudTable.setName(TABLE_NAME_HUD);
		addHealthIndicator(hudTable);
		addStorageButton(hudTable);
	}


	private void addHealthIndicator(Table hudTable) {
		GameAssetManager assetsManager = getAssetsManager();
		Texture texture = assetsManager.getTexture(Assets.UiTextures.HUD_HP_BORDER);
		int hp = ComponentsMapper.character.get(getSystemsCommonData().getPlayer()).getAttributes().getHealthData().getHp();
		BitmapFont font = assetsManager.getFont(Fonts.HUD);
		healthIndicator = new HealthIndicator(texture, font, hp, assetsManager.getTexture(Assets.UiTextures.HUD_HP_HEART));
		hudTable.add(healthIndicator).expand().left().bottom().pad(BUTTON_PADDING);
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
	public void onNewTurn(Entity entity) {
		Button button = getSystemsCommonData().getUiStage().getRoot().findActor(BUTTON_NAME_STORAGE);
		button.setTouchable(ComponentsMapper.player.has(entity) ? Touchable.enabled : Touchable.disabled);
	}


	private void addStorageButton(final Table table) {
		Button.ButtonStyle buttonStyle = new Button.ButtonStyle();
		GameAssetManager assetsManager = getAssetsManager();
		Button button = createButtonStyle(buttonStyle, assetsManager);
		button.setName(BUTTON_NAME_STORAGE);
		button.addListener(new ClickListener() {
			@Override
			public void clicked(final InputEvent event, final float x, final float y) {
				super.clicked(event, x, y);
				SystemsCommonData commonData = getSystemsCommonData();
				commonData.getUiStage().openStorageWindow(assetsManager, commonData, subscribers);
				getSystemsCommonData().getSoundPlayer().playSound(Assets.Sounds.UI_CLICK);
			}
		});
		table.add(button).expand().left().bottom().pad(BUTTON_PADDING);
	}

	private Button createButtonStyle(Button.ButtonStyle buttonStyle, GameAssetManager assetsManager) {
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

	private GameStage addUiStage( ) {
		int width = FULL_SCREEN ? FULL_SCREEN_RESOLUTION_WIDTH : WINDOWED_RESOLUTION_WIDTH;
		int height = FULL_SCREEN ? FULL_SCREEN_RESOLUTION_HEIGHT : WINDOWED_RESOLUTION_HEIGHT;
		GameStage stage;
		stage = new GameStage(new FitViewport(width, height), getSystemsCommonData().getSoundPlayer());
		getSystemsCommonData().setUiStage(stage);
		stage.setDebugAll(DebugSettings.DISPLAY_HUD_OUTLINES);
		return stage;
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
		menuHandler = new MenuHandlerImpl(getSystemsCommonData(), getSubscribers(), getAssetsManager());
		menuHandler.init(addTable(), getAssetsManager(), getSystemsCommonData());
		toolTipHandler = new ToolTipHandler(getSystemsCommonData().getUiStage());
		toolTipHandler.addToolTipTable();
	}

	@Override
	public void touchDown(final int screenX, final int screenY, final int button) {
		if (isTouchDisabled()) return;

		SystemsCommonData data = getSystemsCommonData();
		if (button == Input.Buttons.LEFT) {
			GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(data.getCursor()).getModelInstance();
			Vector3 cursorPos = modelInstance.transform.getTranslation(auxVector3_2);
			MapGraphNode node = data.getMap().getNode(cursorPos);
			Vector3 playerPosition = ComponentsMapper.characterDecal.get(data.getPlayer()).getDecal().getPosition();
			if (node.equals(data.getMap().getNode(playerPosition))) {
				for (UserInterfaceSystemEventsSubscriber sub : subscribers) {
					sub.onUserLeftClickedThePlayer(node);
				}
			} else {
				Entity nodeEntity = node.getEntity();
				if (nodeEntity != null) {
					boolean hasModel = ComponentsMapper.modelInstance.has(nodeEntity);
					if (hasModel && ComponentsMapper.modelInstance.get(nodeEntity).getFlatColor() == null) {
						onUserSelectedNodeToApplyTurn();
					}
				}
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
		Entity currentChar = getSystemsCommonData().getTurnsQueue().first();
		if (getSystemsCommonData().getCurrentGameMode() == GameMode.EXPLORE || (ComponentsMapper.player.has(currentChar)
				&& ComponentsMapper.character.get(currentChar).getCommands().isEmpty())) {
			MapGraphNode cursorNode = cursorHandler.getCursorNode();
			for (UserInterfaceSystemEventsSubscriber sub : subscribers) {
				sub.onUserSelectedNodeToApplyTurn(cursorNode);
			}
		}
	}

	private Entity createAndAdd3dCursor( ) {
		Model model = getAssetsManager().getModel(Assets.Models.CURSOR);
		model.calculateBoundingBox(auxBoundingBox);
		return EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
				.addModelInstanceComponent(new GameModelInstance(model, auxBoundingBox, false), true)
				.finishAndAddToEngine();
	}

	@Override
	public void dispose( ) {
		cursorHandler.dispose();
		toolTipHandler.dispose();
	}

}
