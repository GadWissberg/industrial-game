package com.gadarts.industrial.systems.enemy.ai;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.character.CharacterRotationData;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.player.PathPlanHandler;

import static com.gadarts.industrial.systems.enemy.ai.EnemyAiStatus.SEARCHING_WONDERING;

public class AiStatusSearchingLookingLogic extends AiStatusLogic {
	public static final float SEARCHING_LOOKING_STATUS_ROTATION_INTERVAL = 500F;

	@Override
	public boolean run(Entity enemy, MapGraph map, PathPlanHandler pathPlanner) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(enemy);
		CharacterRotationData rotationData = characterComponent.getRotationData();
		boolean intervalPassed = TimeUtils.timeSinceMillis(rotationData.getLastRotation()) >= SEARCHING_LOOKING_STATUS_ROTATION_INTERVAL;
		Entity floorEntity = map.getNode(ComponentsMapper.characterDecal.get(enemy).getDecal().getPosition()).getEntity();
		if (intervalPassed || !ComponentsMapper.floor.get(floorEntity).isRevealed()) {
			EnemyComponent enemyComponent = ComponentsMapper.enemy.get(enemy);
			if (enemyComponent.getInitialSearchingLookingDirection() != characterComponent.getFacingDirection()) {
				Vector2 direction = characterComponent.getFacingDirection().getDirection(auxVector2_1);
				Direction newDirection = Direction.findDirection(direction.rotateDeg(45F));
				characterComponent.setFacingDirection(newDirection);
				rotationData.setLastRotation(TimeUtils.millis());
			} else {
				enemyComponent.setAiStatus(SEARCHING_WONDERING);
			}
		}
		return false;
	}
}
