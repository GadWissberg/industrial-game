package com.gadarts.industrial.systems.render.shaders;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;

public class MainShaderProvider extends DefaultShaderProvider {
	private final DefaultShader.Config mainShaderConfig;
	private final FrameBuffer shadowFrameBuffer;

	public MainShaderProvider(final GameAssetsManager assetsManager, FrameBuffer shadowFrameBuffer) {
		mainShaderConfig = new DefaultShader.Config();
		mainShaderConfig.vertexShader = assetsManager.getShader(Assets.Shaders.VERTEX);
		mainShaderConfig.fragmentShader = assetsManager.getShader(Assets.Shaders.FRAGMENT);
		this.shadowFrameBuffer = shadowFrameBuffer;
	}

	@Override
	protected Shader createShader(final Renderable renderable) {
		return new ModelsShader(renderable, mainShaderConfig, shadowFrameBuffer);
	}
}
