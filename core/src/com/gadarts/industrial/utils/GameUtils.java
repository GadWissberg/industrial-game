package com.gadarts.industrial.utils;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.player.Weapon;
import com.gadarts.industrial.map.CalculatePathRequest;
import com.gadarts.industrial.map.GameHeuristic;
import com.gadarts.industrial.map.GamePathFinder;
import com.gadarts.industrial.map.MapGraphPath;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.model.pickups.PlayerWeaponsDefinitions;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.google.gson.JsonObject;

import java.util.LinkedHashSet;

import static com.gadarts.industrial.components.ComponentsMapper.character;
import static com.gadarts.industrial.components.ComponentsMapper.characterDecal;
import static com.gadarts.industrial.components.player.PlayerComponent.PLAYER_HEIGHT;

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
		return output;
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

	public static float calculateCharacterHeight(Entity character) {
		float height;
		if (ComponentsMapper.enemy.has(character)) {
			height = ComponentsMapper.enemy.get(character).getEnemyDefinition().getHeight();
		} else {
			height = PLAYER_HEIGHT;
		}
		return height;
	}

	public static boolean calculatePath(CalculatePathRequest request,
										GamePathFinder pathFinder,
										GameHeuristic heuristic,
										int maxNumberOfNodesInOutput) {
		MapGraphPath outputPath = request.getOutputPath();
		outputPath.clear();
		pathFinder.searchNodePathBeforeCommand(heuristic, request);
		boolean foundPath = outputPath.nodes.size > 1;
		if (maxNumberOfNodesInOutput > 0 && maxNumberOfNodesInOutput < outputPath.nodes.size) {
			outputPath.nodes.removeRange(maxNumberOfNodesInOutput, outputPath.nodes.size - 1);
		}
		return foundPath;
	}

	public static LinkedHashSet<GridPoint2> findAllNodesToTarget(Entity enemy,
																 LinkedHashSet<GridPoint2> output,
																 boolean removeEdgeNodes) {
		output.clear();
		Vector2 pos = characterDecal.get(enemy).getNodePosition(auxVector2_1);
		Entity target = character.get(enemy).getTarget();
		Vector2 targetPos = characterDecal.get(target).getNodePosition(auxVector2_2);
		return findAllNodesBetweenNodes(
				pos,
				targetPos,
				false,
				output,
				removeEdgeNodes);
	}

	public static LinkedHashSet<GridPoint2> findAllNodesBetweenNodes(Vector2 src,
																	 Vector2 dst,
																	 LinkedHashSet<GridPoint2> output) {
		return findAllNodesBetweenNodes(src, dst, false, output, false);
	}

	public static LinkedHashSet<GridPoint2> findAllNodesBetweenNodes(Vector2 src,
																	 Vector2 dst,
																	 boolean srcToDstOnly,
																	 LinkedHashSet<GridPoint2> output,
																	 boolean removeEdgeNodes) {
		output.clear();

		Array<GridPoint2> srcToDst = bresenham.line((int) src.x, (int) src.y, (int) dst.x, (int) dst.y);
		if (srcToDst.size > 1 && removeEdgeNodes) {
			srcToDst.removeIndex(0);
			srcToDst.removeIndex(srcToDst.size - 1);
		}

		Array<GridPoint2> dstToSrc = null;

		if (!srcToDstOnly) {
			dstToSrc = bresenham.line((int) dst.x, (int) dst.y, (int) src.x, (int) src.y);
			if (dstToSrc.size > 1 && removeEdgeNodes) {
				dstToSrc.removeIndex(0);
				dstToSrc.removeIndex(dstToSrc.size - 1);
			}
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

	public static int getPrimaryAttackHitFrameIndexForCharacter(Entity character, SystemsCommonData commonData) {
		if (ComponentsMapper.player.has(character)) {
			Weapon selectedWeapon = commonData.getStorage().getSelectedWeapon();
			PlayerWeaponsDefinitions definition = (PlayerWeaponsDefinitions) (selectedWeapon.getDefinition());
			return definition.getHitFrameIndex();
		} else {
			return ComponentsMapper.character.get(character).getCharacterSpriteData().getPrimaryAttackHitFrameIndex();
		}
	}

	public static float calculateAngbandDistanceToTarget(Entity character) {
		Entity target = ComponentsMapper.character.get(character).getTarget();
		Vector3 targetPosition = ComponentsMapper.characterDecal.get(target).getDecal().getPosition();
		Vector3 position = ComponentsMapper.characterDecal.get(character).getDecal().getPosition();
		Vector2 src = auxVector2_1.set((int) position.x, (int) position.z);
		Vector2 dst = auxVector2_2.set((int) targetPosition.x, (int) targetPosition.z);
		return GameUtils.calculateAngbandDistance(src, dst);
	}

	public static int calculateAngbandDistance(Vector2 src, Vector2 dst) {
		float xAxis = Math.abs(src.x - dst.x);
		float zAxis = Math.abs(src.y - dst.y);
		float longAxis = Math.max(xAxis, zAxis);
		float shortAxis = Math.min(xAxis, zAxis);
		return (int) (longAxis + shortAxis / 2F);
	}
}
