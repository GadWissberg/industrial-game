package com.gadarts.industrial.systems.player;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.map.GameHeuristic;
import com.gadarts.industrial.map.GamePathFinder;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphPath;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.utils.EntityBuilder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class PathPlanHandler {
	public static final int ARROWS_POOL_SIZE = 20;
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

	public void init(final PooledEngine engine) {
		createArrowsEntities(engine);
	}

	public void resetPlan( ) {
		hideAllArrows();
		currentPath.clear();
	}
}
