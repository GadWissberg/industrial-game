package com.gadarts.industrial.systems.enemy.ai;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphConnectionCosts;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.map.MapGraphPath;
import com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.player.PathPlanHandler;
import lombok.val;

public class AiStatusDodgingLogic extends AiStatusLogic {
	@Override
	public boolean run(Entity enemy, MapGraph map, PathPlanHandler pathPlanner, long currentTurnId) {
		CharacterDecalComponent characterDecalComponent = ComponentsMapper.characterDecal.get(enemy);
		MapGraphNode dst = findAvailableNodeAround(map, characterDecalComponent);
		var finishedTurn = false;
		if (ComponentsMapper.character.get(enemy).getCommands().isEmpty()) {
			if (dst != null) {
				MapGraphPath outputPath = request.getOutputPath();
				initializePathPlanRequest(dst, characterDecalComponent, MapGraphConnectionCosts.CLEAN, enemy, outputPath, map);
				val found = planPath(enemy, map, pathPlanner, dst);
				if (found) {
					addCommand(enemy, CharacterCommandsDefinitions.DODGE, outputPath);
				} else {
					finishedTurn = true;
				}
			} else {
				finishedTurn = true;
			}
			if (finishedTurn) {
				ComponentsMapper.enemy.get(enemy).setAiStatus(EnemyAiStatus.ATTACKING);
			}
		}
		return finishedTurn;
	}
}
