package com.gadarts.industrial.systems.player;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pools;
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.components.EnvironmentObjectComponent;
import com.gadarts.industrial.components.PickUpComponent;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.CharacterAnimation;
import com.gadarts.industrial.components.character.CharacterAnimations;
import com.gadarts.industrial.components.floor.FloorComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
import com.gadarts.industrial.components.player.Ammo;
import com.gadarts.industrial.components.player.Item;
import com.gadarts.industrial.components.player.PlayerComponent;
import com.gadarts.industrial.components.player.Weapon;
import com.gadarts.industrial.map.*;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.assets.declarations.pickups.weapons.PlayerWeaponDeclaration;
import com.gadarts.industrial.shared.assets.declarations.pickups.weapons.PlayerWeaponsDeclarations;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.shared.model.pickups.BulletTypes;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.amb.AmbSystemEventsSubscriber;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.systems.character.commands.CharacterCommand;
import com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.character.commands.CommandStates;
import com.gadarts.industrial.systems.enemy.EnemySystemEventsSubscriber;
import com.gadarts.industrial.systems.enemy.ai.EnemyAiStatus;
import com.gadarts.industrial.systems.input.InputSystemEventsSubscriber;
import com.gadarts.industrial.systems.render.RenderSystemEventsSubscriber;
import com.gadarts.industrial.systems.turns.GameMode;
import com.gadarts.industrial.systems.turns.TurnsSystemEventsSubscriber;
import com.gadarts.industrial.systems.ui.UserInterfaceSystemEventsSubscriber;
import com.gadarts.industrial.utils.GameUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static com.gadarts.industrial.components.character.CharacterComponent.TURN_DURATION;
import static com.gadarts.industrial.map.MapGraphConnectionCosts.CLEAN;
import static com.gadarts.industrial.shared.assets.Assets.Declarations.PLAYER_WEAPONS;
import static com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions.*;
import static com.gadarts.industrial.utils.GameUtils.calculatePath;

