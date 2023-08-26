package com.gadarts.industrial.systems.render.shaders.shadow;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import lombok.Getter;

@Getter
public class ShadowMapShaderUniforms {
	public static final String UNIFORM_DEPTH_MAP_CUBE = "u_depthMapCube";
	public static final String UNIFORM_CAMERA_FAR = "u_cameraFar";
	public static final String UNIFORM_LIGHT_POSITION = "u_lightPosition";
	private static final String UNIFORM_LIGHTS_COLORS = "u_lightColor";
	private static final String UNIFORM_RADIUS = "u_radius";
	private static final String UNIFORM_DEPTH_MAP_SIZE = "u_depthMapSize";
	private static final String UNIFORM_INTENSITY = "u_intensity";
	private static final String UNIFORM_MAX_BIAS = "u_maxBias";
	private static final String UNIFORM_MIN_BIAS = "u_minBias";
	private int uniformLocMaxBias;
	private int uniformLocMinBias;
	private int uniformLocLightColor;
	private int uniformLocCameraFar;
	private int uniformLocDepthMapCube;
	private int uniformLocLightPosition;
	private int uniformLocRadius;
	private int uniformLocIntensity;
	private int uniformLocDepthMapSize;

	void fetchUniformsLocations(ShaderProgram program) {
		uniformLocMaxBias = program.getUniformLocation(UNIFORM_MAX_BIAS);
		uniformLocMinBias = program.getUniformLocation(UNIFORM_MIN_BIAS);
		uniformLocLightColor = program.getUniformLocation(UNIFORM_LIGHTS_COLORS);
		uniformLocCameraFar = program.getUniformLocation(UNIFORM_CAMERA_FAR);
		uniformLocDepthMapCube = program.getUniformLocation(UNIFORM_DEPTH_MAP_CUBE);
		uniformLocLightPosition = program.getUniformLocation(UNIFORM_LIGHT_POSITION);
		uniformLocRadius = program.getUniformLocation(UNIFORM_RADIUS);
		uniformLocIntensity = program.getUniformLocation(UNIFORM_INTENSITY);
		uniformLocDepthMapSize = program.getUniformLocation(UNIFORM_DEPTH_MAP_SIZE);
	}
}
