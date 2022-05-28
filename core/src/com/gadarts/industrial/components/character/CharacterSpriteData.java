package com.gadarts.industrial.components.character;

import com.badlogic.gdx.utils.Pool;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CharacterSpriteData implements Pool.Poolable {
	private Direction facingDirection;
	private int frameIndexNotAffectedByLight;
	private SpriteType spriteType;
	private int meleeHitFrameIndex;
	private int primaryAttackHitFrameIndex;
	private boolean singleDeathAnimation;

	@Override
	public void reset( ) {
		frameIndexNotAffectedByLight = -1;
	}

	public boolean isSingleDeathAnimation( ) {
		return singleDeathAnimation;
	}

	public void init(final Direction direction,
					 final SpriteType spriteType,
					 final int hitFrameIndex,
					 final int primaryAttackHitFrameIndex,
					 final boolean singleDeathAnimation) {
		this.facingDirection = direction;
		this.spriteType = spriteType;
		this.meleeHitFrameIndex = hitFrameIndex;
		this.primaryAttackHitFrameIndex = primaryAttackHitFrameIndex;
		this.singleDeathAnimation = singleDeathAnimation;
	}
}
