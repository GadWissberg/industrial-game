package com.gadarts.industrial.components.character;

import com.gadarts.industrial.shared.assets.Assets;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharacterSoundData {
	private Assets.Sounds painSound;
	private Assets.Sounds deathSound;

	public void set(final CharacterSoundData soundData) {
		set(soundData.getPainSound(), soundData.getDeathSound());
	}

	public void set(Assets.Sounds painSound, Assets.Sounds deathSound) {
		this.painSound = painSound;
		this.deathSound = deathSound;
	}
}
