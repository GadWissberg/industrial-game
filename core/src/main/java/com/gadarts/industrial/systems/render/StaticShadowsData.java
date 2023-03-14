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
import lombok.Getter;
import lombok.Setter;

import static com.gadarts.industrial.shared.assets.Assets.Shaders.*;

@Getter
public class StaticShadowsData implements Disposable {
	private GameFrameBuffer shadowFrameBuffer;
	private ShaderProgram depthShaderProgram;
	private ShaderProgram shadowsShaderProgram;
	private ImmutableArray<Entity> staticLightsEntities;
	private ShaderProgram blurShader;
	private GameFrameBuffer blurTargetA;
	private GameFrameBuffer blurTargetB;
	@Setter
	private boolean take;

	private void createBlurShader(GameAssetManager assetsManager) {
		blurShader = new ShaderProgram(assetsManager.getShader(BLUR_VERTEX), assetsManager.getShader(BLUR_FRAGMENT));
		if (!blurShader.isCompiled()) {
			System.err.println(blurShader.getLog());
			System.exit(0);
		}
		if (blurShader.getLog().length() != 0)
			System.out.println(blurShader.getLog());
		//setup uniforms for our shader
		blurShader.begin();
		blurShader.setUniformf("dir", 0f, 0f);
		blurShader.setUniformf("resolution", getShadowFrameBuffer().getWidth());
		blurShader.setUniformf("radius", 1f);
		blurShader.end();
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
		createBlurShader(assetsManager);
	}

	public void init(GameAssetManager assetsManager, ImmutableArray<Entity> staticLightsEntities) {
		shadowFrameBuffer = new GameFrameBuffer(Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
		blurTargetA = new GameFrameBuffer(Format.RGBA8888,
				Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight(),
				false);
		blurTargetB = new GameFrameBuffer(Format.RGBA8888,
				Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight(),
				false);
		createShaderPrograms(assetsManager);
		this.staticLightsEntities = staticLightsEntities;
	}

	@Override
	public void dispose( ) {
		if (!DebugSettings.ALLOW_STATIC_SHADOWS) return;
		depthShaderProgram.dispose();
		shadowsShaderProgram.dispose();
		blurTargetA.dispose();
		blurTargetB.dispose();
		blurShader.dispose();
		shadowFrameBuffer.dispose();
		for (Entity light : staticLightsEntities) {
			ComponentsMapper.staticLight.get(light).getShadowFrameBuffer().dispose();
		}
	}
}
