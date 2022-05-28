package com.gadarts.industrial.systems.ui.menu;

import com.badlogic.ashley.core.Entity;

public interface MenuOptionValidation {
	boolean validate(Entity player);
}
