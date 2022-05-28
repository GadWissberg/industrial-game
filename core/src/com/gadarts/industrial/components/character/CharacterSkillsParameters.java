package com.gadarts.industrial.components.character;

import com.gadarts.industrial.shared.model.characters.attributes.Accuracy;
import com.gadarts.industrial.shared.model.characters.attributes.Agility;
import com.gadarts.industrial.shared.model.characters.attributes.Strength;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CharacterSkillsParameters {
	private final int health;
	private final Agility agility;
	private final Strength strength;
	private final Accuracy accuracy;

}
