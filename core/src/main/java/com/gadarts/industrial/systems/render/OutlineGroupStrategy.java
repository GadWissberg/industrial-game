package com.gadarts.industrial.systems.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;

public class OutlineGroupStrategy extends GameCameraGroupStrategy {
	public OutlineGroupStrategy(Camera camera, GameAssetsManager assetsManager) {
		super(camera, assetsManager);
	}

	@Override
	public ShaderProgram getGroupShader(int group) {
		return super.getGroupShader(group);
	}

	@Override
	void createDefaultShader(GameAssetsManager assetsManager) {
		String vertexShader = assetsManager.getShader(Assets.Shaders.DECAL_VERTEX);
		String fragmentShader = assetsManager.getShader(Assets.Shaders.DECAL_OUTLINE_FRAGMENT);
		shader = new ShaderProgram(vertexShader, fragmentShader);
		if (!shader.isCompiled())
			throw new IllegalArgumentException(String.format(MSG_FAILED_TO_COMPILE, shader.getLog()));
	}

	@Override
	public void beforeGroups( ) {
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
		shader.bind();
		shader.setUniformMatrix("u_projectionViewMatrix", camera.combined);
		shader.setUniformi("u_texture", 0);
	}
}
