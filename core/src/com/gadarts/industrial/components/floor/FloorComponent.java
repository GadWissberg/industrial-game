package com.gadarts.industrial.components.floor;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.components.GameComponent;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.Assets;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class FloorComponent implements GameComponent {

	@Getter
	private final List<Entity> nearbySimpleShadows = new ArrayList<>();

	@Setter
	private int fogOfWarSignature;
	private MapGraphNode node;

	@Setter
	private boolean revealCalculated;
	private Assets.SurfaceTextures definition;

	@Override
	public void reset( ) {

	}

	public void init(MapGraphNode node, Assets.SurfaceTextures definition) {
		this.node = node;
		this.definition = definition;
	}

	public boolean isRevealed( ) {
		return (getFogOfWarSignature() & 16) == 0;
	}
}
