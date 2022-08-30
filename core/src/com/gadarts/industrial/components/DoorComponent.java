package com.gadarts.industrial.components;

import com.gadarts.industrial.map.MapGraphNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoorComponent implements GameComponent {

	private int openCounter;
	private DoorStates state;
	@Setter(AccessLevel.NONE)
	private MapGraphNode node;

	@Override
	public void reset( ) {

	}

	public void init(MapGraphNode node) {
		this.node = node;
		this.state = DoorStates.CLOSED;
	}

	public enum DoorStates {
		OPEN, CLOSED, OPENING, CLOSING
	}
}
