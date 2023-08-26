package com.gadarts.industrial.systems.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;

public class NoiseEffectHandler implements Disposable {
	public static final String UNIFORM_TIME = "u_time";
	private final int timeUniformLocation;
	private final ShaderProgram noiseShader;

	public NoiseEffectHandler(String vertexShaderSource, String fragmentShaderSource) {
		this.noiseShader = new ShaderProgram(vertexShaderSource, fragmentShaderSource);
		timeUniformLocation = noiseShader.getUniformLocation(UNIFORM_TIME);
	}


	@Override
	public void dispose( ) {
		noiseShader.dispose();
	}

	public void begin(Batch batch) {
		batch.setShader(noiseShader);
		noiseShader.setUniformi(timeUniformLocation, (int) TimeUtils.millis());
	}

	public void end(Batch batch) {
		batch.setShader(null);
	}
}
