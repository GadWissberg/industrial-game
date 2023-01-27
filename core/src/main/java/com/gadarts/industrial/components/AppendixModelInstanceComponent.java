package com.gadarts.industrial.components;

import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
import lombok.Getter;

@Getter
public class AppendixModelInstanceComponent extends ModelInstanceComponent {
	public void init(GameModelInstance modelInstance) {
		super.init(modelInstance, true);
	}
}
