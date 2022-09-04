package com.gadarts.industrial.map;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.gadarts.industrial.shared.model.map.MapNodesTypes;
import com.gadarts.industrial.utils.EntityBuilder;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;

@Getter
public class MapGraphNode {
	private final Array<MapGraphConnection> connections;
	private int col;
	private int row;

	@Setter
	private Entity door;
	@Setter
	private int nodeAmbientOcclusionValue;
	@Setter
	private MapNodesTypes type;
	@Setter
	private float height;
	@Setter
	private Entity entity;
	@Setter
	private boolean reachable;

	public MapGraphNode(final int col, final int row, final MapNodesTypes type, final int connections) {
		this.col = col;
		this.row = row;
		this.type = type;
		this.connections = new Array<>(connections);
	}

	@Override
	public String toString( ) {
		return "MapGraphNode{" +
				"x=" + col +
				", y=" + row +
				'}';
	}

	public Vector2 getCenterPosition(final Vector2 output) {
		return output.set(col + 0.5f, row + 0.5f);
	}

	public Vector3 getCenterPosition(final Vector3 output) {
		return output.set(col + 0.5f, height, row + 0.5f);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MapGraphNode that = (MapGraphNode) o;

		if (col != that.col) return false;
		if (row != that.row) return false;
		if (type != that.type) return false;
		return connections.equals(that.connections);
	}

	public int getIndex(final Dimension mapSize) {
		return row * mapSize.width + col;
	}

	public MapGraphNode set(final MapGraphNode newValue) {
		this.col = newValue.col;
		this.row = newValue.row;
		this.type = newValue.type;
		this.connections.clear();
		connections.addAll(newValue.connections);
		return this;
	}

	public boolean isConnectedNeighbour(final MapGraphNode node) {
		boolean result = false;
		for (Connection<MapGraphNode> connection : connections) {
			if (connection.getToNode().equals(node)) {
				result = true;
				break;
			}
		}
		return result;
	}
}
