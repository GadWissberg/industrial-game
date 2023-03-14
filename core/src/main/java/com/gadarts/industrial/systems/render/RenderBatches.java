package com.gadarts.industrial.systems.render;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.systems.render.shaders.ModelsShaderProvider;
import com.gadarts.industrial.systems.render.shaders.ShadowMapDepthMapShader;
import com.gadarts.industrial.systems.render.shaders.ShadowMapShader;
import lombok.Getter;

@Getter
public class RenderBatches implements Disposable {
	private static final int DECALS_POOL_SIZE = 200;
	private ModelsShaderProvider shaderProvider;
	private ModelBatch depthModelBatch;
	private ModelBatch modelBatchShadows;
	private ModelBatch modelBatch;
	private SpriteBatch blurBatch;
	private DecalBatch decalBatch;

	void createBatches(StaticShadowsData staticShadowsData,
					   ImmutableArray<Entity> staticLightsEntities,
					   GameCameraGroupStrategy regularDecalGroupStrategy) {
		this.modelBatch = new ModelBatch(shaderProvider);
		this.blurBatch = new SpriteBatch();
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
					return new ShadowMapShader(renderable, shadowsShaderProgram, staticLightsEntities);
				}
			});
		}
		this.decalBatch = new DecalBatch(DECALS_POOL_SIZE, regularDecalGroupStrategy);
	}

	@Override
	public void dispose( ) {
		shaderProvider.dispose();
		decalBatch.dispose();
		blurBatch.dispose();
		modelBatch.dispose();
		if (DebugSettings.ALLOW_STATIC_SHADOWS) {
			depthModelBatch.dispose();
			modelBatchShadows.dispose();
		}
	}

	public void createShaderProvider(GameAssetManager assetsManager, GameFrameBuffer shadowFrameBuffer) {
		shaderProvider = new ModelsShaderProvider(assetsManager, shadowFrameBuffer);
	}
}
