package com.gadarts.industrial.components;

import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.model.env.DoorsDefinitions;
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
	private DoorsDefinitions definition;

	@Override
	public void reset( ) {

	}

	public void init(MapGraphNode node, DoorsDefinitions definition) {
		this.node = node;
		this.state = DoorStates.CLOSED;
		this.definition = definition;
	}

	public enum DoorStates {
		OPEN, CLOSED, OPENING, CLOSING
	}
}
