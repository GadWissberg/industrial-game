package com.gadarts.industrial.systems.enemy.ai;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pools;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.map.*;
import com.gadarts.industrial.systems.character.commands.CharacterCommand;
import com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.player.PathPlanHandler;
import com.gadarts.industrial.utils.GameUtils;

import java.util.List;

import static com.gadarts.industrial.map.MapGraphConnectionCosts.CLEAN;

public abstract class AiStatusLogic {
	static final Vector2 auxVector2_1 = new Vector2();
	static final CalculatePathRequest request = new CalculatePathRequest();

	private void cleanAvailableNodesWithDifferentHeight(MapGraph map,
														CharacterDecalComponent characterDecalComponent,
														List<MapGraphNode> availableNodes) {
		for (int i = 0; i < availableNodes.size(); i++) {
			float enemyNodeHeight = map.getNode(characterDecalComponent.getDecal().getPosition()).getHeight();
			float height = availableNodes.get(i).getHeight();
			if (Math.abs(enemyNodeHeight - height) > CharacterComponent.PASSABLE_MAX_HEIGHT_DIFF) {
				availableNodes.remove(i);
				i--;
			}
		}
	}

	protected MapGraphNode findAvailableNodeAround(MapGraph map, CharacterDecalComponent characterDecalComponent) {
		List<MapGraphNode> availableNodes = map.fetchAvailableNodesAroundNode(map.getNode(characterDecalComponent.getDecal().getPosition()));
		cleanAvailableNodesWithDifferentHeight(map, characterDecalComponent, availableNodes);
		return availableNodes.size() > 0 ? availableNodes.get(MathUtils.random(availableNodes.size() - 1)) : null;
	}

	boolean planPath(Entity enemy, MapGraph map, PathPlanHandler pathPlanner, MapGraphNode targetNode) {
		initializePathPlanRequest(targetNode, ComponentsMapper.characterDecal.get(enemy), CLEAN, enemy, pathPlanner.getCurrentPath(), map);
		return GameUtils.calculatePath(request, pathPlanner.getPathFinder(), pathPlanner.getHeuristic());
	}

	void initializePathPlanRequest(MapGraphNode destinationNode,
								   CharacterDecalComponent charDecalComp,
								   MapGraphConnectionCosts maxCostInclusive,
								   Entity enemy,
								   MapGraphPath pathToDestination,
								   MapGraph map) {
		initializePathPlanRequest(
				map.getNode(charDecalComp.getNodePosition(auxVector2_1)),
				destinationNode,
				maxCostInclusive,
				enemy,
				pathToDestination);
	}

	public abstract boolean run(Entity enemy, MapGraph map, PathPlanHandler pathPlanner, long currentTurnId);

	void addCommand(Entity enemy,
					CharacterCommandsDefinitions characterCommandsDefinitions) {
		addCommand(enemy, characterCommandsDefinitions, null);
	}

	void addCommand(Entity enemy,
					CharacterCommandsDefinitions characterCommandsDefinitions,
					MapGraphPath path) {
		CharacterCommand command = Pools.get(characterCommandsDefinitions.getCharacterCommandImplementation()).obtain();
		command.reset(characterCommandsDefinitions, enemy, path);
		ComponentsMapper.character.get(enemy).getCommands().addLast(command);
	}

	void initializePathPlanRequest(MapGraphNode sourceNode,
								   MapGraphNode destinationNode,
								   MapGraphConnectionCosts maxCostInclusive,
								   Entity character,
								   MapGraphPath outputPath) {
		request.setSourceNode(sourceNode);
		request.setDestNode(destinationNode);
		request.setOutputPath(outputPath);
		request.setMaxCostInclusive(maxCostInclusive);
		request.setRequester(character);
	}
}
