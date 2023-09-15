package com.gadarts.industrial.systems.ui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.floor.FloorComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.player.Item;
import com.gadarts.industrial.components.player.WeaponAmmo;
import com.gadarts.industrial.console.commands.ConsoleCommandResult;
import com.gadarts.industrial.console.commands.ConsoleCommands;
import com.gadarts.industrial.console.commands.ConsoleCommandsList;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.Assets.Fonts;
import com.gadarts.industrial.shared.assets.Assets.UiTextures;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.assets.declarations.characters.enemies.EnemiesDeclarations;
import com.gadarts.industrial.shared.assets.declarations.pickups.weapons.PlayerWeaponDeclaration;
import com.gadarts.industrial.shared.assets.declarations.pickups.weapons.PlayerWeaponsDeclarations;
import com.gadarts.industrial.shared.model.characters.player.PlayerDeclaration;
import com.gadarts.industrial.shared.model.map.MapNodesTypes;
import com.gadarts.industrial.shared.utils.CameraUtils;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.systems.enemy.EnemySystemEventsSubscriber;
import com.gadarts.industrial.systems.enemy.ai.EnemyAiStatus;
import com.gadarts.industrial.systems.input.InputSystemEventsSubscriber;
import com.gadarts.industrial.systems.player.PlayerSystemEventsSubscriber;
import com.gadarts.industrial.systems.turns.GameMode;
import com.gadarts.industrial.systems.turns.TurnsSystemEventsSubscriber;
import com.gadarts.industrial.systems.ui.indicators.AmmoIndicator;
import com.gadarts.industrial.systems.ui.indicators.DamageIndicator;
import com.gadarts.industrial.systems.ui.indicators.WeaponIndicator;
import com.gadarts.industrial.systems.ui.indicators.health.HealthIndicator;
import com.gadarts.industrial.systems.ui.indicators.turns.TurnsIndicatorsHandler;
import com.gadarts.industrial.systems.ui.menu.MenuHandler;
import com.gadarts.industrial.systems.ui.menu.MenuHandlerImpl;
import com.gadarts.industrial.utils.EntityBuilder;
import lombok.Getter;
import squidpony.squidmath.Coord3D;

import java.util.ArrayDeque;
import java.util.HashMap;

import static com.badlogic.gdx.Application.LOG_DEBUG;
import static com.gadarts.industrial.DebugSettings.FULL_SCREEN;
import static com.gadarts.industrial.TerrorEffector.*;
import static com.gadarts.industrial.shared.assets.Assets.Declarations.ENEMIES;
import static com.gadarts.industrial.shared.assets.Assets.Declarations.PLAYER_WEAPONS;
import static com.gadarts.industrial.systems.SystemsCommonData.TABLE_NAME_HUD;

