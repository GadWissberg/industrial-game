package com.gadarts.industrial.utils;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.player.Weapon;
import com.gadarts.industrial.map.*;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.declarations.pickups.weapons.PlayerWeaponDeclaration;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.player.PlayerStorage;
import com.google.gson.JsonObject;

import java.util.LinkedHashSet;

import static com.gadarts.industrial.components.ComponentsMapper.character;
import static com.gadarts.industrial.components.ComponentsMapper.characterDecal;
import static com.gadarts.industrial.components.player.PlayerComponent.PLAYER_HEIGHT;

public class GameUtils {
	public static final float EPSILON = 0.025f;
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final Vector2 auxVector2_2 = new Vector2();
	private static final Bresenham2 bresenham = new Bresenham2();
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final Vector3 auxVector3_2 = new Vector3();

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

	public static String getRandomSound(final Assets.Sounds soundDefinition) {
		int random = MathUtils.random(soundDefinition.getFiles().length - 1);
		return soundDefinition.getFiles()[random];
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

	public static float calculateCharacterHeight(Entity character) {
		float height;
		if (ComponentsMapper.enemy.has(character)) {
			height = ComponentsMapper.enemy.get(character).getEnemyDeclaration().getHeight();
		} else {
			height = PLAYER_HEIGHT;
		}
		return height;
	}

	public static Vector3 calculateDirectionToTarget(Entity character,
													 MapGraph map) {
		CharacterDecalComponent targetDecalComp = ComponentsMapper.characterDecal.get(ComponentsMapper.character.get(character).getTarget());
		MapGraphNode targetNode = map.getNode(targetDecalComp.getDecal().getPosition());
		Vector3 targetNodeCenterPosition = targetNode.getCenterPosition(auxVector3_1);
		targetNodeCenterPosition.y += 0.5f;
		MapGraphNode node = map.getNode(characterDecal.get(character).getDecal().getPosition());
		return targetNodeCenterPosition.sub(node.getCenterPosition(auxVector3_2));
	}

	public static boolean calculatePath(CalculatePathRequest request,
										GamePathFinder pathFinder,
										GameHeuristic heuristic) {
		MapGraphPath outputPath = request.getOutputPath();
		outputPath.clear();
		pathFinder.searchNodePathBeforeCommand(heuristic, request);
		return outputPath.nodes.size > 1;
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
			PlayerStorage storage = commonData.getStorage();
			Weapon selectedWeapon = storage.getSelectedWeapon();
			PlayerWeaponDeclaration playerWeaponDeclaration = (PlayerWeaponDeclaration) selectedWeapon.getDeclaration();
			PlayerWeaponDeclaration definition = storage.getPlayerWeaponsDeclarations().get(playerWeaponDeclaration.declaration());
			return definition.hitFrameIndex();
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

	public static void clearDisplay(float alpha) {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		int sam = Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0;
		Gdx.gl.glClearColor(0, 0, 0, alpha);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | sam);
	}
}
