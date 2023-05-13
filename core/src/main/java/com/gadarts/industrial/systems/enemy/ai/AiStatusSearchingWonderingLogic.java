package com.gadarts.industrial.systems.enemy.ai;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.player.PathPlanHandler;

import java.util.List;

public class AiStatusSearchingWonderingLogic extends AiStatusLogic {
	@Override
	public boolean run(Entity enemy, MapGraph map, PathPlanHandler pathPlanner, long currentTurnId) {
		boolean finishedTurn = false;
		EnemyComponent enemyComponent = ComponentsMapper.enemy.get(enemy);
		if (ComponentsMapper.character.get(enemy).getCommands().isEmpty()) {
			if (enemyComponent.getWonderingCounter() > 0) {
				finishedTurn = wonderAround(enemy, map, pathPlanner, currentTurnId);
			} else {
				finishedTurn = true;
				enemyComponent.setAiStatus(EnemyAiStatus.IDLE);
			}
		}
		return finishedTurn;
	}

	private boolean wonderAround(Entity enemy, MapGraph map, PathPlanHandler pathPlanner, long currentTurnId) {
		boolean finishedTurn = false;
		CharacterDecalComponent characterDecalComponent = ComponentsMapper.characterDecal.get(enemy);
		MapGraphNode dst = findAvailableNodeAround(map, characterDecalComponent);
		EnemyComponent enemyComponent = ComponentsMapper.enemy.get(enemy);
		if (planPath(enemy, map, pathPlanner, dst)) {
			if (enemyComponent.getWonderingPrevTurnId() != currentTurnId) {
				enemyComponent.setWonderingCounter(enemyComponent.getWonderingCounter() - 1);
				enemyComponent.setWonderingPrevTurnId(currentTurnId);
			}
			addCommand(enemy, CharacterCommandsDefinitions.RUN, request.getOutputPath());
		} else {
			finishedTurn = true;
			enemyComponent.setAiStatus(EnemyAiStatus.IDLE);
		}
		return finishedTurn;
	}


}
