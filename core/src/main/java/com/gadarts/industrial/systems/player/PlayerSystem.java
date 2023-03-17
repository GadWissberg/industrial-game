package com.gadarts.industrial.systems.player;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.Queue;
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
import com.gadarts.industrial.components.player.Item;
import com.gadarts.industrial.components.player.PlayerComponent;
import com.gadarts.industrial.components.player.Weapon;
import com.gadarts.industrial.map.*;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.Assets.Declarations;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.assets.declarations.weapons.PlayerWeaponDeclaration;
import com.gadarts.industrial.shared.assets.declarations.weapons.PlayerWeaponsDeclarations;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.amb.AmbSystemEventsSubscriber;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.systems.character.commands.CharacterCommand;
import com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.character.commands.CommandStates;
import com.gadarts.industrial.systems.enemy.EnemyAiStatus;
import com.gadarts.industrial.systems.enemy.EnemySystemEventsSubscriber;
import com.gadarts.industrial.systems.input.InputSystemEventsSubscriber;
import com.gadarts.industrial.systems.render.RenderSystemEventsSubscriber;
import com.gadarts.industrial.systems.turns.TurnsSystemEventsSubscriber;
import com.gadarts.industrial.systems.ui.UserInterfaceSystemEventsSubscriber;
import com.gadarts.industrial.utils.GameUtils;

import java.util.LinkedHashSet;

