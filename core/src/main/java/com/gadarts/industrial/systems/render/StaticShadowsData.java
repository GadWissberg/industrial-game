package com.gadarts.industrial.systems.render;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.systems.render.fb.GameFrameBuffer;
import lombok.Getter;
import lombok.Setter;

import static com.gadarts.industrial.shared.assets.Assets.Shaders.*;

@Getter
public class StaticShadowsData implements Disposable {
	private GameFrameBuffer shadowFrameBuffer;
	private ShaderProgram depthShaderProgram;
	private ShaderProgram shadowsShaderProgram;
	private ImmutableArray<Entity> staticLightsEntities;
	@Setter
	private boolean take;

	public void init(GameAssetManager assetsManager, ImmutableArray<Entity> staticLightsEntities) {
		shadowFrameBuffer = new GameFrameBuffer(Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
		createShaderPrograms(assetsManager);
		this.staticLightsEntities = staticLightsEntities;
	}

	@Override
	public void dispose( ) {
		if (!DebugSettings.ALLOW_STATIC_SHADOWS) return;
		depthShaderProgram.dispose();
		shadowsShaderProgram.dispose();
		shadowFrameBuffer.dispose();
		for (Entity light : staticLightsEntities) {
			ComponentsMapper.staticLight.get(light).getShadowFrameBuffer().dispose();
		}
	}

	void handleScreenshot(FrameBuffer frameBuffer) {
		if (take) {
			ScreenshotFactory.saveScreenshot(frameBuffer.getWidth(), frameBuffer.getHeight(), "depthmap");
			take = false;
		}
	}

	private void createShaderPrograms(GameAssetManager assetsManager) {
		depthShaderProgram = new ShaderProgram(
				assetsManager.getShader(DEPTHMAP_VERTEX),
				assetsManager.getShader(DEPTHMAP_FRAGMENT));
		shadowsShaderProgram = new ShaderProgram(
				assetsManager.getShader(SHADOW_VERTEX),
				assetsManager.getShader(SHADOW_FRAGMENT));
	}
}
