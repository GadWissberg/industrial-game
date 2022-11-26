package com.gadarts.industrial.components.character;


import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.shared.model.characters.SpriteType;

import java.util.*;
import java.util.stream.IntStream;

public class CharacterAnimations {
	final HashMap<SpriteType, List<Map<Direction, CharacterAnimation>>> animations = new HashMap<>();

	public void put(SpriteType type, int variationIndex, Direction dir, CharacterAnimation animation) {
		if (!animations.containsKey(type)) {
			ArrayList<Map<Direction, CharacterAnimation>> variations = new ArrayList<>();
			animations.put(type, variations);
		}
		List<Map<Direction, CharacterAnimation>> variations = animations.get(type);
		if (variationIndex >= variations.size()) {
			variations.add(new HashMap<>());
		}
		variations.get(variationIndex).put(dir, animation);
	}

	public CharacterAnimation get(final SpriteType type, final Direction direction) {
		return get(type, 0, direction);
	}

	public CharacterAnimation get(SpriteType type, int variationIndex, Direction direction) {
		List<Map<Direction, CharacterAnimation>> typeAnimations = animations.get(type);
		boolean validIndex = variationIndex < typeAnimations.size();
		return validIndex ? typeAnimations.get(variationIndex).get(direction) : typeAnimations.get(0).get(direction);
	}

	public boolean contains(final SpriteType spriteType) {
		return animations.containsKey(spriteType);
	}

}
