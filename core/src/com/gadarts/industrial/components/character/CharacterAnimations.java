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
			IntStream.range(0, type.getVariations()).forEach(i -> variations.add(new HashMap<>()));
			animations.put(type, variations);
		}
		animations.get(type).get(variationIndex).put(dir, animation);
	}

	public void clear( ) {
		Set<Map.Entry<SpriteType, List<Map<Direction, CharacterAnimation>>>> entrySet = animations.entrySet();
		for (Map.Entry<SpriteType, List<Map<Direction, CharacterAnimation>>> entry : entrySet) {
			List<Map<Direction, CharacterAnimation>> variations = entry.getValue();
			variations.forEach(map -> map.clear());
			variations.clear();
		}
	}

	public CharacterAnimation get(final SpriteType type, final Direction direction) {
		return get(type, 0, direction);
	}

	public CharacterAnimation get(SpriteType type, int variationIndex, Direction direction) {
		return animations.get(type).get(variationIndex).get(direction);
	}

	public boolean contains(final SpriteType spriteType) {
		return animations.containsKey(spriteType);
	}
}
