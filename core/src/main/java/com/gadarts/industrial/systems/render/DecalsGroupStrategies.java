package com.gadarts.industrial.systems.render;

import com.badlogic.gdx.graphics.Camera;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import lombok.Getter;

@Getter
public class DecalsGroupStrategies {
	private GameCameraGroupStrategy regularDecalGroupStrategy;
	private OutlineGroupStrategy outlineDecalGroupStrategy;

	void createDecalGroupStrategies(Camera camera, GameAssetManager assetsManager) {
		regularDecalGroupStrategy = new GameCameraGroupStrategy(camera, assetsManager);
		outlineDecalGroupStrategy = new OutlineGroupStrategy(camera, assetsManager);
	}
}
