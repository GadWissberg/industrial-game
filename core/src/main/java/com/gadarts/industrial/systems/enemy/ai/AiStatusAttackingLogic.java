package com.gadarts.industrial.systems.enemy.ai;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.enemy.EnemySystemEventsSubscriber;
import com.gadarts.industrial.systems.player.PathPlanHandler;

import java.util.List;

import static com.gadarts.industrial.systems.SystemsCommonData.MELEE_ATTACK_MAX_HEIGHT;

public class AiStatusAttackingLogic extends AiStatusLogic {
	@Override
	public boolean run(Entity enemy,
					   MapGraph map,
					   PathPlanHandler pathPlanner,
					   long currentTurnId,
					   List<EnemySystemEventsSubscriber> subscribers) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(enemy);
		boolean finishedTurn = false;
		if (characterComponent.getPrimaryAttack().melee()) {
			CharacterDecalComponent characterDecalComponent = ComponentsMapper.characterDecal.get(characterComponent.getTarget());
			MapGraphNode targetNode = map.getNode(characterDecalComponent.getDecal().getPosition());
			MapGraphNode node = map.getNode(ComponentsMapper.characterDecal.get(enemy).getDecal().getPosition());
			if (map.areNodesAdjacent(node, targetNode, MELEE_ATTACK_MAX_HEIGHT)) {
				if (characterComponent.getAttributes().getActionPoints() >= characterComponent.getPrimaryAttack().actionPointsConsumption()) {
					addCommand(enemy, CharacterCommandsDefinitions.ATTACK_PRIMARY, request.getOutputPath());
				} else {
					finishedTurn = true;
				}
			} else {
				if (planPath(enemy, map, pathPlanner, targetNode)) {
					goToTargetAndAttack(enemy);
				} else {
					finishedTurn = true;
				}
			}
		} else {
			EnemyComponent enemyComponent = ComponentsMapper.enemy.get(enemy);
			if (enemyComponent.getEngineEnergy() >= characterComponent.getPrimaryAttack().engineConsumption()) {
				addCommand(enemy, CharacterCommandsDefinitions.ATTACK_PRIMARY);
			} else {
				updateEnemyAiStatus(enemy, EnemyAiStatus.RUNNING_TO_LAST_SEEN_POSITION, subscribers);
			}
		}
		return finishedTurn;
	}

	private void goToTargetAndAttack(Entity enemy) {
		addCommand(enemy, CharacterCommandsDefinitions.RUN, request.getOutputPath());
		addCommand(enemy, CharacterCommandsDefinitions.ATTACK_PRIMARY, request.getOutputPath());
	}


}
