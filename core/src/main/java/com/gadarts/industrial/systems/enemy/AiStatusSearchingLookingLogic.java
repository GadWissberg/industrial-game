package com.gadarts.industrial.systems.enemy;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.TimeUtils;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.character.CharacterRotationData;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.systems.player.PathPlanHandler;

import static com.gadarts.industrial.systems.enemy.EnemyAiStatus.SEARCHING_WONDERING;

public class AiStatusSearchingLookingLogic extends AiStatusLogic {
	public static final float SEARCHING_LOOKING_STATUS_ROTATION_INTERVAL = 500F;

	@Override
	boolean run(Entity enemy, MapGraph map, PathPlanHandler pathPlanner) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(enemy);
		CharacterRotationData rotationData = characterComponent.getRotationData();
		if (TimeUtils.timeSinceMillis(rotationData.getLastRotation()) >= SEARCHING_LOOKING_STATUS_ROTATION_INTERVAL) {
			EnemyComponent enemyComponent = ComponentsMapper.enemy.get(enemy);
			if (enemyComponent.getInitialSearchingLookingDirection() != characterComponent.getFacingDirection()) {
				Direction newDirection = Direction.findDirection(characterComponent.getFacingDirection().getDirection(auxVector2_1).rotateDeg(45F));
				characterComponent.setFacingDirection(newDirection);
				rotationData.setLastRotation(TimeUtils.millis());
			} else {
				enemyComponent.setAiStatus(SEARCHING_WONDERING);
			}
		}
		return false;
	}
}
