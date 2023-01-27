package com.gadarts.industrial.systems.amb;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.components.AppendixModelInstanceComponent;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
import com.gadarts.industrial.shared.model.env.DoorTypes;

import java.util.Map;

import static com.gadarts.industrial.components.DoorComponent.DoorStates.CLOSED;
import static com.gadarts.industrial.components.DoorComponent.DoorStates.OPEN;
import static com.gadarts.industrial.utils.GameUtils.EPSILON;

public final class DoorsAnimations {
	public static final float DOOR_OPEN_OFFSET = 1F;
	public static final float DOOR_MOVE_INTER_COEF = 0.03F;
	public static final float MAX_DEG_ROTATE_DOOR_OPEN = 80F;
	private static final float DOOR_OPEN_OFFSET_EPSILON = 3 * EPSILON;
	private static final float DOOR_CLOSED_OFFSET_EPSILON = 0.003f;
	private static final Matrix4 auxMatrix = new Matrix4();
	private static final float DOOR_ROTATION_STEP = 2F;
	private final static Vector2 auxVector2 = new Vector2();
	private final static Vector3 auxVector3 = new Vector3();
	private static final Quaternion auxQuat1 = new Quaternion();
	private static final Quaternion auxQuat2 = new Quaternion();
	static final Map<DoorTypes, DoorAnimation> animations = Map.of(
			DoorTypes.SLIDE, new DoorAnimation() {
				@Override
				public void update(ModelInstance modelInstance,
								   Vector3 nodeCenterPosition,
								   DoorComponent.DoorStates targetState) {
					modelInstance.transform.lerp(auxMatrix.set(modelInstance.transform)
									.setTranslation(nodeCenterPosition)
									.translate(0F, 0F, (targetState == OPEN ? 1 : -1) * DOOR_OPEN_OFFSET),
							DOOR_MOVE_INTER_COEF);
				}

				@Override
				public boolean isAnimationEnded(DoorComponent.DoorStates targetState,
												Vector3 nodeCenterPosition,
												Entity entity) {
					AppendixModelInstanceComponent modelInstComp = ComponentsMapper.appendixModelInstance.get(entity);
					GameModelInstance modelInstance = modelInstComp.getModelInstance();
					float distance = nodeCenterPosition.dst2(modelInstance.transform.getTranslation(auxVector3));
					boolean farEnough = distance > DOOR_OPEN_OFFSET - DOOR_OPEN_OFFSET_EPSILON;
					boolean closeEnough = distance < DOOR_CLOSED_OFFSET_EPSILON;
					return (targetState == OPEN && farEnough) || (targetState == CLOSED && closeEnough);
				}
			},
			DoorTypes.ROTATE, new DoorAnimation() {
				@Override
				public void update(ModelInstance modelInstance,
								   Vector3 nodeCenterPosition,
								   DoorComponent.DoorStates targetState) {
					modelInstance.transform.rotate(Vector3.Y, (targetState == OPEN ? 1 : -1) * DOOR_ROTATION_STEP);
				}

				@Override
				public boolean isAnimationEnded(DoorComponent.DoorStates targetState,
												Vector3 nodeCenterPosition,
												Entity doorEntity) {
					ModelInstanceComponent frameModelInstanceComp = ComponentsMapper.modelInstance.get(doorEntity);
					ModelInstanceComponent doorModelInstanceComp = ComponentsMapper.appendixModelInstance.get(doorEntity);
					float closeYaw = frameModelInstanceComp.getModelInstance().transform.getRotation(auxQuat1).getYaw();
					closeYaw = auxVector2.set(1F, 0F).setAngleDeg(closeYaw + 1).angleDeg();
					float yaw = doorModelInstanceComp.getModelInstance().transform.getRotation(auxQuat2).getYaw();
					yaw = auxVector2.set(1F, 0F).setAngleDeg(yaw).angleDeg();
					float openYaw = auxVector2.set(1F, 0F).setAngleDeg(closeYaw + MAX_DEG_ROTATE_DOOR_OPEN).angleDeg();
					return targetState == OPEN ? yaw >= openYaw : yaw <= closeYaw;
				}

			});
}
