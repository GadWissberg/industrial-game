package com.gadarts.industrial.systems.render.shaders;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.components.player.PlayerComponent;

import static com.gadarts.industrial.components.ComponentsMapper.*;

public final class ShaderUtils {
	public static final float X_RAY_PLAYER_DISTANCE_CHECK_BIAS = -0.2F;

	private static final Vector3 auxVector3_1 = new Vector3();
	private static final Vector3 auxVector3_2 = new Vector3();
	private static final float X_RAY_RADIUS_CHECK = 100F;

	public static Vector2 calculateXRay(Entity renderedEntity, Vector3 targetPos, Camera camera, Vector2 output, float xRayDistanceCheckBias) {
		if (wall.has(renderedEntity) || cursor.has(renderedEntity)) return output.setZero();

		Vector3 renderedEntityPos = modelInstance.get(renderedEntity).getModelInstance().transform.getTranslation(auxVector3_2);
		float targetToCameraDistance = auxVector3_1.set(targetPos.x, 0F, targetPos.z).dst2(camera.position.x, 0F, camera.position.z);
		boolean floorCheck = !floor.has(renderedEntity) || renderedEntityPos.y > targetPos.y + PlayerComponent.PLAYER_HEIGHT / 3F;
		renderedEntityPos.y = 0F;
		float renderedEntityToCameraDistance = renderedEntityPos.dst2(camera.position.x, 0F, camera.position.z) + xRayDistanceCheckBias;
		float renderedEntityToTargetDistance = renderedEntityPos.dst2(targetPos.x, 0F, targetPos.z);
		if (renderedEntityToTargetDistance <= X_RAY_RADIUS_CHECK && renderedEntityToCameraDistance < targetToCameraDistance && floorCheck) {
			Vector3 project = camera.project(auxVector3_1.set(targetPos));
			output.set(project.x, project.y);
		} else {
			output.setZero();
		}
		return output;
	}
}
