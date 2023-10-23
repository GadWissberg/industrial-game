package com.gadarts.industrial.systems.render.shaders;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.components.player.PlayerComponent;

import static com.gadarts.industrial.components.ComponentsMapper.*;

public final class ShaderUtils {
	private static final float X_RAY_DISTANCE_CHECK_BIAS = -0.2F;
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final Vector3 auxVector3_2 = new Vector3();

	public static Vector2 calculateXRay(Entity renderedEntity, Decal playerDecal, Camera camera, Vector2 output) {
		if (wall.has(renderedEntity)) return output.setZero();

		Vector3 renderedEntityPos = modelInstance.get(renderedEntity).getModelInstance().transform.getTranslation(auxVector3_2);
		Vector3 playerPos = playerDecal.getPosition();
		float playerToCameraDistance = auxVector3_1.set(playerPos.x, 0F, playerPos.z).dst2(camera.position);
		boolean floorCheck = !floor.has(renderedEntity) || renderedEntityPos.y > playerPos.y + PlayerComponent.PLAYER_HEIGHT / 3F;
		renderedEntityPos.y = 0F;
		float renderedEntityToCameraDistance = renderedEntityPos.dst2(camera.position) + X_RAY_DISTANCE_CHECK_BIAS;
		if (renderedEntityToCameraDistance < playerToCameraDistance && floorCheck) {
			Vector3 project = camera.project(auxVector3_1.set(playerPos));
			output.set(project.x, project.y);
		} else {
			output.setZero();
		}
		return output;
	}
}
