package com.gadarts.industrial.components;

import com.badlogic.ashley.core.Entity;
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
	@Setter(AccessLevel.NONE)
	private Entity openRequestor;

	@Override
	public void reset( ) {

	}

	public void init(MapGraphNode node, DoorsDefinitions definition) {
		this.node = node;
		this.state = DoorStates.CLOSED;
		this.definition = definition;
	}

	public void requestToOpen(Entity requestor) {
		this.openRequestor = requestor;
	}

	public void clearOpenRequestor( ) {
		this.openRequestor = null;
	}

	public enum DoorStates {
		OPEN, CLOSED, OPENING, CLOSING
	}
}