public class UserInterfaceSystem extends GameSystem<UserInterfaceSystemEventsSubscriber> implements
		InputSystemEventsSubscriber,
		TurnsSystemEventsSubscriber,
		PlayerSystemEventsSubscriber,
		CharacterSystemEventsSubscriber,
		EnemySystemEventsSubscriber {
	private static final BoundingBox auxBoundingBox = new BoundingBox();
	private static final Vector3 auxVector3_2 = new Vector3();
	private static final float PADDING = 20;
	private static final float PADDING_BOTTOM_INVENTORY_BUTTON = 10;
	private boolean showBorders = DebugSettings.DISPLAY_HUD_OUTLINES;
	@Getter
	private MenuHandler menuHandler;
	private CursorHandler cursorHandler;
	private ToolTipHandler toolTipHandler;
	private TurnsIndicatorsHandler turnsIndicatorsHandler;
	private NoiseEffectHandler noiseEffectHandler;

	public UserInterfaceSystem(GameAssetManager assetsManager,
							   GameLifeCycleHandler lifeCycleHandler) {
		super(assetsManager, lifeCycleHandler);
	}

	@Override
	public void onEnemyAwaken(Entity enemy, EnemyAiStatus prevAiStatus, boolean wokeBySpottingPlayer) {
		turnsIndicatorsHandler.addCharacter(enemy);
	}

	@Override
	public void onCharacterDies(Entity character) {
		turnsIndicatorsHandler.removeCharacter(character);
	}

	@Override
	public void onCharacterGotDamage(Entity character, int originalValue) {
		if (ComponentsMapper.player.has(character)) {
			SystemsCommonData systemsCommonData = getSystemsCommonData();
			int hp = ComponentsMapper.character.get(systemsCommonData.getPlayer()).getAttributes().getHealthData().getHp();
			systemsCommonData.getHealthIndicator().setValue(hp, originalValue);
			systemsCommonData.getDamageIndicator().show();
		}
		turnsIndicatorsHandler.applyDamageEffect(character);
	}

	@Override
	public void onSystemReset(SystemsCommonData systemsCommonData) {
		super.onSystemReset(systemsCommonData);
		GameAssetManager assetsManager = getAssetsManager();
		noiseEffectHandler = new NoiseEffectHandler(assetsManager.getShader(Assets.Shaders.BASIC_VERTEX),
				assetsManager.getShader(Assets.Shaders.NOISE_FRAGMENT));
		GameStage stage = addUiStage();
		systemsCommonData.setDamageIndicator(new DamageIndicator(stage, assetsManager));
		Table hudTable = addTable();
		hudTable.setName(TABLE_NAME_HUD);
		var leftSideIndicatorsTable = new Table();
		addInventoryButton(leftSideIndicatorsTable);
		addHealthIndicator(leftSideIndicatorsTable);
		hudTable.add(leftSideIndicatorsTable).expand().bottom().left();
		var rightSideIndicatorsTable = new Table();
		var ammoIndicator = addAmmoIndicator(rightSideIndicatorsTable);
		var weaponIndicator = addWeaponIndicator(rightSideIndicatorsTable);
		PlayerWeaponDeclaration weapon = (PlayerWeaponDeclaration) systemsCommonData.getStorage().getSelectedWeapon().getDeclaration();
		if (!weapon.id().equals("pnc")) {
			WeaponAmmo weaponAmmo = ComponentsMapper.player.get(systemsCommonData.getPlayer()).getAmmo().get(weapon);
			ammoIndicator.setValues(weaponAmmo);
			ammoIndicator.setVisible(!weapon.declaration().melee());
			weaponIndicator.setIcon(weapon);
		}
		hudTable.add(rightSideIndicatorsTable).expand().bottom().right();
		addTurnsIndicator();
	}

	@Override
	public void onCombatModeEngaged( ) {
		Queue<Entity> turnsQueue = getSystemsCommonData().getTurnsQueue();
		turnsIndicatorsHandler.applyCombatMode(turnsQueue);
	}

	@Override
	public void onEnemyAiStatusChange(Entity enemy, EnemyAiStatus enemyAiStatus) {
		if (enemyAiStatus == EnemyAiStatus.IDLE) {
			turnsIndicatorsHandler.removeCharacter(enemy);
		}
	}

	private void addTurnsIndicator( ) {
		GameAssetManager assetsManager = getAssetsManager();
		EnemiesDeclarations enemiesDeclarations = (EnemiesDeclarations) assetsManager.getDeclaration(ENEMIES);
		HashMap<String, TextureRegionDrawable> icons = new HashMap<>();
		enemiesDeclarations.enemiesDeclarations()
				.forEach(dec -> icons.put(dec.id(), new TextureRegionDrawable(assetsManager.getTexture(dec.getHudIcon()))));
		UiTextures hudIcon = PlayerDeclaration.getInstance().getHudIcon();
		icons.put(PlayerDeclaration.getInstance().id(), new TextureRegionDrawable(assetsManager.getTexture(hudIcon)));
		GameStage uiStage = getSystemsCommonData().getUiStage();
		turnsIndicatorsHandler = new TurnsIndicatorsHandler(assetsManager, icons, noiseEffectHandler, uiStage);
	}

	private AmmoIndicator addAmmoIndicator(Table armsIndicatorsTable) {
		GameAssetManager assetsManager = getAssetsManager();
		BitmapFont font = assetsManager.getFont(Fonts.HUD);
		AmmoIndicator ammoIndicator = new AmmoIndicator(createHudBorderStyle(), font, assetsManager, noiseEffectHandler);
		ammoIndicator.setVisible(false);
		getSystemsCommonData().setAmmoIndicator(ammoIndicator);
		armsIndicatorsTable.add(ammoIndicator).right().bottom().pad(0F, PADDING, 0F, PADDING).row();
		return ammoIndicator;
	}

	@Override
	public void onPlayerConsumedAmmo(WeaponAmmo weaponAmmo) {
		getSystemsCommonData().getAmmoIndicator().setValues(weaponAmmo);
	}

	private WeaponIndicator addWeaponIndicator(Table armsIndicatorsTable) {
		GameAssetManager assetsManager = getAssetsManager();
		PlayerWeaponsDeclarations weapons = (PlayerWeaponsDeclarations) assetsManager.getDeclaration(PLAYER_WEAPONS);
		Button.ButtonStyle style = createHudBorderStyle();
		WeaponIndicator weaponIndicator = new WeaponIndicator(style, weapons, assetsManager, noiseEffectHandler);
		weaponIndicator.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				SystemsCommonData commonData = getSystemsCommonData();
				commonData.getUiStage().openStorageWindow(assetsManager, commonData, subscribers);
			}
		});
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		systemsCommonData.setWeaponIndicator(weaponIndicator);
		armsIndicatorsTable.add(weaponIndicator).right().bottom().pad(0F, PADDING, PADDING, PADDING);
		return weaponIndicator;
	}

	private Button.ButtonStyle createHudBorderStyle( ) {
		Button.ButtonStyle style = new Button.ButtonStyle();
		GameAssetManager assetsManager = getAssetsManager();
		style.up = new TextureRegionDrawable(assetsManager.getTexture(UiTextures.HUD_BORDER));
		style.down = new TextureRegionDrawable(assetsManager.getTexture(UiTextures.HUD_BORDER_DOWN));
		style.over = new TextureRegionDrawable(assetsManager.getTexture(UiTextures.HUD_BORDER_OVER));
		return style;
	}

	private void addInventoryButton(Table table) {
		Button.ButtonStyle style = new Button.ButtonStyle();
		GameAssetManager assetsManager = getAssetsManager();
		style.up = new TextureRegionDrawable(assetsManager.getTexture(UiTextures.HUD_INVENTORY_BUTTON));
		style.down = new TextureRegionDrawable(assetsManager.getTexture(UiTextures.HUD_INVENTORY_BUTTON_CLICKED));
		style.over = new TextureRegionDrawable(assetsManager.getTexture(UiTextures.HUD_INVENTORY_BUTTON_HOVER));
		Button inventoryButton = new StorageButton(style, noiseEffectHandler);
		getSystemsCommonData().setInventoryButton(inventoryButton);
		table.add(inventoryButton).expand().bottom().left().pad(
				0F,
				PADDING,
				PADDING_BOTTOM_INVENTORY_BUTTON,
				0F).row();
		inventoryButton.addListener(new ClickListener() {
			@Override
			public void clicked(final InputEvent event, final float x, final float y) {
				super.clicked(event, x, y);
				SystemsCommonData commonData = getSystemsCommonData();
				inventoryButton.clearActions();
				commonData.getUiStage().openStorageWindow(assetsManager, commonData, subscribers);
				getSystemsCommonData().getSoundPlayer().playSound(Assets.Sounds.UI_CLICK);
			}
		});
	}


	private void addHealthIndicator(Table hudTable) {
		GameAssetManager assetsManager = getAssetsManager();
		int hp = ComponentsMapper.character.get(getSystemsCommonData().getPlayer()).getAttributes().getHealthData().getHp();
		BitmapFont font = assetsManager.getFont(Fonts.HUD);
		HealthIndicator healthIndicator = new HealthIndicator(
				createHudBorderStyle(),
				font,
				hp,
				assetsManager.getTexture(UiTextures.HUD_HP_HEART),
				noiseEffectHandler);
		getSystemsCommonData().setHealthIndicator(healthIndicator);
		hudTable.add(healthIndicator).left().bottom().pad(0F, PADDING, PADDING, 0F);
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
	public void onItemAddedToStorage(Item item, boolean firstTime) {
		getSystemsCommonData().getUiStage().onItemAddedToStorage(item);
		InputEvent overEvent = new InputEvent();
		overEvent.setType(InputEvent.Type.enter);
		overEvent.setPointer(-1);
		InputEvent defocusEvent = new InputEvent();
		defocusEvent.setType(InputEvent.Type.exit);
		defocusEvent.setPointer(-1);
		Button inventoryButton = getSystemsCommonData().getInventoryButton();
		inventoryButton.addAction(Actions.repeat(
				10,
				Actions.sequence(
						Actions.run(( ) -> inventoryButton.fire(overEvent)),
						Actions.delay(1F),
						Actions.run(( ) -> inventoryButton.fire(defocusEvent)),
						Actions.delay(1F))));
	}

	@Override
	public void onNewTurn(Entity entity) {
		Button inventoryButton = getSystemsCommonData().getInventoryButton();
		inventoryButton.setTouchable(ComponentsMapper.player.has(entity) ? Touchable.enabled : Touchable.disabled);
		turnsIndicatorsHandler.applyBorderForNewTurn(entity);
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
	public void onCharacterConsumedActionPoint(Entity character, int newValue) {
		turnsIndicatorsHandler.updateCurrentActionPointsIndicator(character, newValue);
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
		noiseEffectHandler.dispose();
	}

}
