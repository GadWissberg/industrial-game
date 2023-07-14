package com.gadarts.industrial.map;

import com.badlogic.ashley.core.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalculatePathRequest {
	private MapGraphNode sourceNode;
	private MapGraphNode destNode;
	private MapGraphConnectionCosts maxCostInclusive;
	private Entity requester;

	@Setter(AccessLevel.NONE)
	private MapGraphPath outputPath = new MapGraphPath();


	public void setOutputPath(MapGraphPath outputPath) {
		this.outputPath.set(outputPath);
	}
}
