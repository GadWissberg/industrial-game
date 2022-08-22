package com.gadarts.industrial.systems.enemy;

import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import com.gadarts.industrial.shared.model.characters.enemies.Enemies;
import com.gadarts.industrial.shared.model.characters.enemies.WeaponsDefinitions;
import com.gadarts.industrial.utils.GameUtils;

import java.util.List;

import static com.gadarts.industrial.systems.enemy.EnemyAiStatus.DODGING;

public class PrimaryAttackValidations {
	public static List<PrimaryAttackValidation> primaryAttackValidations = List.of(
			new PrimaryAttackValidation(
					(entity, enemySystem) -> {
						EnemyComponent enemyComp = ComponentsMapper.enemy.get(entity);
						WeaponsDefinitions primaryAttack = enemyComp.getEnemyDefinition().getPrimaryAttack();
						return enemyComp.getEngineEnergy() >= primaryAttack.getEngineConsumption();
					},
					(entity, enemySystem) -> {
						ComponentsMapper.enemy.get(entity).setAiStatus(DODGING);
						enemySystem.invokeEnemyTurn(entity);
					}),
			new PrimaryAttackValidation(
					(entity, enemySystem) -> {
						Enemies enemyDefinition = ComponentsMapper.enemy.get(entity).getEnemyDefinition();
						WeaponsDefinitions primaryAttack = enemyDefinition.getPrimaryAttack();
						return ComponentsMapper.character.get(entity).getTurnTimeLeft() >= primaryAttack.getDuration();
					},
					null),
			new PrimaryAttackValidation(
					(entity, enemySystem) -> {
						Enemies enemyDefinition = ComponentsMapper.enemy.get(entity).getEnemyDefinition();
						float disToTarget = GameUtils.calculateDistanceToTarget(entity);
						return disToTarget <= enemyDefinition.getSight().getMaxDistance();
					},
					null),
			new PrimaryAttackValidation(
					(entity, enemySystem) -> {
						Enemies enemyDefinition = ComponentsMapper.enemy.get(entity).getEnemyDefinition();
						WeaponsDefinitions primaryAttack = enemyDefinition.getPrimaryAttack();
						float disToTarget = GameUtils.calculateDistanceToTarget(entity);
						return primaryAttack.isMelee() ? disToTarget <= 1 : enemySystem.checkIfWayIsClearToTarget(entity);
					},
					(entity, enemySystem) -> {
						ComponentsMapper.enemy.get(entity).setAiStatus(DODGING);
						enemySystem.invokeEnemyTurn(entity);
					}));
}