import static com.gadarts.industrial.components.character.CharacterComponent.TURN_DURATION;
import static com.gadarts.industrial.map.MapGraphConnectionCosts.CLEAN;
import static com.gadarts.industrial.shared.model.characters.SpriteType.IDLE;
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
	private static final Queue<CharacterCommand> auxCommandQueue = new Queue<>();
	private PathPlanHandler playerPathPlanner;
	private ImmutableArray<Entity> ambObjects;
	private ImmutableArray<Entity> pickups;

	public PlayerSystem(GameAssetManager assetsManager,
						GameLifeCycleHandler lifeCycleHandler) {
		super(assetsManager, lifeCycleHandler);
	}

	private static void endPlayerTurnOnEnemyAwaken(SystemsCommonData systemsCommonData) {
		Entity currentCharacter = systemsCommonData.getTurnsQueue().first();
		if (ComponentsMapper.player.has(currentCharacter)) {
			Queue<CharacterCommand> commands = ComponentsMapper.character.get(currentCharacter).getCommands();
			if (!commands.isEmpty()) {
				CharacterCommand currentCommand = commands.get(0);
				if (currentCommand.getDefinition() == RUN) {
					currentCommand.setState(CommandStates.ENDED);
					ComponentsMapper.character.get(currentCharacter).getCharacterSpriteData().setSpriteType(IDLE);
				}
			}
		}
	}

	@Override
	public void spaceKeyPressed( ) {
		if (ComponentsMapper.player.has(getSystemsCommonData().getTurnsQueue().first())) {
			notifyPlayerFinishedTurn();
		}
	}

	private CharacterCommand initializeCommand(CharacterCommandsDefinitions commandDefinition,
											   Object additionalData,
											   MapGraphNode destination) {
		CharacterCommand command = Pools.get(commandDefinition.getCharacterCommandImplementation()).obtain();
		command.init(commandDefinition, getSystemsCommonData().getPlayer(), additionalData, destination);
		return command;
	}

	@Override
	public void onCommandInitialized(Entity character, CharacterCommand command) {
		if (!ComponentsMapper.player.has(character)) return;

		for (Entity entity : getSystemsCommonData().getTurnsQueue()) {
			boolean isEnemy = ComponentsMapper.enemy.has(entity);
			if (isEnemy && ComponentsMapper.enemy.get(entity).getAiStatus() == EnemyAiStatus.ATTACKING) {
				command.onInFight();
				return;
			}
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
				notifyPlayerFinishedTurn();
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
					calculateFogOfWarSignature(nearbyNode.getEntity());
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
		MapGraphNode node = ComponentsMapper.floor.get(entity).getNode();
		MapGraph map = getSystemsCommonData().getMap();
		MapGraphNode nearbyNode = map.getNode(node.getCol() + colOffset, node.getRow() + rowOffset);
		boolean result = true;
		if (nearbyNode != null) {
			Entity nearbyNodeEntity = nearbyNode.getEntity();
			if (nearbyNodeEntity != null) {
				result = !DebugSettings.DISABLE_FOW
						&& ComponentsMapper.modelInstance.has(nearbyNodeEntity)
						&& ComponentsMapper.modelInstance.get(nearbyNodeEntity).getFlatColor() != null;
			}
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

	@Override
	public void onEnemyAwaken(Entity enemy, EnemyAiStatus prevAiStatus) {
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		endPlayerTurnOnEnemyAwaken(systemsCommonData);
	}

	private boolean applyLineOfSightOnNode(MapGraph map, MapGraphNode playerNode, boolean blocked, GridPoint2 nodeCoord) {
		MapGraphNode currentNode = map.getNode(nodeCoord.x, nodeCoord.y);
		if (currentNode != null && currentNode.getEntity() != null) {
			FloorComponent floorComponent = ComponentsMapper.floor.get(currentNode.getEntity());
			ModelInstanceComponent modelInstanceComponent = ComponentsMapper.modelInstance.get(currentNode.getEntity());
			if (!floorComponent.isRevealCalculated()) {
				if (modelInstanceComponent != null) {
					modelInstanceComponent.setFlatColor(!DebugSettings.DISABLE_FOW && blocked ? Color.BLACK : null);
				}
				floorComponent.setFogOfWarSignature(blocked ? 16 : 0);
				floorComponent.setRevealCalculated(true);
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
	public void onCharacterFinishedTurn(Entity character) {
		if (ComponentsMapper.player.has(character)) {
			notifyPlayerFinishedTurn();
		}
	}

	@Override
	public void onItemPickedUp(final Entity itemPickedUp) {
		Item item = ComponentsMapper.pickup.get(itemPickedUp).getItem();
		if (getSystemsCommonData().getStorage().addItem(item)) {
			for (PlayerSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onItemAddedToStorage(item);
			}
		}
		getSystemsCommonData().getSoundPlayer().playSound(Assets.Sounds.PICKUP);
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
	public void onUserSelectedNodeToApplyTurn(MapGraphNode node) {
		applyPlayerTurn(node);
	}

	private void applyPlayerTurn(final MapGraphNode cursorNode) {
		planPath(cursorNode);
	}

	private void planPath(final MapGraphNode cursorNode) {
		Entity enemyAtNode = getSystemsCommonData().getMap().fetchAliveCharacterFromNode(cursorNode);
		if (!calculatePathAccordingToSelection(cursorNode, enemyAtNode)) return;

		pathHasCreated(cursorNode, enemyAtNode);
	}

	private void enemySelected(MapGraphNode targetNode, Entity enemyAtNode) {
		ComponentsMapper.character.get(getSystemsCommonData().getPlayer()).setTarget(enemyAtNode);
		Entity targetCharacter = getSystemsCommonData().getMap().fetchAliveCharacterFromNode(targetNode);
		Decal playerDecal = ComponentsMapper.characterDecal.get(getSystemsCommonData().getPlayer()).getDecal();
		if (!getSystemsCommonData().getStorage().getSelectedWeapon().isMelee()) {
			playerPathPlanner.resetPlan();
			selectedEnemyToAttack(targetNode, targetCharacter);
		} else {
			runToMelee(targetNode, targetCharacter, getSystemsCommonData().getMap().getNode(playerDecal.getPosition()));
		}
	}

	private void runToMelee(MapGraphNode targetNode, Entity targetCharacter, MapGraphNode playerNode) {
		calculatePathToEnemy(targetCharacter, playerNode);
		MapGraphPath currentPath = playerPathPlanner.getCurrentPath();
		int pathSize = currentPath.getCount();
		if (!currentPath.nodes.isEmpty() && currentPath.get(pathSize - 1).equals(targetNode)) {
			auxCommandQueue.clear();
			auxCommandQueue.addLast(initializeCommand(RUN, currentPath, currentPath.get(1)));
			auxCommandQueue.addLast(initializeCommand(ATTACK_PRIMARY, targetCharacter, targetNode));
			shrinkRunCommandInBattle();
			subscribers.forEach(sub -> sub.onPlayerAppliedCommand(auxCommandQueue));
		}
	}

	private void selectedEnemyToAttack(final MapGraphNode node, Entity targetCharacter) {
		Entity player = getSystemsCommonData().getPlayer();
		ComponentsMapper.character.get(player).setTarget(targetCharacter);
		auxCommandQueue.clear();
		auxCommandQueue.addLast(initializeCommand(ATTACK_PRIMARY, targetCharacter, node));
		for (PlayerSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onPlayerAppliedCommand(auxCommandQueue);
		}
	}

	private void pathHasCreated(MapGraphNode destination, Entity enemyAtNode) {
		if (enemyAtNode != null) {
			enemySelected(destination, enemyAtNode);
		} else {
			MapGraphPath currentPath = playerPathPlanner.getCurrentPath();
			int pathSize = currentPath.getCount();
			if (!currentPath.nodes.isEmpty() && currentPath.get(pathSize - 1).equals(destination)) {
				applySingleCommand(RUN, playerPathPlanner.getCurrentPath(), playerPathPlanner.getCurrentPath().get(1));
			}
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
		CharacterDecalComponent characterDecalComponent = ComponentsMapper.characterDecal.get(character);
		Vector2 cellPosition = characterDecalComponent.getNodePosition(auxVector2_1);
		MapGraphNode destNode = getSystemsCommonData().getMap().getNode((int) cellPosition.x, (int) cellPosition.y);
		initializePathPlanRequest(sourceNode, destNode, maxCostPerNodeConnection, avoidCharactersInCalculation, playerPathPlanner.getCurrentPath());
		return playerPathPlanner.getPathFinder().searchNodePathBeforeCommand(playerPathPlanner.getHeuristic(), request);
	}

	@Override
	public void onUserLeftClickedThePlayer(MapGraphNode playerNode) {
		for (Entity pickup : pickups) {
			GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(pickup).getModelInstance();
			Vector3 pickupPosition = modelInstance.transform.getTranslation(auxVector3_1);
			if (getSystemsCommonData().getMap().getNode(pickupPosition).equals(playerNode)) {
				applySingleCommand(PICKUP, pickup, playerNode);
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

	private boolean calculatePathAccordingToSelection(final MapGraphNode cursorNode, Entity enemyAtNode) {
		CharacterDecalComponent charDecalComp = ComponentsMapper.characterDecal.get(getSystemsCommonData().getPlayer());
		MapGraphPath plannedPath = playerPathPlanner.getCurrentPath();
		initializePathPlanRequest(cursorNode, charDecalComp, plannedPath);
		Vector2 cellPosition = charDecalComp.getNodePosition(auxVector2_1);
		MapGraphNode playerNode = getSystemsCommonData().getMap().getNode(cellPosition);
		return calculatePathToEnemy(enemyAtNode, playerNode)
				|| calculatePath(request, playerPathPlanner.getPathFinder(), playerPathPlanner.getHeuristic());
	}

	private boolean calculatePathToEnemy(Entity enemyAtNode, MapGraphNode playerNode) {
		return enemyAtNode != null
				&& ComponentsMapper.character.get(enemyAtNode).getSkills().getHealthData().getHp() > 0
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

	private void applySingleCommand(CharacterCommandsDefinitions commandDefinition,
									Object additionalData,
									MapGraphNode destinationNode) {
		MapGraph map = getSystemsCommonData().getMap();
		Entity player = getSystemsCommonData().getPlayer();
		MapGraphNode playerNode = map.getNode(ComponentsMapper.characterDecal.get(player).getDecal().getPosition());
		MapGraphPath path = playerPathPlanner.getCurrentPath();
		if (!commandDefinition.isRequiresMovement() || path.getCount() > 0 && !playerNode.equals(path.get(path.getCount() - 1))) {
			CharacterCommand command = initializeCommand(commandDefinition, additionalData, destinationNode);
			auxCommandQueue.clear();
			auxCommandQueue.addLast(command);
			shrinkRunCommandInBattle();
			subscribers.forEach(sub -> sub.onPlayerAppliedCommand(auxCommandQueue));
		}
	}

	private void shrinkRunCommandInBattle( ) {
		Array<MapGraphNode> nodes = playerPathPlanner.getCurrentPath().nodes;
		if (nodes.size > 2 && isTurnsQueueHasVisibleEnemies()) {
			nodes.removeRange(2, nodes.size - 1);
		}
	}

	private boolean isTurnsQueueHasVisibleEnemies( ) {
		Queue<Entity> turnsQueue = getSystemsCommonData().getTurnsQueue();
		if (turnsQueue.size <= 1) return false;

		for (int i = 0; i < turnsQueue.size; i++) {
			Entity entity = turnsQueue.get(i);
			if (ComponentsMapper.enemy.has(entity)) {
				Decal decal = ComponentsMapper.characterDecal.get(entity).getDecal();
				MapGraphNode enemyNode = getSystemsCommonData().getMap().getNode(decal.getPosition());
				if (enemyNode.getEntity() != null) {
					if ((ComponentsMapper.floor.get(enemyNode.getEntity()).getFogOfWarSignature() & 16) == 0) {
						return true;
					}
				}
			}
		}
		return false;
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
		subscribers.forEach(subscriber -> subscriber.onPlayerStatusChanged(disabled));
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
		PlayerWeaponsDeclarations weaponsDeclarations = (PlayerWeaponsDeclarations) am.getDeclaration(Declarations.PLAYER_WEAPONS);
		PlayerWeaponDeclaration declaration = weaponsDeclarations.parse(DebugSettings.STARTING_WEAPON);
		Assets.UiTextures symbol = declaration.declaration().getSymbol();
		Texture image = symbol != null ? am.getTexture(symbol) : null;
		weapon.init(declaration.declaration(), 0, 0, image);
		return weapon;
	}

	@Override
	public void dispose( ) {
		getSystemsCommonData().getStorage().clear();
	}
}
