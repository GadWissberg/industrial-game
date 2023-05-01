package com.gadarts.industrial.systems.enemy.ai;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.player.PathPlanHandler;

import java.util.List;

public class AiStatusSearchingWonderingLogic extends AiStatusLogic {
	@Override
	public boolean run(Entity enemy, MapGraph map, PathPlanHandler pathPlanner) {
		CharacterDecalComponent characterDecalComponent = ComponentsMapper.characterDecal.get(enemy);
		Decal decal = characterDecalComponent.getDecal();
		List<MapGraphNode> availableNodes = map.fetchAvailableNodesAroundNode(map.getNode(decal.getPosition()));
		for (int i = 0; i < availableNodes.size(); i++) {
			Vector3 enemyPosition = characterDecalComponent.getDecal().getPosition();
			float enemyNodeHeight = map.getNode(enemyPosition).getHeight();
			float height = availableNodes.get(i).getHeight();
			if (Math.abs(enemyNodeHeight - height) > CharacterComponent.PASSABLE_MAX_HEIGHT_DIFF) {
				availableNodes.remove(i);
				i--;
			}
		}
		int count = availableNodes.size();
		MapGraphNode dst = count > 0 ? availableNodes.get(MathUtils.random(count - 1)) : null;
		boolean finishedTurn = false;
		if (planPath(enemy, map, pathPlanner, dst)) {
			addCommand(enemy, CharacterCommandsDefinitions.RUN, request.getOutputPath());
		} else {
			finishedTurn = true;
		}
		return finishedTurn;
	}
}
