package com.gadarts.industrial.map;

import com.badlogic.gdx.ai.pfa.Connection;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MapGraphConnection implements Connection<MapGraphNode> {
	private final MapGraphNode source;
	private final MapGraphNode dest;
	private final MapGraphConnectionCosts cost;

	@Override
	public float getCost( ) {
		return cost.getCostValue();
	}

	@Override
	public MapGraphNode getFromNode( ) {
		return source;
	}

	@Override
	public MapGraphNode getToNode( ) {
		return dest;
	}

}
