package com.gadarts.industrial.components.character;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharacterMotivationData {
	private CharacterMotivation motivation;
	private Object motivationAdditionalData;

	public void reset() {
		motivation = null;
		motivationAdditionalData = null;
	}
}
