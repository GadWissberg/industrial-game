package com.gadarts.industrial.components.character;

import com.gadarts.industrial.shared.model.characters.attributes.Accuracy;
import com.gadarts.industrial.shared.model.characters.attributes.Agility;

public record CharacterSkillsParameters(int health, Agility agility, Accuracy accuracy) {
}
