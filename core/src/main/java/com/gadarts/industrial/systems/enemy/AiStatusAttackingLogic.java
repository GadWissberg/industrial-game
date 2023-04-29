package com.gadarts.industrial.systems.enemy;

import com.badlogic.ashley.core.Entity;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.player.PathPlanHandler;
import com.gadarts.industrial.utils.GameUtils;

import static com.gadarts.industrial.map.MapGraphConnectionCosts.CLEAN;
import static com.gadarts.industrial.systems.SystemsCommonData.MELEE_ATTACK_MAX_HEIGHT;

public class AiStatusAttackingLogic extends AiStatusLogic {
	@Override
	public boolean run(Entity enemy, MapGraph map, PathPlanHandler pathPlanner) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(enemy);
		if (characterComponent.getPrimaryAttack().melee()) {
			CharacterDecalComponent characterDecalComponent = ComponentsMapper.characterDecal.get(characterComponent.getTarget());
			MapGraphNode targetNode = map.getNode(characterDecalComponent.getDecal().getPosition());
			MapGraphNode node = map.getNode(ComponentsMapper.characterDecal.get(enemy).getDecal().getPosition());
			boolean adjacent = map.areNodesAdjacent(node, targetNode, MELEE_ATTACK_MAX_HEIGHT);
			if (adjacent) {
				if (characterComponent.getSkills().getActionPoints() >= characterComponent.getPrimaryAttack().actionPointsConsumption()) {
					addCommand(enemy, CharacterCommandsDefinitions.ATTACK_PRIMARY, request.getOutputPath());
				} else {
					return true;
				}
			} else {
				boolean pathFound = planPath(enemy, map, pathPlanner, targetNode);
				if (pathFound) {
					addCommand(enemy, CharacterCommandsDefinitions.RUN, request.getOutputPath());
					addCommand(enemy, CharacterCommandsDefinitions.ATTACK_PRIMARY, request.getOutputPath());
				} else {
					return true;
				}
			}
		}
		return false;
	}


}
