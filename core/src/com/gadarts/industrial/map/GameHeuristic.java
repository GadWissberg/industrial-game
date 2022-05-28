package com.gadarts.industrial.map;

import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.math.Vector2;

public class GameHeuristic implements Heuristic<MapGraphNode> {
	private static final Vector2 auxVector = new Vector2();

	@Override
	public float estimate(final MapGraphNode node, final MapGraphNode endNode) {
		return auxVector.set(node.getCol(), node.getRow()).dst2(endNode.getCol(), endNode.getRow());
	}
}
