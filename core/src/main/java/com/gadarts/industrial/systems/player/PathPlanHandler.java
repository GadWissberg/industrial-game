package com.gadarts.industrial.systems.player;

import com.gadarts.industrial.map.GameHeuristic;
import com.gadarts.industrial.map.GamePathFinder;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphPath;
import lombok.Getter;

public class PathPlanHandler {

	@Getter
	private final GameHeuristic heuristic;

	@Getter
	private final MapGraphPath currentPath = new MapGraphPath();

	@Getter
	private final GamePathFinder pathFinder;

	public PathPlanHandler(MapGraph map) {
		this.pathFinder = new GamePathFinder(map);
		this.heuristic = new GameHeuristic();
	}

	public void resetPlan( ) {
		currentPath.clear();
	}
}
