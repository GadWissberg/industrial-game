package com.gadarts.industrial.map;

import com.badlogic.ashley.core.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalculatePathRequest {
	private MapGraphNode sourceNode;
	private MapGraphNode destNode;
	private boolean avoidCharactersInCalculations;
	private MapGraphConnectionCosts maxCostInclusive;
	private MapGraphPath outputPath;
	private Entity requester;


}
