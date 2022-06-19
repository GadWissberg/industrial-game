package com.gadarts.industrial.components;

import com.gadarts.industrial.map.MapGraphNode;
import lombok.Getter;
import lombok.Setter;

@Getter
public class DoorComponent implements GameComponent {

	@Setter
	private DoorStates state;
	private MapGraphNode node;

	@Override
	public void reset( ) {

	}

	public void init(MapGraphNode node) {
		this.node = node;
	}

	public enum DoorStates {
		OPEN, CLOSED, OPENING, CLOSING
	}
}
