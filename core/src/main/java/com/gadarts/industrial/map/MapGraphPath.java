package com.gadarts.industrial.map;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;

public class MapGraphPath extends DefaultGraphPath<MapGraphNode> {

	public void set(MapGraphPath path) {
		nodes.clear();
		if (path != null) {
			nodes.addAll(path.nodes);
		}
	}

	@Override
	public String toString( ) {
		return nodes.toString();
	}
}
