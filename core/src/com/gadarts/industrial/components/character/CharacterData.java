package com.gadarts.industrial.components.character;

import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.shared.model.characters.Direction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class CharacterData {
	private final Vector3 position;
	private final Direction direction;
	private final CharacterSkillsParameters skills;
	private final CharacterSoundData soundData;

}
