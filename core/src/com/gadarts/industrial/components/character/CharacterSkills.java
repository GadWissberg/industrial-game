package com.gadarts.industrial.components.character;

import com.gadarts.industrial.shared.model.characters.attributes.Accuracy;
import com.gadarts.industrial.shared.model.characters.attributes.Agility;
import lombok.Getter;

@Getter
public class CharacterSkills {

	private final CharacterHealthData healthData = new CharacterHealthData();
	private Agility agility;
	private Accuracy accuracy;

	public void applyParameters(final CharacterSkillsParameters skills) {
		this.healthData.init(skills.health());
		this.agility = skills.agility();
		this.accuracy = skills.accuracy();
	}
}
