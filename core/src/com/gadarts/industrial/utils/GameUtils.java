package com.gadarts.industrial.utils;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.map.CalculatePathRequest;
import com.gadarts.industrial.map.GameHeuristic;
import com.gadarts.industrial.map.GamePathFinder;
import com.gadarts.industrial.map.MapGraphPath;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.model.pickups.PlayerWeaponsDefinitions;
import com.google.gson.JsonObject;

import java.util.LinkedHashSet;

import static com.gadarts.industrial.components.ComponentsMapper.character;
import static com.gadarts.industrial.components.ComponentsMapper.characterDecal;

public class GameUtils {
	public static final float EPSILON = 0.025f;
	private static final Plane floorPlane = new Plane(new Vector3(0, 1, 0), 0);
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final Vector2 auxVector2_2 = new Vector2();
	private static final Bresenham2 bresenham = new Bresenham2();

	/**
	 * Whether given contained is fully inside the container.
	 */
	public static boolean rectangleContainedInRectangleWithBoundaries(final Rectangle container,
																	  final Rectangle contained) {
		float xmin = contained.x;
		float xmax = xmin + contained.width;
		float ymin = contained.y;
		float ymax = ymin + contained.height;
		float x = container.getX();
		float y = container.getY();
		float width = container.getWidth();
		float height = container.getHeight();
		return ((xmin >= x && xmin <= x + width) && (xmax >= x && xmax <= x + width))
				&& ((ymin >= y && ymin <= y + height) && (ymax >= y && ymax <= y + height));
	}

	public static String getRandomRoadSound(final Assets.Sounds soundDefinition) {
		int random = MathUtils.random(soundDefinition.getFiles().length - 1);
		return soundDefinition.getFiles()[random];
	}

	/**
	 * Calculates the node's position based on screen mouse position.
	 *
	 * @param camera  The rendering camera.
	 * @param screenX MouseX
	 * @param screenY MouseY
	 * @param output  The result
	 * @return output argument for chaining.
	 */
	public static Vector3 calculateGridPositionFromMouse(final Camera camera,
														 final float screenX,
														 final float screenY,
														 final Vector3 output) {
		Ray ray = camera.getPickRay(screenX, screenY);
		Intersector.intersectRayPlane(ray, floorPlane, output);
		return alignPositionToGrid(output);
	}

	/**
	 * Floors x and z.
	 *
	 * @param position Given position
	 * @return position argument for chaining.
	 */
	public static Vector3 alignPositionToGrid(final Vector3 position) {
		position.x = MathUtils.floor(position.x);
		position.y = 0;
		position.z = MathUtils.floor(position.z);
		return position;
	}

	public static float getFloatFromJsonOrDefault(final JsonObject jsonObject,
												  final String key,
												  final float defaultValue) {
		float result = defaultValue;
		if (jsonObject.has(key)) {
			result = jsonObject.get(key).getAsFloat();
		}
		return result;
	}

	public static boolean calculatePath(CalculatePathRequest request,
										GamePathFinder pathFinder,
										GameHeuristic heuristic) {
		return calculatePath(request, pathFinder, heuristic, 0);
	}

	public static boolean calculatePath(CalculatePathRequest request,
										GamePathFinder pathFinder,
										GameHeuristic heuristic,
										int maxNumberOfNodes) {
		MapGraphPath outputPath = request.getOutputPath();
		outputPath.clear();
		pathFinder.searchNodePathBeforeCommand(heuristic, request);
		boolean foundPath = outputPath.nodes.size > 1;
		if (maxNumberOfNodes > 0 && maxNumberOfNodes < outputPath.nodes.size && foundPath) {
			outputPath.nodes.removeRange(maxNumberOfNodes, outputPath.nodes.size - 1);
		}
		return foundPath;
	}

	public static LinkedHashSet<GridPoint2> findAllNodesToTarget(Entity enemy, LinkedHashSet<GridPoint2> output) {
		output.clear();
		Vector2 pos = characterDecal.get(enemy).getNodePosition(auxVector2_1);
		Entity target = character.get(enemy).getTarget();
		Vector2 targetPos = characterDecal.get(target).getNodePosition(auxVector2_2);
		return findAllNodesBetweenNodes(pos, targetPos, false, output);
	}

	public static LinkedHashSet<GridPoint2> findAllNodesBetweenNodes(Vector2 src,
																	 Vector2 dst,
																	 LinkedHashSet<GridPoint2> output) {
		return findAllNodesBetweenNodes(src, dst, false, output);
	}

	public static LinkedHashSet<GridPoint2> findAllNodesBetweenNodes(Vector2 src,
																	 Vector2 dst,
																	 boolean srcToDstOnly,
																	 LinkedHashSet<GridPoint2> output) {
		output.clear();

		Array<GridPoint2> srcToDst = bresenham.line((int) src.x, (int) src.y, (int) dst.x, (int) dst.y);
		Array<GridPoint2> dstToSrc = null;

		if (!srcToDstOnly) {
			dstToSrc = bresenham.line((int) dst.x, (int) dst.y, (int) src.x, (int) src.y);
		}

		for (int i = srcToDst.size - 1; i >= 0; i--) {
			output.add(srcToDst.get(i));
		}
		if (!srcToDstOnly) {
			for (int i = dstToSrc.size - 1; i >= 0; i--) {
				output.add(dstToSrc.get(i));
			}
		}

		return output;
	}

	public static int getPrimaryAttackHitFrameIndexForCharacter(Entity character, PlayerWeaponsDefinitions selectedWeapon) {
		if (ComponentsMapper.player.has(character)) {
			return selectedWeapon.getHitFrameIndex();
		} else {
			return ComponentsMapper.character.get(character).getCharacterSpriteData().getPrimaryAttackHitFrameIndex();
		}
	}
}
