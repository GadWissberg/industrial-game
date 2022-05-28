package com.gadarts.industrial.systems.player;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.shared.model.characters.attributes.Agility;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.map.*;
import com.gadarts.industrial.utils.EntityBuilder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class PathPlanHandler {
	public static final int ARROWS_POOL_SIZE = 20;
	public static final float ARROW_HEIGHT = 0.2f;
	private static final Vector2 auxVector2 = new Vector2();
	private static final Vector3 auxVector3_1 = new Vector3();
	private final GameAssetsManager assetManager;
	private final List<Entity> arrowsEntities = new ArrayList<>();

	@Getter
	private final GameHeuristic heuristic;

	@Getter
	private final MapGraphPath currentPath = new MapGraphPath();

	@Getter
	private final GamePathFinder pathFinder;

	public PathPlanHandler(final GameAssetsManager assetManager, MapGraph map) {
		this.assetManager = assetManager;
		this.pathFinder = new GamePathFinder(map);
		this.heuristic = new GameHeuristic();
	}

	private void createArrowsEntities(final PooledEngine engine) {
		Texture texture = assetManager.getTexture(Assets.UiTextures.PATH_ARROW);
		IntStream.range(0, ARROWS_POOL_SIZE).forEach(i -> {
			Entity entity = EntityBuilder.beginBuildingEntity(engine)
					.addSimpleDecalComponent(auxVector3_1.setZero(), texture, false)
					.finishAndAddToEngine();
			arrowsEntities.add(entity);
		});
	}

	public void hideAllArrows( ) {
		arrowsEntities.forEach(arrow -> ComponentsMapper.simpleDecal.get(arrow).setVisible(false));
	}

	void displayPathPlan(final Agility agility) {
		hideAllArrows();
		IntStream.range(0, Math.min(getCurrentPath().getCount(), agility.getValue())).forEach(i -> {
			if (i < arrowsEntities.size() && i < currentPath.getCount() - 1) {
				MapGraphNode n = currentPath.get(i);
				MapGraphNode next = currentPath.get(i + 1);
				Vector2 dirVector = auxVector2.set(next.getCol(), next.getRow()).sub(n.getCol(), n.getRow()).nor().scl(0.5f);
				transformArrowDecal(n, dirVector, ComponentsMapper.simpleDecal.get(arrowsEntities.get(i)).getDecal());
				ComponentsMapper.simpleDecal.get(arrowsEntities.get(i)).setVisible(true);
			}
		});
	}

	private void transformArrowDecal(final MapGraphNode currentNode, final Vector2 directionVector, final Decal decal) {
		decal.getRotation().idt();
		decal.rotateX(90);
		decal.rotateZ(directionVector.angleDeg());
		Vector3 pos = auxVector3_1.set(currentNode.getCol() + 0.5f, ARROW_HEIGHT, currentNode.getRow() + 0.5f);
		decal.setPosition(pos.add(directionVector.x, currentNode.getHeight(), directionVector.y));
	}

	public void init(final PooledEngine engine) {
		createArrowsEntities(engine);
	}

	public void resetPlan( ) {
		hideAllArrows();
		currentPath.clear();
	}
}
