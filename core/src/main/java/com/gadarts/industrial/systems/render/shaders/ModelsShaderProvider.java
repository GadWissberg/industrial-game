package com.gadarts.industrial.systems.render.shaders;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;

public class ModelsShaderProvider extends DefaultShaderProvider {
	private final DefaultShader.Config shaderConfig;
	private final FrameBuffer shadowFrameBuffer;

	public ModelsShaderProvider(final GameAssetsManager assetsManager, FrameBuffer shadowFrameBuffer) {
		shaderConfig = new DefaultShader.Config();
		shaderConfig.vertexShader = assetsManager.getShader(Assets.Shaders.MODEL_VERTEX);
		shaderConfig.fragmentShader = assetsManager.getShader(Assets.Shaders.MODEL_FRAGMENT);
		this.shadowFrameBuffer = shadowFrameBuffer;
	}

	@Override
	protected Shader createShader(final Renderable renderable) {
		return new ModelsShader(renderable, shaderConfig, shadowFrameBuffer);
	}
}
