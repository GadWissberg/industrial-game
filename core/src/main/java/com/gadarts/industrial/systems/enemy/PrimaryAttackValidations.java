package com.gadarts.industrial.systems.enemy;

import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.enemy.EnemyComponent;
import com.gadarts.industrial.shared.assets.declarations.enemies.EnemyDeclaration;
import com.gadarts.industrial.shared.assets.declarations.weapons.WeaponDeclaration;
import com.gadarts.industrial.utils.GameUtils;

import java.util.List;

import static com.gadarts.industrial.systems.enemy.EnemyAiStatus.*;

final public class PrimaryAttackValidations {
	public final static List<PrimaryAttackValidation> primaryAttackValidations = List.of(
			new PrimaryAttackValidation(
					(entity, enemySystem) -> {
						EnemyComponent enemyComp = ComponentsMapper.enemy.get(entity);
						WeaponDeclaration primaryAttack = enemyComp.getEnemyDeclaration().attackPrimary();
						return enemyComp.getEngineEnergy() >= primaryAttack.engineConsumption();
					},
					(entity, enemySystem) -> {
						ComponentsMapper.enemy.get(entity).setAiStatus(DODGING);
						enemySystem.invokeEnemyTurn(entity);
					}),
			new PrimaryAttackValidation(
					(entity, enemySystem) -> {
						EnemyDeclaration enemyDefinition = ComponentsMapper.enemy.get(entity).getEnemyDeclaration();
						WeaponDeclaration primaryAttack = enemyDefinition.attackPrimary();
						return ComponentsMapper.character.get(entity).getTurnTimeLeft() >= primaryAttack.duration();
					},
					null),
			new PrimaryAttackValidation(
					(entity, enemySystem) -> {
						EnemyDeclaration enemyDefinition = ComponentsMapper.enemy.get(entity).getEnemyDeclaration();
						float disToTarget = GameUtils.calculateAngbandDistanceToTarget(entity);
						return disToTarget <= enemyDefinition.sight().getMaxDistance();
					},
					null),
			new PrimaryAttackValidation(
					(entity, enemySystem) -> {
						EnemyDeclaration enemyDefinition = ComponentsMapper.enemy.get(entity).getEnemyDeclaration();
						WeaponDeclaration primaryAttack = enemyDefinition.attackPrimary();
						float disToTarget = GameUtils.calculateAngbandDistanceToTarget(entity);
						return !primaryAttack.melee() || disToTarget <= 1;
					},
					(entity, enemySystem) -> {
						ComponentsMapper.enemy.get(entity).setAiStatus(RUNNING_TO_LAST_SEEN_POSITION);
						enemySystem.invokeEnemyTurn(entity);
					}),
			new PrimaryAttackValidation(
					(entity, enemySystem) -> {
						EnemyDeclaration enemyDefinition = ComponentsMapper.enemy.get(entity).getEnemyDeclaration();
						WeaponDeclaration primaryAttack = enemyDefinition.attackPrimary();
						return primaryAttack.melee() || enemySystem.checkIfWayIsClearToTarget(entity);
					},
					(entity, enemySystem) -> {
						ComponentsMapper.enemy.get(entity).setAiStatus(RUNNING_TO_LAST_SEEN_POSITION);
						enemySystem.invokeEnemyTurn(entity);
					}));
}
