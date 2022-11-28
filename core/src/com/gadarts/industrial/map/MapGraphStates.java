package com.gadarts.industrial.map;

import com.badlogic.ashley.core.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public class MapGraphStates {
	@Setter(AccessLevel.PACKAGE)
	@Getter(AccessLevel.PACKAGE)
	private MapGraphNode currentPathFinalDestination;
	@Setter
	private MapGraphConnectionCosts maxConnectionCostInSearch;
	@Setter
	private boolean includeCharactersInGetConnections = true;
	@Setter(AccessLevel.PACKAGE)
	@Getter(AccessLevel.PACKAGE)
	private Entity currentCharacterPathPlanner;

}
