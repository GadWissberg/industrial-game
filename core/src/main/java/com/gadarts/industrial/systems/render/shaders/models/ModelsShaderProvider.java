package com.gadarts.industrial.systems.render.shaders.models;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetManager;

public class ModelsShaderProvider extends DefaultShaderProvider {
	private final DefaultShader.Config shaderConfig;
	private final FrameBuffer shadowFrameBuffer;
	private final Decal playerDecal;

	public ModelsShaderProvider(GameAssetManager assetsManager,
								FrameBuffer shadowFrameBuffer,
								Decal playerDecal) {
		shaderConfig = new DefaultShader.Config();
		shaderConfig.vertexShader = assetsManager.getShader(Assets.Shaders.MODEL_VERTEX);
		shaderConfig.fragmentShader = assetsManager.getShader(Assets.Shaders.MODEL_FRAGMENT);
		this.shadowFrameBuffer = shadowFrameBuffer;
		this.playerDecal = playerDecal;
	}

	@Override
	protected Shader createShader(final Renderable renderable) {
		return new ModelsShader(renderable, shaderConfig, shadowFrameBuffer, playerDecal);
	}
}
