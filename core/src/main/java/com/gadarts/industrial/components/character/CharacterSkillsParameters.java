package com.gadarts.industrial.components.character;

import com.gadarts.industrial.shared.assets.declarations.Agility;
import com.gadarts.industrial.shared.model.characters.attributes.Accuracy;

public record CharacterSkillsParameters(int health, Agility agility, Accuracy accuracy) {
}
