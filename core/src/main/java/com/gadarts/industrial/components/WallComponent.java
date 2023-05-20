package com.gadarts.industrial.components;

import com.gadarts.industrial.map.MapGraphNode;
import lombok.Getter;
import lombok.Setter;

@Getter
public class WallComponent implements GameComponent {
	private MapGraphNode parentNode;
	@Setter
	private boolean applyGrayScale;

	public void init(final MapGraphNode parentNode) {
		this.parentNode = parentNode;
	}

	@Override
	public void reset( ) {

	}
}
