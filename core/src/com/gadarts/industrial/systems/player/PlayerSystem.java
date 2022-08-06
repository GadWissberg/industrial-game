package com.gadarts.industrial.systems.player;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.gadarts.industrial.DefaultGameSettings;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.CharacterAnimation;
import com.gadarts.industrial.components.character.CharacterAnimations;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.floor.FloorComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
import com.gadarts.industrial.components.player.Item;
import com.gadarts.industrial.components.player.PlayerComponent;
import com.gadarts.industrial.components.player.Weapon;
import com.gadarts.industrial.map.*;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.shared.model.pickups.PlayerWeaponsDefinitions;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.amb.AmbSystemEventsSubscriber;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.systems.character.commands.CharacterCommand;
import com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.enemy.EnemyAiStatus;
import com.gadarts.industrial.systems.enemy.EnemySystemEventsSubscriber;
import com.gadarts.industrial.systems.render.RenderSystemEventsSubscriber;
import com.gadarts.industrial.systems.turns.TurnsSystemEventsSubscriber;
import com.gadarts.industrial.systems.ui.AttackNodesHandler;
import com.gadarts.industrial.systems.ui.UserInterfaceSystemEventsSubscriber;
import com.gadarts.industrial.utils.GameUtils;

import java.util.LinkedHashSet;
import java.util.List;

import static com.gadarts.industrial.components.ComponentsMapper.*;
import static com.gadarts.industrial.map.MapGraphConnectionCosts.CLEAN;
import static com.gadarts.industrial.shared.model.characters.Direction.*;
import static com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions.*;
import static com.gadarts.industrial.utils.GameUtils.calculatePath;

