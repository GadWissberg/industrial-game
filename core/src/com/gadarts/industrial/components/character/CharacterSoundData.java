package com.gadarts.industrial.components.character;

import com.gadarts.industrial.shared.assets.Assets;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharacterSoundData {
	private Assets.Sounds painSound;
	private Assets.Sounds deathSound;
	private Assets.Sounds stepSound;

	public void set(final CharacterSoundData soundData) {
		set(soundData.getPainSound(), soundData.getDeathSound(), soundData.getStepSound());
	}

	public void set(final Assets.Sounds painSound, final Assets.Sounds deathSound, final Assets.Sounds stepSound) {
		this.painSound = painSound;
		this.deathSound = deathSound;
		this.stepSound = stepSound;
	}
}
