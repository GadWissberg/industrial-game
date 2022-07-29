package com.gadarts.industrial.systems;

import com.badlogic.gdx.utils.Pool;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;

import java.util.HashMap;
import java.util.Map;

public class ModelInstancePools {
	private Map<Assets.Models, Pool<GameModelInstance>> pools = new HashMap<>();

	private void createPoolForModelInstanceIfNotExists(GameAssetsManager assetsManager, Assets.Models modelDefinition) {
		if (!pools.containsKey(modelDefinition)) {
			pools.put(modelDefinition, new Pool<>() {
				@Override
				protected GameModelInstance newObject( ) {
					return new GameModelInstance(assetsManager.getModel(modelDefinition));
				}
			});
		}
	}

	public GameModelInstance obtain(GameAssetsManager assetsManager, Assets.Models modelDefinition) {
		createPoolForModelInstanceIfNotExists(assetsManager, modelDefinition);
		return pools.get(modelDefinition).obtain();
	}
}