public class PlayerSystem extends GameSystem<PlayerSystemEventsSubscriber> implements
		UserInterfaceSystemEventsSubscriber,
		CharacterSystemEventsSubscriber,
		RenderSystemEventsSubscriber,
		AmbSystemEventsSubscriber,
		TurnsSystemEventsSubscriber,
		EnemySystemEventsSubscriber,
		InputSystemEventsSubscriber {
	public static final float LOS_MAX = 24F;
	public static final int LOS_CHECK_DELTA = 5;
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final Vector2 auxVector2_2 = new Vector2();
	private static final Vector2 auxVector2_3 = new Vector2();
	private static final CalculatePathRequest request = new CalculatePathRequest();
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final Vector3 auxVector3_2 = new Vector3();
	private final static LinkedHashSet<GridPoint2> bresenhamOutput = new LinkedHashSet<>();
	private static final List<Entity> auxEntityList = new ArrayList<>();
	public static final int PICKUP_WEAPON_AMMO_AMOUNT = 15;
	private PathPlanHandler playerPathPlanner;
	private ImmutableArray<Entity> ambObjects;
	private ImmutableArray<Entity> pickups;

	public PlayerSystem(GameAssetManager assetsManager,
						GameLifeCycleHandler lifeCycleHandler) {
		super(assetsManager, lifeCycleHandler);
	}

	@Override
	public void onCharacterEngagesPrimaryAttack(Entity character, Vector3 direction, Vector3 positionNodeCenterPosition) {
		if (ComponentsMapper.player.has(character)) {
			Weapon selectedWeapon = getSystemsCommonData().getStorage().getSelectedWeapon();
			PlayerWeaponDeclaration declaration = (PlayerWeaponDeclaration) selectedWeapon.getDeclaration();
			Ammo ammo = ComponentsMapper.player.get(character).getAmmo().get(declaration.ammoType());
			ammo.setLoaded(ammo.getLoaded() - 1);
			subscribers.forEach(sub -> sub.onPlayerConsumedAmmo(ammo));
		}
	}

	private static int flipOffDiagonal(int total, int mask, int firstDirMask, int secondDirMask) {
		if ((total & mask) == mask && ((total & firstDirMask) == firstDirMask || (total & secondDirMask) == secondDirMask)) {
			total = total & ~mask;
		}
		return total;
	}

	@Override
	public void spaceKeyPressed( ) {
		if (ComponentsMapper.player.has(getSystemsCommonData().getTurnsQueue().first())) {
			notifyPlayerFinishedTurn();
		}
	}

	@Override
	public void onDoorStateChanged(Entity doorEntity,
								   DoorComponent.DoorStates oldState,
								   DoorComponent.DoorStates newState) {
		if (newState == DoorComponent.DoorStates.OPENING || newState == DoorComponent.DoorStates.CLOSED) {
			refreshFogOfWar();
		}
	}

	@Override
	public void onCharacterNodeChanged(Entity entity, MapGraphNode oldNode, MapGraphNode newNode) {
		if (ComponentsMapper.player.has(entity)) {
			if (ComponentsMapper.trigger.has(newNode.getEntity())) {
				getLifeCycleHandler().raiseFlagToRestartGame();
			} else {
				refreshFogOfWar();
			}
		}
	}

	private void refreshFogOfWar( ) {
		MapGraph map = getSystemsCommonData().getMap();
		Vector3 playerPos = ComponentsMapper.characterDecal.get(getSystemsCommonData().getPlayer()).getNodePosition(auxVector3_1);
		MapGraphNode playerNode = map.getNode(playerPos);
		clearFlatColorAndFowSignatureForRegionOfNodes(playerNode);
		for (int dir = 0; dir < 360; dir += LOS_CHECK_DELTA) {
			revealNodes(map, playerPos, playerNode, dir);
		}
		calculateFogOfWarEdgesForFloor(playerNode, map);
		calculateGraySignatures(playerNode, map);
	}

	private void clearFlatColorAndFowSignatureForRegionOfNodes(MapGraphNode playerNode) {
		MapGraph map = getSystemsCommonData().getMap();
		int playerRow = playerNode.getRow();
		int playerCol = playerNode.getCol();
		int depth = map.getDepth();
		int width = map.getWidth();

		for (int row = (int) Math.max(playerRow - LOS_MAX, 0); row < Math.min(playerRow + LOS_MAX, depth); row++) {
			for (int col = (int) Math.max(playerCol - LOS_MAX, 0); col < Math.min(playerCol + LOS_MAX, width); col++) {
				Entity floorEntity = map.getNode(col, row).getEntity();
				if (floorEntity != null) {
					ComponentsMapper.floor.get(floorEntity).setRevealCalculated(false);
					ComponentsMapper.floor.get(floorEntity).setFogOfWarSignature(16);
					ComponentsMapper.modelInstance.get(floorEntity).setGraySignature(16);
				}
			}
		}
	}

	private void calculateFogOfWarEdgesForFloor(MapGraphNode node, MapGraph map) {
		int nodeRow = node.getRow();
		int nodeCol = node.getCol();
		float half = LOS_MAX / 2;
		for (int row = (int) (nodeRow - half); row < nodeRow + half; row++) {
			for (int col = (int) (nodeCol - half); col < nodeCol + half; col++) {
				MapGraphNode nearbyNode = map.getNode(col, row);
				if (nearbyNode != null && nearbyNode.getEntity() != null) {
					ComponentsMapper.floor.get(nearbyNode.getEntity()).setFogOfWarSignature(calculateFowSignature(nearbyNode.getEntity(), ComponentsMapper.floor.get(nearbyNode.getEntity()).getFogOfWarSignature()));
				}
			}
		}
	}

	private void calculateGraySignatures(MapGraphNode node, MapGraph map) {
		int nodeRow = node.getRow();
		int nodeCol = node.getCol();
		float half = LOS_MAX / 2;
		for (int row = (int) (nodeRow - half); row < nodeRow + half; row++) {
			for (int col = (int) (nodeCol - half); col < nodeCol + half; col++) {
				MapGraphNode nearbyNode = map.getNode(col, row);
				if (nearbyNode != null) {
					Entity entity = nearbyNode.getEntity();
					if (entity != null) {
						ModelInstanceComponent modelInstanceComponent = ComponentsMapper.modelInstance.get(entity);
						modelInstanceComponent.setGraySignature(calculateGraySignature(entity, modelInstanceComponent.getGraySignature()));
					}
				}
			}
		}
	}

	private int calculateFowSignature(Entity floor, int currentSignature) {
		int total = currentSignature & 16;
		for (Direction direction : Direction.values()) {
			Vector2 vector = direction.getDirection(auxVector2_1);
			total = calculateFowSignatureRelativeToNearbyNode(floor, total, (int) vector.x, (int) vector.y, direction.getMask());
		}
		return total;
	}

	private int calculateGraySignature(Entity floor, int currentSignature) {
		int total = currentSignature & 16;
		for (Direction direction : Direction.values()) {
			Vector2 vector = direction.getDirection(auxVector2_1);
			total = calculateGraySignatureRelativeToNearbyNode(floor, total, (int) vector.x, (int) vector.y, direction.getMask());
		}
		return flipOffDiagonalsIfNeeded(total);
	}

	private int flipOffDiagonalsIfNeeded(int total) {
		total = flipOffDiagonal(total, Direction.NORTH_EAST.getMask(), Direction.NORTH.getMask(), Direction.EAST.getMask());
		total = flipOffDiagonal(total, Direction.NORTH_WEST.getMask(), Direction.NORTH.getMask(), Direction.WEST.getMask());
		total = flipOffDiagonal(total, Direction.SOUTH_EAST.getMask(), Direction.SOUTH.getMask(), Direction.EAST.getMask());
		total = flipOffDiagonal(total, Direction.SOUTH_WEST.getMask(), Direction.SOUTH.getMask(), Direction.WEST.getMask());
		return total;
	}

	private int calculateFowSignatureRelativeToNearbyNode(Entity entity, int total, int colOffset, int rowOffset, int mask) {
		MapGraphNode node = ComponentsMapper.floor.get(entity).getNode();
		MapGraph map = getSystemsCommonData().getMap();
		MapGraphNode nearbyNode = map.getNode(node.getCol() + colOffset, node.getRow() + rowOffset);
		boolean result = true;
		if (nearbyNode != null) {
			Entity nearbyNodeEntity = nearbyNode.getEntity();
			if (nearbyNodeEntity != null) {
				result = !DebugSettings.DISABLE_FOW
						&& !ComponentsMapper.floor.get(nearbyNodeEntity).isDiscovered();
			}
		}
		total |= result ? mask : 0;
		return total;
	}

	private int calculateGraySignatureRelativeToNearbyNode(Entity entity, int total, int colOffset, int rowOffset, int mask) {
		MapGraphNode node = ComponentsMapper.floor.get(entity).getNode();
		MapGraph map = getSystemsCommonData().getMap();
		MapGraphNode nearbyNode = map.getNode(node.getCol() + colOffset, node.getRow() + rowOffset);
		boolean result = true;
		if (nearbyNode != null) {
			Entity nearbyNodeEntity = nearbyNode.getEntity();
			if (nearbyNodeEntity != null) {
				result = !DebugSettings.DISABLE_FOW
						&& ((ComponentsMapper.modelInstance.get(nearbyNodeEntity).getGraySignature() & 16) == 16);
			}
		}
		total |= result ? mask : 0;
		return total;
	}

	@Override
	public void onCombatModeEngaged( ) {
		ComponentsMapper.character.get(getSystemsCommonData().getPlayer()).getCommands().clear();
	}

	private void revealNodes(MapGraph map, Vector3 src, MapGraphNode playerNode, int dir) {
		Vector2 maxSight = auxVector2_2.set(src.x, src.z)
				.add(auxVector2_3.set(1, 0)
						.setAngleDeg(dir).nor()
						.scl(LOS_MAX));
		LinkedHashSet<GridPoint2> nodes = GameUtils.findAllNodesBetweenNodes(
				auxVector2_1.set(src.x, src.z),
				maxSight,
				bresenhamOutput);
		boolean blocked = false;
		for (GridPoint2 nodeCoord : nodes) {
			blocked = applyLineOfSightOnNode(map, playerNode, blocked, nodeCoord);
		}
	}

	@Override
	public void onEnemyAwaken(Entity enemy, EnemyAiStatus prevAiStatus, boolean wokeBySpottingPlayer) {
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		if (systemsCommonData.getCurrentGameMode() == GameMode.EXPLORE) {
			Entity player = systemsCommonData.getPlayer();
			ComponentsMapper.character.get(player).getCommands().forEach(c -> c.setState(CommandStates.ENDED));
		}
	}

	private boolean applyLineOfSightOnNode(MapGraph map, MapGraphNode playerNode, boolean blocked, GridPoint2 nodeCoord) {
		MapGraphNode currentNode = map.getNode(nodeCoord.x, nodeCoord.y);
		if (currentNode != null && currentNode.getEntity() != null) {
			FloorComponent floorComponent = ComponentsMapper.floor.get(currentNode.getEntity());
			ModelInstanceComponent modelInstanceComp = ComponentsMapper.modelInstance.get(currentNode.getEntity());
			if (!floorComponent.isRevealCalculated()) {
				if (modelInstanceComp != null) {
					Color flatColor = null;
					if (!DebugSettings.DISABLE_FOW && blocked && !floorComponent.isDiscovered()) {
						flatColor = Color.BLACK;
					}
					modelInstanceComp.setFlatColor(flatColor);
					modelInstanceComp.setGraySignature(blocked ? 16 : 0);
				}
				floorComponent.setFogOfWarSignature(blocked ? 16 : 0);
				floorComponent.setRevealCalculated(true);
				if (!blocked) {
					floorComponent.setDiscovered(true);
				}
			}
			if (!blocked) {
				if (checkIfNodeBlocks(playerNode, currentNode) || checkIfAnyEnvironmentObjectBlocks(playerNode, currentNode)) {
					blocked = true;
				}
			}
		}
		return blocked;
	}

	private boolean checkIfAnyEnvironmentObjectBlocks(MapGraphNode playerNode, MapGraphNode currentNode) {
		boolean result = false;
		for (Entity environmentObject : ambObjects) {
			if (checkIfEnvironmentObjectBlocks(playerNode, currentNode, environmentObject)) {
				result = true;
				break;
			}
		}
		return result;
	}

	private boolean checkIfEnvironmentObjectBlocks(MapGraphNode playerNode,
												   MapGraphNode currentNode,
												   Entity environmentObject) {
		float height = ComponentsMapper.environmentObject.get(environmentObject).getType().getHeight();
		if (height > 0) {
			GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(environmentObject).getModelInstance();
			Vector3 position = modelInstance.transform.getTranslation(auxVector3_2);
			MapGraphNode node = getSystemsCommonData().getMap().getNode(position);
			float thingTopSide = position.y + height;
			return thingTopSide >= playerNode.getHeight() + PlayerComponent.PLAYER_HEIGHT && currentNode.equals(node);
		}
		return false;
	}

	@Override
	public void onSelectedWeaponChanged(Weapon selectedWeapon) {
		PlayerWeaponDeclaration definition = (PlayerWeaponDeclaration) selectedWeapon.getDeclaration();
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		Entity player = systemsCommonData.getPlayer();
		CharacterDecalComponent cdc = ComponentsMapper.characterDecal.get(player);
		CharacterAnimations animations = getAssetsManager().get(definition.relatedAtlas().name());
		cdc.init(animations, cdc.getSpriteType(), cdc.getDirection(), auxVector3_1.set(cdc.getDecal().getPosition()));
		CharacterAnimation animation = animations.get(cdc.getSpriteType(), cdc.getDirection());
		ComponentsMapper.animation.get(player).init(cdc.getSpriteType().getFrameDuration(), animation);
		ComponentsMapper.character.get(player).setPrimaryAttack(definition.declaration());
		if (selectedWeapon != systemsCommonData.getStorage().getSelectedWeapon()) {
			systemsCommonData.getStorage().setSelectedWeapon(selectedWeapon);
		}
	}

	@Override
	public void onItemPickedUp(final Entity itemPickedUp) {
		Item item = ComponentsMapper.pickup.get(itemPickedUp).getItem();
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		PlayerStorage storage = systemsCommonData.getStorage();
		var firstTime = storage.isFirstTimePickup(item);
		boolean added = storage.addItem(item);
		if (item.isWeapon()) {
			Map<BulletTypes, Ammo> ammo = ComponentsMapper.player.get(systemsCommonData.getPlayer()).getAmmo();
			PlayerWeaponDeclaration declaration = (PlayerWeaponDeclaration) item.getDeclaration();
			BulletTypes ammoType = declaration.ammoType();
			if (ammo.containsKey(ammoType)) {
				ammo.get(ammoType).setLoaded(PICKUP_WEAPON_AMMO_AMOUNT);
			}
		}
		if (added) {
			for (PlayerSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onItemAddedToStorage(item, firstTime);
			}
		}
		systemsCommonData.getSoundPlayer().playSound(Assets.Sounds.PICKUP);
	}

	private boolean checkIfNodeBlocks(MapGraphNode playerNode, MapGraphNode currentNode) {
		Entity door = currentNode.getDoor();
		return playerNode.getHeight() + PlayerComponent.PLAYER_HEIGHT < currentNode.getHeight()
				|| (door != null && ComponentsMapper.door.get(door).getState() == DoorComponent.DoorStates.CLOSED);
	}

	private void notifyPlayerFinishedTurn( ) {
		ComponentsMapper.character.get(getSystemsCommonData().getTurnsQueue().first()).setTurnTimeLeft(TURN_DURATION);
		for (PlayerSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onPlayerFinishedTurn();
		}
	}

	@Override
	public void onCharacterCommandDone(Entity character) {
		if (ComponentsMapper.player.has(character)) {
			if (ComponentsMapper.character.get(character).getAttributes().getActionPoints() <= 0) {
				subscribers.forEach(PlayerSystemEventsSubscriber::onPlayerFinishedTurn);
			}
		}
	}

	@Override
	public void onUserSelectedNodeToApplyTurn(MapGraphNode node) {
		CharacterDecalComponent charDecalComp = ComponentsMapper.characterDecal.get(getSystemsCommonData().getPlayer());
		MapGraphPath plannedPath = playerPathPlanner.getCurrentPath();
		initializePathPlanRequest(node, charDecalComp, plannedPath);
		boolean foundPath = calculatePath(request, playerPathPlanner.getPathFinder(), playerPathPlanner.getHeuristic());
		if (foundPath) {
			pathHasCreated(node, request.getOutputPath());
		}
	}

	@Override
	public void onGameModeSet( ) {
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		if (systemsCommonData.getCurrentGameMode() == GameMode.EXPLORE) {
			ComponentsMapper.character.get(systemsCommonData.getPlayer()).getAttributes().resetActionPoints();
		}
	}

	private void pathHasCreated(MapGraphNode destination, MapGraphPath outputPath) {
		int pathSize = outputPath.getCount();
		if (!outputPath.nodes.isEmpty() && outputPath.get(pathSize - 1).equals(destination)) {
			SystemsCommonData systemsCommonData = getSystemsCommonData();
			Entity player = systemsCommonData.getPlayer();
			ComponentsMapper.character.get(player).getCommands().clear();
			addCommand(outputPath, RUN);
			Entity enemyAtNode = systemsCommonData.getMap().fetchAliveCharacterFromNode(destination);
			if (enemyAtNode != null) {
				ComponentsMapper.character.get(player).setTarget(enemyAtNode);
				addCommand(outputPath, ATTACK_PRIMARY);
			} else {
				List<Entity> pickupsAtNode = systemsCommonData.getMap().fetchPickupsFromNode(destination, auxEntityList);
				if (!pickupsAtNode.isEmpty()) {
					addCommand(outputPath, PICKUP);
				}
			}
		}
	}

	private void addCommand(MapGraphPath outputPath,
							CharacterCommandsDefinitions characterCommandDefinition) {
		CharacterCommand command = Pools.get(characterCommandDefinition.getCharacterCommandImplementation()).obtain();
		Entity player = getSystemsCommonData().getPlayer();
		command.reset(characterCommandDefinition, player, outputPath);
		ComponentsMapper.character.get(player).getCommands().addLast(command);
	}


	@Override
	public void onUserLeftClickedThePlayer(MapGraphNode playerNode) {
		for (Entity pickup : pickups) {
			GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(pickup).getModelInstance();
			Vector3 pickupPosition = modelInstance.transform.getTranslation(auxVector3_1);
			if (getSystemsCommonData().getMap().getNode(pickupPosition).equals(playerNode)) {
			}
		}
	}

	private void initializePathPlanRequest(MapGraphNode sourceNode,
										   MapGraphNode destinationNode,
										   MapGraphConnectionCosts maxCostInclusive,
										   boolean avoidCharactersInCalculations,
										   MapGraphPath outputPath) {
		request.setSourceNode(sourceNode);
		request.setDestNode(destinationNode);
		request.setOutputPath(outputPath);
		request.setAvoidCharactersInCalculations(avoidCharactersInCalculations);
		request.setMaxCostInclusive(maxCostInclusive);
		request.setRequester(getSystemsCommonData().getPlayer());
	}

	private void initializePathPlanRequest(MapGraphNode cursorNode,
										   CharacterDecalComponent charDecalComp,
										   MapGraphPath plannedPath) {
		initializePathPlanRequest(
				getSystemsCommonData().getMap().getNode(charDecalComp.getNodePosition(auxVector2_1)),
				cursorNode,
				CLEAN,
				true,
				plannedPath);
	}

	@Override
	public Class<PlayerSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return PlayerSystemEventsSubscriber.class;
	}

	@Override
	public void initializeData( ) {
		playerPathPlanner = new PathPlanHandler(getSystemsCommonData().getMap());
		changePlayerStatus(!getLifeCycleHandler().isInGame());
		refreshFogOfWar();
	}

	private void changePlayerStatus(final boolean disabled) {
		PlayerComponent playerComponent = ComponentsMapper.player.get(getSystemsCommonData().getPlayer());
		playerComponent.setDisabled(disabled);
		subscribers.forEach(PlayerSystemEventsSubscriber::onPlayerStatusChanged);
	}


	@Override
	public void onSystemReset(SystemsCommonData systemsCommonData) {
		super.onSystemReset(systemsCommonData);
		getSystemsCommonData().setPlayer(getEngine().getEntitiesFor(Family.all(PlayerComponent.class).get()).first());
		Weapon weapon = initializeStartingWeapon();
		getSystemsCommonData().setStorage(new PlayerStorage(getAssetsManager()));
		getSystemsCommonData().getStorage().setSelectedWeapon(weapon);
		ambObjects = getEngine().getEntitiesFor(Family.all(EnvironmentObjectComponent.class).exclude(DoorComponent.class).get());
		pickups = getEngine().getEntitiesFor(Family.all(PickUpComponent.class).get());
	}

	private Weapon initializeStartingWeapon( ) {
		Weapon weapon = Pools.obtain(Weapon.class);

		GameAssetManager am = getAssetsManager();
		PlayerWeaponsDeclarations weaponsDeclarations = (PlayerWeaponsDeclarations) am.getDeclaration(PLAYER_WEAPONS);
		PlayerWeaponDeclaration declaration = weaponsDeclarations.parse(DebugSettings.STARTING_WEAPON);
		Assets.UiTextures symbol = declaration.declaration().getSymbol();
		Texture image = symbol != null ? am.getTexture(symbol) : null;
		weapon.init(declaration, 0, 0, image);
		return weapon;
	}

	@Override
	public void dispose( ) {
		getSystemsCommonData().getStorage().clear();
	}
}
