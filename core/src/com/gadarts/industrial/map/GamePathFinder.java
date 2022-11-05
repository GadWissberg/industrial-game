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
		MapGraphStates mapGraphStates = map.getMapGraphStates();
		MapGraphNode oldDest = mapGraphStates.getCurrentPathFinalDestination();
		mapGraphStates.setIncludeEnemiesInGetConnections(req.isAvoidCharactersInCalculations());
		mapGraphStates.setCurrentPathFinalDestination(req.getDestNode());
		mapGraphStates.setMaxConnectionCostInSearch(req.getMaxCostInclusive());
		mapGraphStates.setCurrentCharacterPathPlanner(req.getRequester());
		boolean result = searchNodePath(req.getSourceNode(), req.getDestNode(), heuristic, req.getOutputPath());
		mapGraphStates.setMaxConnectionCostInSearch(MapGraphConnectionCosts.CLEAN);
		mapGraphStates.setCurrentPathFinalDestination(oldDest);
		mapGraphStates.setIncludeEnemiesInGetConnections(true);
		return result;
	}
}
