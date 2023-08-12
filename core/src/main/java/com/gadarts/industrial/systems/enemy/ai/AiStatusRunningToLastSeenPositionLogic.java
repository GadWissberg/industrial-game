package com.gadarts.industrial.systems.enemy.ai;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.enemy.EnemySystemEventsSubscriber;
import com.gadarts.industrial.systems.player.PathPlanHandler;

import java.util.List;

import static com.gadarts.industrial.systems.enemy.ai.EnemyAiStatus.SEARCHING_LOOKING;

public class AiStatusRunningToLastSeenPositionLogic extends AiStatusLogic {

	@Override
	public boolean run(Entity enemy, MapGraph map, PathPlanHandler pathPlanner, long currentTurnId, List<EnemySystemEventsSubscriber> subscribers) {
		EnemyComponent enemyComp = ComponentsMapper.enemy.get(enemy);
		MapGraphNode targetLastVisibleNode = enemyComp.getTargetLastVisibleNode();
		CharacterDecalComponent charDecalComp = ComponentsMapper.characterDecal.get(enemy);
		MapGraphNode node = map.getNode(charDecalComp.getDecal().getPosition());
		if (node.equals(targetLastVisibleNode)) {
			updateEnemyAiStatus(enemy, SEARCHING_LOOKING, subscribers);
			Direction facingDirection = ComponentsMapper.character.get(enemy).getFacingDirection();
			enemyComp.setInitialSearchingLookingDirection(Direction.findDirection(facingDirection.getDirection(auxVector2_1).rotateDeg(-45)));
		} else {
			if (planPath(enemy, map, pathPlanner, targetLastVisibleNode)) {
				addCommand(enemy, CharacterCommandsDefinitions.RUN, request.getOutputPath());
			} else {
				return true;
			}
		}
		return false;
	}
}
