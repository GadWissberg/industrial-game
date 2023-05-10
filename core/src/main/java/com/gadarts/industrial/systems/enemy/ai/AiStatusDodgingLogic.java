package com.gadarts.industrial.systems.enemy.ai;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.systems.player.PathPlanHandler;

public class AiStatusDodgingLogic extends AiStatusLogic {
	@Override
	public boolean run(Entity enemy, MapGraph map, PathPlanHandler pathPlanner, long currentTurnId) {
		ComponentsMapper.enemy.get(enemy).setAiStatus(EnemyAiStatus.SEARCHING_LOOKING);
		return true;
	}
}
