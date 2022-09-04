package com.gadarts.industrial.map;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.shared.model.Coords;
import com.gadarts.industrial.shared.model.map.MapNodesTypes;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.EnvironmentObjectComponent;
import com.gadarts.industrial.components.PickUpComponent;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import com.gadarts.industrial.utils.GameUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MapGraph implements IndexedGraph<MapGraphNode> {
	private static final Vector3 auxVector3 = new Vector3();
	private static final Array<Connection<MapGraphNode>> auxConnectionsList = new Array<>();
	private static final float PASSABLE_MAX_HEIGHT_DIFF = 0.3f;
	private final static Vector2 auxVector2 = new Vector2();
	private static final List<MapGraphNode> auxNodesList_1 = new ArrayList<>();
	private static final List<MapGraphNode> auxNodesList_2 = new ArrayList<>();
	@Getter
	private final float ambient;
	private final Dimension mapSize;
	@Getter
	private final Array<MapGraphNode> nodes;
	private final ImmutableArray<Entity> pickupEntities;
	private final ImmutableArray<Entity> enemiesEntities;
	private final ImmutableArray<Entity> characterEntities;
	private final ImmutableArray<Entity> obstacleEntities;
	@Setter(AccessLevel.PACKAGE)
	@Getter(AccessLevel.PACKAGE)
	MapGraphNode currentPathFinalDestination;
	@Setter
	private MapGraphConnectionCosts maxConnectionCostInSearch;
	@Setter(AccessLevel.PACKAGE)
	@Getter(AccessLevel.PACKAGE)
	private Entity currentCharacterPathPlanner;
	@Setter
	private boolean includeEnemiesInGetConnections = true;

	public MapGraph(Dimension mapSize, PooledEngine engine, float ambient) {
		this.ambient = ambient;
		this.characterEntities = engine.getEntitiesFor(Family.all(CharacterComponent.class).get());
		this.obstacleEntities = engine.getEntitiesFor(Family.all(EnvironmentObjectComponent.class).get());
		this.enemiesEntities = engine.getEntitiesFor(Family.all(EnemyComponent.class).get());
		this.mapSize = mapSize;
		this.nodes = new Array<>(mapSize.width * mapSize.height);
		for (int row = 0; row < mapSize.height; row++) {
			for (int col = 0; col < mapSize.width; col++) {
				nodes.add(new MapGraphNode(col, row, MapNodesTypes.values()[MapNodesTypes.PASSABLE_NODE.ordinal()], 8));
			}
		}
		this.pickupEntities = engine.getEntitiesFor(Family.all(PickUpComponent.class).get());
	}

	public Entity fetchAliveEnemyFromNode(final MapGraphNode node) {
		Entity result = null;
		for (Entity enemy : enemiesEntities) {
			MapGraphNode enemyNode = getNode(ComponentsMapper.characterDecal.get(enemy).getDecal().getPosition());
			if (ComponentsMapper.character.get(enemy).getSkills().getHealthData().getHp() > 0 && enemyNode.equals(node)) {
				result = enemy;
				break;
			}
		}
		return result;
	}

	private void getThreeBehind(final MapGraphNode node, final List<MapGraphNode> output) {
		int x = node.getCol();
		int y = node.getRow();
		if (y > 0) {
			if (x > 0) {
				output.add(getNode(x - 1, y - 1));
			}
			output.add(getNode(x, y - 1));
			if (x < mapSize.width - 1) {
				output.add(getNode(x + 1, y - 1));
			}
		}
	}

	private void getThreeInFront(final MapGraphNode node, final List<MapGraphNode> output) {
		int x = node.getCol();
		int y = node.getRow();
		if (y < mapSize.height - 1) {
			if (x > 0) {
				output.add(getNode(x - 1, y + 1));
			}
			output.add(getNode(x, y + 1));
			if (x < mapSize.width - 1) {
				output.add(getNode(x + 1, y + 1));
			}
		}
	}

	public java.util.List<MapGraphNode> getNodesAround(final MapGraphNode node, final List<MapGraphNode> output) {
		output.clear();
		getThreeBehind(node, output);
		getThreeInFront(node, output);
		if (node.getCol() > 0) {
			output.add(getNode(node.getCol() - 1, node.getRow()));
		}
		if (node.getCol() < mapSize.width - 1) {
			output.add(getNode(node.getCol() + 1, node.getRow()));
		}
		return output;
	}

	public Entity getPickupFromNode(final MapGraphNode node) {
		Entity result = null;
		for (Entity pickup : pickupEntities) {
			ModelInstance modelInstance = ComponentsMapper.modelInstance.get(pickup).getModelInstance();
			MapGraphNode pickupNode = getNode(modelInstance.transform.getTranslation(auxVector3));
			if (pickupNode.equals(node)) {
				result = pickup;
				break;
			}
		}
		return result;
	}

	public MapGraphNode getRayNode(final int screenX, final int screenY, final Camera camera) {
		Vector3 output = GameUtils.calculateGridPositionFromMouse(camera, screenX, screenY, auxVector3);
		output.set(Math.max(output.x, 0), Math.max(output.y, 0), Math.max(output.z, 0));
		return getNode(output);
	}

	public MapGraphNode getNode(final Vector3 position) {
		return getNode((int) position.x, (int) position.z);
	}

	public MapGraphNode getNode(final int col, final int row) {
		if (col < 0 || col >= getWidth() || row < 0 || row >= getDepth()) return null;

		int index = row * mapSize.width + col;
		MapGraphNode result = null;
		if (0 <= index && index < getWidth() * getDepth()) {
			result = nodes.get(index);
		}
		return result;
	}

	public int getDepth( ) {
		return mapSize.height;
	}

	public int getWidth( ) {
		return mapSize.width;
	}

	public MapGraphNode getNode(final Vector2 position) {
		return getNode((int) position.x, (int) position.y);
	}

	@Override
	public int getIndex(MapGraphNode node) {
		return node.getIndex(mapSize);
	}

	@Override
	public int getNodeCount( ) {
		return nodes.size;
	}

	@Override
	public Array<Connection<MapGraphNode>> getConnections(MapGraphNode fromNode) {
		auxConnectionsList.clear();
		Array<MapGraphConnection> connections = fromNode.getConnections();
		for (Connection<MapGraphNode> connection : connections) {
			checkIfConnectionIsAvailable(connection);
		}
		return auxConnectionsList;
	}

	public boolean checkIfNodeIsFreeOfAliveCharacters(MapGraphNode destinationNode) {
		return checkIfNodeIsFreeOfAliveCharactersAndClosedDoors(destinationNode, null, false);
	}

	public boolean checkIfNodeIsFreeOfAliveCharactersAndClosedDoors(MapGraphNode destinationNode) {
		return checkIfNodeIsFreeOfAliveCharactersAndClosedDoors(destinationNode, null, true);
	}

	public boolean checkIfNodeIsFreeOfAliveCharactersAndClosedDoors(MapGraphNode destinationNode,
																	MapGraphNode pathFinalNode) {
		return checkIfNodeIsFreeOfAliveCharactersAndClosedDoors(destinationNode, pathFinalNode, true);
	}

	public boolean checkIfNodeIsFreeOfAliveCharactersAndClosedDoors(MapGraphNode destinationNode,
																	MapGraphNode pathFinalNode,
																	boolean includeClosedDoors) {
		Entity door = destinationNode.getDoor();
		if (pathFinalNode != null && pathFinalNode.equals(destinationNode)) return true;
		if (includeClosedDoors
				&& door != null
				&& ComponentsMapper.door.get(door).getState() != DoorComponent.DoorStates.OPEN) return false;

		for (Entity c : characterEntities) {
			MapGraphNode node = getNode(ComponentsMapper.characterDecal.get(c).getNodePosition(auxVector2));
			int hp = ComponentsMapper.character.get(c).getSkills().getHealthData().getHp();
			if (hp > 0 && node.equals(destinationNode)) {
				return false;
			}
		}

		return true;
	}

	private boolean isNodeRevealed(MapGraphNode node) {
		return node.getEntity() != null && ComponentsMapper.modelInstance.get(node.getEntity()).getFlatColor() == null;
	}

	private void checkIfConnectionIsAvailable(final Connection<MapGraphNode> connection) {
		boolean available = true;
		if (includeEnemiesInGetConnections) {
			available = checkIfNodeIsFreeOfAliveCharactersAndClosedDoors(connection.getToNode(), currentPathFinalDestination);
		}
		boolean validCost = connection.getCost() <= maxConnectionCostInSearch.getCostValue();
		if (available && validCost && checkIfConnectionPassable(connection)) {
			auxConnectionsList.add(connection);
		}
	}

	public MapGraphConnection findConnection(MapGraphNode node1, MapGraphNode node2) {
		if (node1 == null || node2 == null) return null;
		MapGraphConnection result = findConnectionBetweenTwoNodes(node1, node2);
		if (result == null) {
			result = findConnectionBetweenTwoNodes(node2, node1);
		}
		return result;
	}

	public List<MapGraphNode> getAvailableNodesAroundNode(final MapGraphNode node) {
		auxNodesList_1.clear();
		auxNodesList_2.clear();
		List<MapGraphNode> nodesAround = getNodesAround(node, auxNodesList_1);
		List<MapGraphNode> availableNodes = auxNodesList_2;
		for (MapGraphNode nearbyNode : nodesAround) {
			if (nearbyNode.getType() == MapNodesTypes.PASSABLE_NODE && fetchAliveEnemyFromNode(nearbyNode) == null) {
				availableNodes.add(nearbyNode);
			}
		}
		return availableNodes;
	}

	private MapGraphConnection findConnectionBetweenTwoNodes(MapGraphNode src, MapGraphNode dst) {
		Array<MapGraphConnection> connections = src.getConnections();
		for (MapGraphConnection connection : connections) {
			if (connection.getToNode() == dst) {
				return connection;
			}
		}
		return null;
	}

	private boolean checkIfConnectionPassable(final Connection<MapGraphNode> con) {
		if (currentCharacterPathPlanner != null
				&& ComponentsMapper.player.has(currentCharacterPathPlanner)
				&& !isNodeRevealed(con.getToNode()))
			return false;

		MapGraphNode fromNode = con.getFromNode();
		MapGraphNode toNode = con.getToNode();
		boolean result = fromNode.getType() == MapNodesTypes.PASSABLE_NODE && toNode.getType() == MapNodesTypes.PASSABLE_NODE;
		result &= Math.abs(fromNode.getCol() - toNode.getCol()) < 2 && Math.abs(fromNode.getRow() - toNode.getRow()) < 2;
		if ((fromNode.getCol() != toNode.getCol()) && (fromNode.getRow() != toNode.getRow())) {
			result &= getNode(fromNode.getCol(), toNode.getRow()).getType() != MapNodesTypes.OBSTACLE_KEY_DIAGONAL_FORBIDDEN;
			result &= getNode(toNode.getCol(), fromNode.getRow()).getType() != MapNodesTypes.OBSTACLE_KEY_DIAGONAL_FORBIDDEN;
		}
		return result;
	}

	public MapGraphNode getNode(final Coords coord) {
		return getNode(coord.getCol(), coord.getRow());
	}

	private boolean isDiagonalBlockedWithEastOrWest(final MapGraphNode source, final int col) {
		float east = getNode(col, source.getRow()).getHeight();
		return Math.abs(source.getHeight() - east) > PASSABLE_MAX_HEIGHT_DIFF;
	}

	private boolean isDiagonalBlockedWithNorthAndSouth(final MapGraphNode target,
													   final int srcX,
													   final int srcY,
													   final float srcHeight) {
		if (srcY < target.getRow()) {
			float bottom = getNode(srcX, srcY + 1).getHeight();
			return Math.abs(srcHeight - bottom) > PASSABLE_MAX_HEIGHT_DIFF;
		} else {
			float top = getNode(srcX, srcY - 1).getHeight();
			return Math.abs(srcHeight - top) > PASSABLE_MAX_HEIGHT_DIFF;
		}
	}

	private boolean isDiagonalPossible(final MapGraphNode source, final MapGraphNode target) {
		if (source.getCol() == target.getCol() || source.getRow() == target.getRow()) return true;
		if (source.getCol() < target.getCol()) {
			if (isDiagonalBlockedWithEastOrWest(source, source.getCol() + 1)) {
				return false;
			}
		} else if (isDiagonalBlockedWithEastOrWest(source, source.getCol() - 1)) {
			return false;
		}
		return !isDiagonalBlockedWithNorthAndSouth(target, source.getCol(), source.getRow(), source.getHeight());
	}

	private void addConnection(final MapGraphNode source, final int xOffset, final int yOffset) {
		MapGraphNode target = getNode(source.getCol() + xOffset, source.getRow() + yOffset);
		if (target.getType() == MapNodesTypes.PASSABLE_NODE && isDiagonalPossible(source, target)) {
			MapGraphConnection connection;
			if (Math.abs(source.getHeight() - target.getHeight()) <= PASSABLE_MAX_HEIGHT_DIFF) {
				connection = new MapGraphConnection(source, target, MapGraphConnectionCosts.CLEAN);
			} else {
				connection = new MapGraphConnection(source, target, MapGraphConnectionCosts.HEIGHT_DIFF);
			}
			source.getConnections().add(connection);
		}
	}

	void applyConnections( ) {
		for (int row = 0; row < mapSize.height; row++) {
			int rows = row * mapSize.width;
			for (int col = 0; col < mapSize.width; col++) {
				MapGraphNode n = nodes.get(rows + col);
				if (col > 0) addConnection(n, -1, 0);
				if (col > 0 && row < mapSize.height - 1) addConnection(n, -1, 1);
				if (col > 0 && row > 0) addConnection(n, -1, -1);
				if (row > 0) addConnection(n, 0, -1);
				if (row > 0 && col < mapSize.width - 1) addConnection(n, 1, -1);
				if (col < mapSize.width - 1) addConnection(n, 1, 0);
				if (col < mapSize.width - 1 && row < mapSize.height - 1) addConnection(n, 1, 1);
				if (row < mapSize.height - 1) addConnection(n, 0, 1);
			}
		}
	}

	public void init( ) {
		applyConnections();
	}

	public Entity findObstacleByNode(final MapGraphNode node) {
		Entity result = null;
		for (Entity obstacle : obstacleEntities) {
			ModelInstance modelInstance = ComponentsMapper.modelInstance.get(obstacle).getModelInstance();
			MapGraphNode pickupNode = getNode(modelInstance.transform.getTranslation(auxVector3));
			if (pickupNode.equals(node)) {
				result = obstacle;
				break;
			}
		}
		return result;
	}

	public Entity fetchObstacleFromNode(MapGraphNode node) {
		Entity result = null;
		for (Entity obstacle : obstacleEntities) {
			GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(obstacle).getModelInstance();
			MapGraphNode obstacleNode = getNode(modelInstance.transform.getTranslation(auxVector3));
			if (obstacleNode.equals(node)) {
				result = obstacle;
				break;
			}
		}
		return result;
	}
}

