package com.gadarts.industrial.components.enemy;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnemyTimeStamps {
	private long lastTurn = -1;
	private long lastPrimaryAttack = -1;

	public void reset() {
		lastTurn = -1;
		lastPrimaryAttack = -1;
	}
}
