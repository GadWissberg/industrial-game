package com.gadarts.industrial.components.floor;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.components.GameComponent;
import com.gadarts.industrial.map.MapGraphNode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class FloorComponent implements GameComponent {
	
	@Getter
	private final List<Entity> nearbyCharacters = new ArrayList<>();

	@Setter
	private int fogOfWarSignature;
	private MapGraphNode node;

	@Setter
	private boolean revealCalculated;
	@Override
	public void reset() {

	}

	public void init(MapGraphNode node) {
		this.node = node;
	}
}
