package com.gadarts.industrial.systems.render;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.systems.render.fb.GameFrameBuffer;
import com.gadarts.industrial.systems.render.shaders.models.ModelsShaderProvider;
import com.gadarts.industrial.systems.render.shaders.shadow.ShadowMapDepthMapShader;
import com.gadarts.industrial.systems.render.shaders.shadow.ShadowMapShader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RenderBatches implements Disposable {
	private static final int DECALS_POOL_SIZE = 200;
	private final Decal playerDecal;
	private final ModelInstance cursorModelInstance;
	private ModelsShaderProvider modelsShaderProvider;
	private ModelBatch depthModelBatch;
	private ModelBatch modelBatchShadows;
	private ModelBatch modelBatch;
	private DecalBatch decalBatch;


	void createBatches(StaticShadowsData staticShadowsData,
					   ImmutableArray<Entity> staticLightsEntities,
					   GameCameraGroupStrategy regularDecalGroupStrategy) {
		this.modelBatch = new ModelBatch(modelsShaderProvider);
		if (DebugSettings.ALLOW_STATIC_SHADOWS) {
			depthModelBatch = new ModelBatch(new DefaultShaderProvider() {
				@Override
				protected Shader createShader(final Renderable renderable) {
					return new ShadowMapDepthMapShader(renderable, staticShadowsData.getDepthShaderProgram());
				}
			});
			modelBatchShadows = new ModelBatch(new DefaultShaderProvider() {
				@Override
				protected Shader createShader(final Renderable renderable) {
					ShaderProgram shadowsShaderProgram = staticShadowsData.getShadowsShaderProgram();
					return new ShadowMapShader(renderable, shadowsShaderProgram, staticLightsEntities, playerDecal, cursorModelInstance);
				}
			});
		}
		this.decalBatch = new DecalBatch(DECALS_POOL_SIZE, regularDecalGroupStrategy);
	}

	@Override
	public void dispose( ) {
		modelsShaderProvider.dispose();
		decalBatch.dispose();
		modelBatch.dispose();
		if (DebugSettings.ALLOW_STATIC_SHADOWS) {
			depthModelBatch.dispose();
			modelBatchShadows.dispose();
		}
	}

	public void createShaderProvider(GameAssetManager assetsManager,
									 GameFrameBuffer shadowFrameBuffer) {
		modelsShaderProvider = new ModelsShaderProvider(assetsManager, shadowFrameBuffer, playerDecal, cursorModelInstance);
	}
}
