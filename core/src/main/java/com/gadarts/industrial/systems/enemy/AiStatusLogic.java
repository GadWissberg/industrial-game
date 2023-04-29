package com.gadarts.industrial.systems.enemy;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pools;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.map.*;
import com.gadarts.industrial.systems.character.commands.CharacterCommand;
import com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.player.PathPlanHandler;
import com.gadarts.industrial.utils.GameUtils;

import static com.gadarts.industrial.map.MapGraphConnectionCosts.CLEAN;

public abstract class AiStatusLogic {
	static final Vector2 auxVector2_1 = new Vector2();
	static final CalculatePathRequest request = new CalculatePathRequest();

	boolean planPath(Entity enemy, MapGraph map, PathPlanHandler pathPlanner, MapGraphNode targetNode) {
		initializePathPlanRequest(targetNode, ComponentsMapper.characterDecal.get(enemy), CLEAN, enemy, pathPlanner.getCurrentPath(), map);
		return GameUtils.calculatePath(request, pathPlanner.getPathFinder(), pathPlanner.getHeuristic());
	}

	void initializePathPlanRequest(MapGraphNode destinationNode,
								   CharacterDecalComponent charDecalComp,
								   MapGraphConnectionCosts maxCostInclusive,
								   Entity enemy,
								   MapGraphPath outputPath,
								   MapGraph map) {
		initializePathPlanRequest(
				map.getNode(charDecalComp.getNodePosition(auxVector2_1)),
				destinationNode,
				maxCostInclusive,
				true,
				enemy,
				outputPath);
	}

	abstract boolean run(Entity enemy, MapGraph map, PathPlanHandler pathPlanner);

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
								   boolean avoidCharactersInCalculations,
								   Entity character,
								   MapGraphPath outputPath) {
		request.setSourceNode(sourceNode);
		request.setDestNode(destinationNode);
		request.setOutputPath(outputPath);
		request.setAvoidCharactersInCalculations(avoidCharactersInCalculations);
		request.setMaxCostInclusive(maxCostInclusive);
		request.setRequester(character);
	}
}
