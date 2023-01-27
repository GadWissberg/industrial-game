package com.gadarts.industrial.systems.amb;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.components.DoorComponent;

public interface DoorAnimation {
	void update(ModelInstance modelInstance, Vector3 nodeCenterPosition, DoorComponent.DoorStates targetState);

	boolean isAnimationEnded(DoorComponent.DoorStates targetState,
							 Vector3 nodeCenterPosition,
							 Entity modelInstance);
}
