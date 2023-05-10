package com.gadarts.industrial.systems.enemy.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnemyAiStatus {
	IDLE,
	ATTACKING(new AiStatusAttackingLogic()),
	DODGING(new AiStatusDodgingLogic()),
	SEARCHING_LOOKING(new AiStatusSearchingLookingLogic()),
	SEARCHING_WONDERING(new AiStatusSearchingWonderingLogic()),
	RUNNING_TO_LAST_SEEN_POSITION(new AiStatusRunningToLastSeenPositionLogic());

	private final AiStatusLogic logic;


	EnemyAiStatus( ) {
		this.logic = null;
	}
}
