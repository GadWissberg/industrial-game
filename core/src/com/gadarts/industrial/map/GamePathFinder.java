package com.gadarts.industrial.map;

import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;

public class GamePathFinder extends IndexedAStarPathFinder<MapGraphNode> {
	private final MapGraph map;

	public GamePathFinder(final MapGraph graph) {
		super(graph);
		this.map = graph;
	}

	public boolean searchNodePathBeforeCommand(final GameHeuristic heuristic,
											   final CalculatePathRequest req) {
		MapGraphNode oldDest = map.getCurrentPathFinalDestination();
		map.setIncludeEnemiesInGetConnections(req.isAvoidCharactersInCalculations());
		map.setCurrentPathFinalDestination(req.getDestNode());
		map.setMaxConnectionCostInSearch(req.getMaxCostInclusive());
		map.setCurrentCharacterPathPlanner(req.getRequester());
		boolean result = searchNodePath(req.getSourceNode(), req.getDestNode(), heuristic, req.getOutputPath());
		map.setMaxConnectionCostInSearch(MapGraphConnectionCosts.CLEAN);
		map.setCurrentPathFinalDestination(oldDest);
		map.setIncludeEnemiesInGetConnections(true);
		return result;
	}
}
