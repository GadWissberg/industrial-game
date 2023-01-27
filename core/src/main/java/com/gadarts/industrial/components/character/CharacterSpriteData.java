package com.gadarts.industrial.components.character;

import com.badlogic.gdx.utils.Pool;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CharacterSpriteData implements Pool.Poolable {
	private int frameIndexNotAffectedByLight;
	private SpriteType spriteType;
	private int primaryAttackHitFrameIndex;
	private boolean singleDeathAnimation;
	private long nextIdleAnimationPlay;

	@Override
	public void reset( ) {
		frameIndexNotAffectedByLight = -1;
	}

	public void init(final SpriteType spriteType,
					 final int primaryAttackHitFrameIndex,
					 final boolean singleDeathAnimation) {
		this.spriteType = spriteType;
		this.primaryAttackHitFrameIndex = primaryAttackHitFrameIndex;
		this.singleDeathAnimation = singleDeathAnimation;
	}
}
