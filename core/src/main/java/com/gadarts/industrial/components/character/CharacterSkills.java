package com.gadarts.industrial.components.character;

import com.badlogic.gdx.math.MathUtils;
import com.gadarts.industrial.shared.assets.declarations.Agility;
import com.gadarts.industrial.shared.model.characters.attributes.Accuracy;
import lombok.Getter;

import java.util.Random;
import java.util.stream.IntStream;

@Getter
public class CharacterSkills {

	private static final Random random = new Random();
	private final CharacterHealthData healthData = new CharacterHealthData();
	private Agility agilityDefinition;
	private Accuracy accuracyDefinition;
	private int turnAgility;

	public void applyParameters(final CharacterSkillsParameters skills) {
		this.healthData.init(skills.health());
		this.agilityDefinition = skills.agility();
		this.accuracyDefinition = skills.accuracy();
	}

	public void resetTurnAgility( ) {
		this.turnAgility = MathUtils.random(agilityDefinition.min(), agilityDefinition.max());
	}
}