public class PlayerSystem extends GameSystem<PlayerSystemEventsSubscriber> implements
		UserInterfaceSystemEventsSubscriber,
		CharacterSystemEventsSubscriber,
		RenderSystemEventsSubscriber,
		AmbSystemEventsSubscriber,
		TurnsSystemEventsSubscriber,
		EnemySystemEventsSubscriber {
	public static final float LOS_MAX = 24F;
	public static final int LOS_CHECK_DELTA = 5;
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final Vector2 auxVector2_2 = new Vector2();
	private static final Vector2 auxVector2_3 = new Vector2();
	private static final CalculatePathRequest request = new CalculatePathRequest();
	private static final Vector3 auxVector3 = new Vector3();
	private final static LinkedHashSet<GridPoint2> bresenhamOutput = new LinkedHashSet<>();
	private PathPlanHandler playerPathPlanner;

	public PlayerSystem(SystemsCommonData systemsCommonData,
						GameAssetsManager assetsManager,
						GameLifeCycleHandler lifeCycleHandler) {
		super(systemsCommonData, assetsManager, lifeCycleHandler);
	}

	@Override
	public void onCommandInitialized( ) {
		CharacterCommand command = character.get(getSystemsCommonData().getTurnsQueue().first()).getCommand();
		if (!player.has(command.getCharacter())) return;

		for (Entity entity : getSystemsCommonData().getTurnsQueue()) {
			if (enemy.has(entity) && enemy.get(entity).getAiStatus() == EnemyAiStatus.ATTACKING) {
				command.onInFight();
				return;
			}
		}
	}

	@Override
	public void onEnemyAwaken(Entity enemy, EnemyAiStatus prevAiStatus) {
		character.get(getSystemsCommonData().getPlayer()).getCommand().onEnemyAwaken(enemy, prevAiStatus);
	}

	@Override
	public void onDoorOpened(Entity doorEntity) {
		refreshFogOfWar();
	}

	@Override
	public void onDoorClosed(Entity doorEntity) {
		refreshFogOfWar();
	}

	@Override
	public void onNewTurn(Entity entity) {

	}

	@Override
	public void onCharacterNodeChanged(Entity entity, MapGraphNode oldNode, MapGraphNode newNode) {
		if (player.has(entity)) {
			refreshFogOfWar();
			notifyPlayerFinishedTurn();
		}
	}

	private void refreshFogOfWar( ) {
		MapGraph map = getSystemsCommonData().getMap();
		Vector3 playerPos = characterDecal.get(getSystemsCommonData().getPlayer()).getNodePosition(auxVector3);
		MapGraphNode playerNode = map.getNode(playerPos);
		clearFlatColorAndFowSignatureForRegionOfNodes(playerNode);
		for (int dir = 0; dir < 360; dir += LOS_CHECK_DELTA) {
			revealNodes(map, playerPos, playerNode, dir);
		}
		calculateFogOfWarEdgesForFloor(playerNode, map);
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
					floor.get(floorEntity).setRevealCalculated(false);
					floor.get(floorEntity).setFogOfWarSignature(16);
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
					if ((row != 0 || col != 0)) {
						calculateFogOfWarSignature(nearbyNode.getEntity());
					} else {
						int northWest = NORTH.getMask() | WEST.getMask() | NORTH_EAST.getMask() | SOUTH_WEST.getMask();
						floor.get(nearbyNode.getEntity()).setFogOfWarSignature(northWest);
					}
				}
			}
		}
	}

	private void calculateFogOfWarSignature(Entity floor) {
		int total = ComponentsMapper.floor.get(floor).getFogOfWarSignature() & 16;
		FloorComponent floorComponent = ComponentsMapper.floor.get(floor);
		for (Direction direction : Direction.values()) {
			Vector2 vector = direction.getDirection(auxVector2_1);
			total = calculateFogOfWarForNode(floor, total, (int) vector.x, (int) vector.y, direction.getMask());
		}
		floorComponent.setFogOfWarSignature(total);
	}

	private int calculateFogOfWarForNode(Entity entity, int total, int colOffset, int rowOffset, int mask) {
		MapGraphNode node = floor.get(entity).getNode();
		MapGraph map = getSystemsCommonData().getMap();
		MapGraphNode nearbyNode = map.getNode(node.getCol() + colOffset, node.getRow() + rowOffset);
		boolean result = true;
		if (nearbyNode != null && nearbyNode.getEntity() != null) {
			result = !DefaultGameSettings.DISABLE_FOG && modelInstance.get(nearbyNode.getEntity()).getFlatColor() != null;
		}
		total |= result ? mask : 0;
		return total;
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

	private boolean applyLineOfSightOnNode(MapGraph map, MapGraphNode playerNode, boolean blocked, GridPoint2 nodeCoord) {
		MapGraphNode currentNode = map.getNode(nodeCoord.x, nodeCoord.y);
		if (currentNode != null && currentNode.getEntity() != null) {
			FloorComponent floorComponent = floor.get(currentNode.getEntity());
			ModelInstanceComponent modelInstanceComponent = modelInstance.get(currentNode.getEntity());
			if (!floorComponent.isRevealCalculated()) {
				modelInstanceComponent.setFlatColor(!DefaultGameSettings.DISABLE_FOG && blocked ? Color.BLACK : null);
				floorComponent.setFogOfWarSignature(blocked ? 16 : 0);
				floorComponent.setRevealCalculated(true);
			}
			if (!blocked && checkIfNodeBlocks(playerNode, currentNode)) {
				blocked = true;
			}
		}
		return blocked;
	}

	private boolean checkIfNodeBlocks(MapGraphNode playerNode, MapGraphNode currentNode) {
		Entity door = currentNode.getDoor();
		return playerNode.getHeight() + PlayerComponent.PLAYER_HEIGHT < currentNode.getHeight()
				|| (door != null && ComponentsMapper.door.get(door).getState() != DoorComponent.DoorStates.OPEN);
	}


	@Override
	public void onSelectedWeaponChanged(Weapon selectedWeapon) {
		PlayerWeaponsDefinitions definition = (PlayerWeaponsDefinitions) selectedWeapon.getDefinition();
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		Entity player = systemsCommonData.getPlayer();
		CharacterDecalComponent cdc = characterDecal.get(player);
		CharacterAnimations animations = getAssetsManager().get(Assets.Atlases.findByRelatedWeapon(definition).name());
		cdc.init(animations, cdc.getSpriteType(), cdc.getDirection(), auxVector3.set(cdc.getDecal().getPosition()));
		CharacterAnimation animation = animations.get(cdc.getSpriteType(), cdc.getDirection());
		ComponentsMapper.animation.get(player).init(cdc.getSpriteType().getAnimationDuration(), animation);
		if (selectedWeapon != systemsCommonData.getStorage().getSelectedWeapon()) {
			systemsCommonData.getStorage().setSelectedWeapon(selectedWeapon);
		}
	}

	@Override
	public void onCharacterCommandDone(final Entity character, final CharacterCommand executedCommand) {
		if (player.has(character) && executedCommand.isNewTurnOnCompletion()) {
			notifyPlayerFinishedTurn();
		}
	}

	private void notifyPlayerFinishedTurn( ) {
		for (PlayerSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onPlayerFinishedTurn();
		}
	}

	@Override
	public void onItemPickedUp(final Entity itemPickedUp) {
		Item item = pickup.get(itemPickedUp).getItem();
		if (getSystemsCommonData().getStorage().addItem(item)) {
			for (PlayerSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onItemAddedToStorage(item);
			}
		}
		getSystemsCommonData().getSoundPlayer().playSound(Assets.Sounds.PICKUP);
	}

	@Override
	public void onUserSelectedNodeToApplyTurn(MapGraphNode node, AttackNodesHandler attackNodesHandler) {
		applyPlayerTurn(node, attackNodesHandler);
	}

	private void applyPlayerTurn(final MapGraphNode cursorNode, AttackNodesHandler attackNodesHandler) {
		MapGraphPath currentPath = playerPathPlanner.getCurrentPath();
		planPath(cursorNode, attackNodesHandler);
		int pathSize = currentPath.getCount();
		if (!currentPath.nodes.isEmpty() && currentPath.get(pathSize - 1).equals(cursorNode)) {
			applyPlayerCommandAccordingToPlan(cursorNode, attackNodesHandler);
		}
	}

	private void planPath(final MapGraphNode cursorNode, AttackNodesHandler attackNodesHandler) {
		Entity enemyAtNode = getSystemsCommonData().getMap().fetchAliveEnemyFromNode(cursorNode);
		if (!calculatePathAccordingToSelection(cursorNode, enemyAtNode)) return;

		MapGraphNode selectedAttackNode = attackNodesHandler.getSelectedAttackNode();
		SystemsCommonData commonData = getSystemsCommonData();
		Entity highLightedPickup = commonData.getCurrentHighLightedPickup();
		if (highLightedPickup != null || isSelectedAttackNodeIsNotInAvailableNodes(cursorNode, selectedAttackNode)) {
			attackNodesHandler.reset();
		}
		pathHasCreated(cursorNode, enemyAtNode, attackNodesHandler);
	}

	private boolean isSelectedAttackNodeIsNotInAvailableNodes(MapGraphNode cursorNode, MapGraphNode selectedAttackNode) {
		MapGraph map = getSystemsCommonData().getMap();
		return selectedAttackNode != null
				&& !isNodeInAvailableNodes(cursorNode, map.getAvailableNodesAroundNode(selectedAttackNode));
	}

	private void enemySelected(final MapGraphNode node, final Entity enemyAtNode, final AttackNodesHandler attackNodesHandler) {
		Weapon selectedWeapon = getSystemsCommonData().getStorage().getSelectedWeapon();
		if (selectedWeapon.isMelee()) {
			List<MapGraphNode> availableNodes = getSystemsCommonData().getMap().getAvailableNodesAroundNode(node);
			attackNodesHandler.setSelectedAttackNode(node);
			activateAttackMode(enemyAtNode, availableNodes);
		} else {
			playerPathPlanner.resetPlan();
			enemySelectedWithRangeWeapon(node);
		}
	}

	private void enemySelectedWithRangeWeapon(final MapGraphNode node) {
		Entity player = getSystemsCommonData().getPlayer();
		CharacterComponent charComp = character.get(player);
		Entity targetNode = getSystemsCommonData().getMap().fetchAliveEnemyFromNode(node);
		charComp.setTarget(targetNode);
		for (PlayerSystemEventsSubscriber subscriber : subscribers) {
			CharacterCommand command = Pools.get(ATTACK_PRIMARY.getCharacterCommandImplementation()).obtain();
			subscriber.onPlayerAppliedCommand(command.set(ATTACK_PRIMARY, player, targetNode, node));
		}
	}

	private void activateAttackMode(final Entity enemyAtNode, final List<MapGraphNode> availableNodes) {
		character.get(getSystemsCommonData().getPlayer()).setTarget(enemyAtNode);
		for (PlayerSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onAttackModeActivated(availableNodes);
		}
	}

	private void pathHasCreated(MapGraphNode destination, Entity enemyAtNode, AttackNodesHandler attackNodesHandler) {
		if (enemyAtNode != null) {
			enemySelected(destination, enemyAtNode, attackNodesHandler);
		}
		for (PlayerSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onPlayerPathCreated(destination);
		}
	}

	public boolean calculatePathToCharacter(MapGraphNode sourceNode,
											Entity character,
											boolean avoidCharactersInCalculation,
											MapGraphConnectionCosts maxCostPerNodeConnection) {
		playerPathPlanner.getCurrentPath().clear();
		CharacterDecalComponent characterDecalComponent = characterDecal.get(character);
		Vector2 cellPosition = characterDecalComponent.getNodePosition(auxVector2_1);
		MapGraphNode destNode = getSystemsCommonData().getMap().getNode((int) cellPosition.x, (int) cellPosition.y);
		initializePathPlanRequest(sourceNode, destNode, maxCostPerNodeConnection, avoidCharactersInCalculation, playerPathPlanner.getCurrentPath());
		return playerPathPlanner.getPathFinder().searchNodePathBeforeCommand(playerPathPlanner.getHeuristic(), request);
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

	private boolean calculatePathAccordingToSelection(final MapGraphNode cursorNode, Entity enemyAtNode) {
		CharacterDecalComponent charDecalComp = characterDecal.get(getSystemsCommonData().getPlayer());
		MapGraphPath plannedPath = playerPathPlanner.getCurrentPath();
		initializePathPlanRequest(cursorNode, charDecalComp, plannedPath);
		Vector2 cellPosition = charDecalComp.getNodePosition(auxVector2_1);
		MapGraphNode playerNode = getSystemsCommonData().getMap().getNode(cellPosition);
		return calculatePathToEnemy(enemyAtNode, playerNode)
				|| calculatePath(request, playerPathPlanner.getPathFinder(), playerPathPlanner.getHeuristic());
	}

	private boolean calculatePathToEnemy(Entity enemyAtNode, MapGraphNode playerNode) {
		return enemyAtNode != null
				&& character.get(enemyAtNode).getSkills().getHealthData().getHp() > 0
				&& calculatePathToCharacter(playerNode, enemyAtNode, true, CLEAN);
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

	private void applyPlayerCommandAccordingToPlan(MapGraphNode destination, AttackNodesHandler attackNodesHandler) {
		playerPathPlanner.hideAllArrows();
		SystemsCommonData commonData = getSystemsCommonData();
		CharacterDecalComponent charDecalComp = characterDecal.get(commonData.getPlayer());
		MapGraphNode playerNode = commonData.getMap().getNode(charDecalComp.getNodePosition(auxVector2_1));
		if (attackNodesHandler.getSelectedAttackNode() == null) {
			applyCommandWhenNoAttackNodeSelected(commonData, playerNode, destination);
		} else {
			applyPlayerMeleeCommand(destination, playerNode, attackNodesHandler);
		}
	}

	private void applyCommandWhenNoAttackNodeSelected(SystemsCommonData commonData,
													  MapGraphNode playerNode,
													  MapGraphNode destination) {
		if (commonData.getItemToPickup() != null || isPickupAndPlayerOnSameNode(commonData.getMap(), playerNode)) {
			applyPlayerCommand(PICKUP, commonData.getItemToPickup(), destination);
		} else if (destination.getDoor() != null) {
			applyPlayerCommand(OPEN_DOOR, null, destination);
		} else {
			applyGoToCommand(playerPathPlanner.getCurrentPath());
		}
	}

	private boolean isNodeInAvailableNodes(final MapGraphNode node, final List<MapGraphNode> availableNodes) {
		boolean result = false;
		for (MapGraphNode availableNode : availableNodes) {
			if (availableNode.equals(node)) {
				result = true;
				break;
			}
		}
		return result;
	}

	private void applyPlayerMeleeCommand(final MapGraphNode targetNode,
										 final MapGraphNode playerNode,
										 final AttackNodesHandler attackNodesHandler) {
		MapGraphNode attackNode = attackNodesHandler.getSelectedAttackNode();
		boolean result = targetNode.equals(attackNode);
		MapGraph map = getSystemsCommonData().getMap();
		result |= isNodeInAvailableNodes(targetNode, map.getAvailableNodesAroundNode(attackNode));
		result |= targetNode.equals(attackNode) && playerNode.isConnectedNeighbour(attackNode);
		if (result) {
			calculatePathToMelee(targetNode, map);
		}
		deactivateAttackMode(attackNodesHandler);
	}

	private void calculatePathToMelee(MapGraphNode targetNode, MapGraph map) {
		if (map.fetchAliveEnemyFromNode(targetNode) != null) {
			Array<MapGraphNode> nodes = playerPathPlanner.getCurrentPath().nodes;
			nodes.removeIndex(nodes.size - 1);
		}
	}

	private void deactivateAttackMode(AttackNodesHandler attackNodesHandler) {
		attackNodesHandler.setSelectedAttackNode(null);
		for (PlayerSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onAttackModeDeactivated();
		}
	}

	private boolean isPickupAndPlayerOnSameNode(MapGraph map, MapGraphNode playerNode) {
		if (getSystemsCommonData().getCurrentHighLightedPickup() == null) return false;
		Entity p = getSystemsCommonData().getCurrentHighLightedPickup();
		GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(p).getModelInstance();
		Vector3 pickupPosition = modelInstance.transform.getTranslation(auxVector3);
		return map.getNode(pickupPosition).equals(playerNode);
	}

	private void applyPlayerCommand(CharacterCommandsDefinitions commandDefinition,
									Object additionalData,
									MapGraphNode destinationNode) {
		Entity player = getSystemsCommonData().getPlayer();
		CharacterCommand command = Pools.get(commandDefinition.getCharacterCommandImplementation()).obtain();
		command.set(commandDefinition, player, additionalData, destinationNode);
		subscribers.forEach(sub -> sub.onPlayerAppliedCommand(command));
	}

	private void applyGoToCommand(final MapGraphPath path) {
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		MapGraph map = systemsCommonData.getMap();
		Entity player = systemsCommonData.getPlayer();
		MapGraphNode playerNode = map.getNode(characterDecal.get(player).getDecal().getPosition());
		if (path.getCount() > 0 && !playerNode.equals(path.get(path.getCount() - 1))) {
			applyPlayerCommand(RUN, playerPathPlanner.getCurrentPath(), path.get(1));
		}
	}

	@Override
	public Class<PlayerSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return PlayerSystemEventsSubscriber.class;
	}

	@Override
	public void initializeData( ) {
		playerPathPlanner = new PathPlanHandler(getAssetsManager(), getSystemsCommonData().getMap());
		playerPathPlanner.init((PooledEngine) getEngine());
		if (!getLifeCycleHandler().isInGame()) {
			changePlayerStatus(true);
		}
		refreshFogOfWar();
	}

	private void changePlayerStatus(final boolean disabled) {
		PlayerComponent playerComponent = ComponentsMapper.player.get(getSystemsCommonData().getPlayer());
		playerComponent.setDisabled(disabled);
		subscribers.forEach(subscriber -> subscriber.onPlayerStatusChanged(disabled));
	}

	@Override
	public void addedToEngine(Engine engine) {
		super.addedToEngine(engine);
		getSystemsCommonData().setPlayer(getEngine().getEntitiesFor(Family.all(PlayerComponent.class).get()).first());
		Weapon weapon = initializeStartingWeapon();
		getSystemsCommonData().getStorage().setSelectedWeapon(weapon);
	}

	private Weapon initializeStartingWeapon( ) {
		Weapon weapon = Pools.obtain(Weapon.class);
		Texture image = getAssetsManager().getTexture(DefaultGameSettings.STARTING_WEAPON.getSymbol());
		weapon.init(DefaultGameSettings.STARTING_WEAPON, 0, 0, image);
		return weapon;
	}

	@Override
	public void dispose( ) {
		getSystemsCommonData().getStorage().clear();
	}
}
