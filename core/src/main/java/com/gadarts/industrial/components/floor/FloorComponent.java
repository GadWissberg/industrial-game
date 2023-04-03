package com.gadarts.industrial.components.floor;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.components.GameComponent;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.Assets;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FloorComponent implements GameComponent {

	@Getter
	@Setter(AccessLevel.NONE)
	private final List<Entity> nearbySimpleShadows = new ArrayList<>();
	private int fogOfWarSignature;
	@Setter(AccessLevel.NONE)
	private MapGraphNode node;
	private boolean revealCalculated;
	private boolean discovered;
	@Setter(AccessLevel.NONE)
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
