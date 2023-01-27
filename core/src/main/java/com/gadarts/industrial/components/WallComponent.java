package com.gadarts.industrial.components;

import com.gadarts.industrial.map.MapGraphNode;
import lombok.Getter;

@Getter
public class WallComponent implements GameComponent {
	private MapGraphNode parentNode;

	public void init(final MapGraphNode parentNode) {
		this.parentNode = parentNode;
	}

	@Override
	public void reset() {

	}
}
