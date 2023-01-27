package com.gadarts.industrial.components.character;

import com.badlogic.gdx.utils.TimeUtils;
import lombok.Getter;

@Getter
public class CharacterHealthData {
	private int hp;
	private int initialHp;
	private long lastDamage;

	public void dealDamage(final int damagePoints) {
		hp -= damagePoints;
		lastDamage = TimeUtils.millis();
	}

	public void init(final int initialHp) {
		this.initialHp = initialHp;
		this.hp = initialHp;
	}
}
