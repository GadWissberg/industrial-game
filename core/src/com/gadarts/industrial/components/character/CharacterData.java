package com.gadarts.industrial.components.character;

import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.shared.model.characters.Direction;

public record CharacterData(Vector3 position, Direction direction, CharacterSkillsParameters skills,
							CharacterSoundData soundData) {
}
