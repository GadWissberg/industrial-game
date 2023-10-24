package com.gadarts.industrial.systems.render.shaders.models;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import lombok.Getter;

@Getter
public class ModelsShaderUniformsLocations {
	private static final String UNIFORM_AFFECTED_BY_LIGHT = "u_affectedByLight";
	private static final String UNIFORM_NUMBER_OF_NEARBY_SIMPLE_SHADOWS = "u_numberOfNearbySimpleShadows";
	private static final String UNIFORM_NEARBY_SIMPLE_SHADOWS_DATA = "u_nearbySimpleShadowsData[0]";
	private static final String UNIFORM_FLOOR_AMBIENT_OCCLUSION = "u_floorAmbientOcclusion";
	private static final String UNIFORM_ENTITY_TYPE = "u_entityType";
	private static final String UNIFORM_NUMBER_OF_SHADOWLESS_LIGHTS = "u_numberOfShadowlessLights";
	private static final String UNIFORM_SHADOWLESS_LIGHTS_POSITIONS = "u_shadowlessLightsPositions[0]";
	private static final String UNIFORM_SHADOWLESS_LIGHTS_EXTRA_DATA = "u_shadowlessLightsExtraData[0]";
	private static final String UNIFORM_SHADOWLESS_LIGHTS_COLORS = "u_shadowlessLightsColors[0]";
	private static final String UNIFORM_FLAT_COLOR = "u_flatColor";
	private static final String UNIFORM_FOW_SIGNATURE = "u_fowSignature";
	private static final String UNIFORM_GRAY_SIGNATURE = "u_graySignature";
	private static final String UNIFORM_GRAY_SCALE = "u_grayScale";
	private static final String UNIFORM_MODEL_WIDTH = "u_modelWidth";
	private static final String UNIFORM_MODEL_HEIGHT = "u_modelHeight";
	private static final String UNIFORM_MODEL_DEPTH = "u_modelDepth";
	private static final String UNIFORM_MODEL_X = "u_modelX";
	private static final String UNIFORM_MODEL_Y = "u_modelY";
	private static final String UNIFORM_MODEL_Z = "u_modelZ";
	private static final String UNIFORM_SHADOWS = "u_shadows";
	private static final String UNIFORM_SCREEN_WIDTH = "u_screenWidth";
	private static final String UNIFORM_SCREEN_HEIGHT = "u_screenHeight";
	private static final String UNIFORM_PLAYER_SCREEN_COORDS = "u_playerScreenCoords";
	private static final String UNIFORM_MOUSE_SCREEN_COORDS = "u_mouseScreenCoords";

	private int uniformLocAffectedByLight;
	private int uniformLocNumberOfNearbySimpleShadows;
	private int uniformLocNumberOfShadowlessLights;
	private int uniformLocShadowlessLightsPositions;
	private int uniformLocShadowlessLightsExtraData;
	private int uniformLocShadowlessLightsColors;
	private int uniformLocNearbyCharsData;
	private int uniformLocFloorAmbientOcclusion;
	private int uniformLocModelWidth;
	private int uniformLocModelHeight;
	private int uniformLocModelDepth;
	private int uniformLocModelX;
	private int uniformLocModelY;
	private int uniformLocModelZ;
	private int uniformLocEntityType;
	private int uniformLocGrayScale;
	private int uniformLocFlatColor;
	private int uniformLocFowSignature;
	private int uniformLocGraySignature;
	private int uniformLocShadows;
	private int uniformLocScreenWidth;
	private int uniformLocScreenHeight;
	private int uniformLocPlayerScreenCoords;
	private int uniformLocMouseScreenCoords;

	public void init(ShaderProgram program) {
		uniformLocAffectedByLight = program.getUniformLocation(UNIFORM_AFFECTED_BY_LIGHT);
		uniformLocNumberOfNearbySimpleShadows = program.getUniformLocation(UNIFORM_NUMBER_OF_NEARBY_SIMPLE_SHADOWS);
		uniformLocNearbyCharsData = program.getUniformLocation(UNIFORM_NEARBY_SIMPLE_SHADOWS_DATA);
		uniformLocFloorAmbientOcclusion = program.getUniformLocation(UNIFORM_FLOOR_AMBIENT_OCCLUSION);
		uniformLocEntityType = program.getUniformLocation(UNIFORM_ENTITY_TYPE);
		uniformLocModelWidth = program.getUniformLocation(UNIFORM_MODEL_WIDTH);
		uniformLocModelHeight = program.getUniformLocation(UNIFORM_MODEL_HEIGHT);
		uniformLocModelDepth = program.getUniformLocation(UNIFORM_MODEL_DEPTH);
		uniformLocModelX = program.getUniformLocation(UNIFORM_MODEL_X);
		uniformLocModelY = program.getUniformLocation(UNIFORM_MODEL_Y);
		uniformLocModelZ = program.getUniformLocation(UNIFORM_MODEL_Z);
		uniformLocNumberOfShadowlessLights = program.getUniformLocation(UNIFORM_NUMBER_OF_SHADOWLESS_LIGHTS);
		uniformLocShadowlessLightsPositions = program.getUniformLocation(UNIFORM_SHADOWLESS_LIGHTS_POSITIONS);
		uniformLocShadowlessLightsExtraData = program.getUniformLocation(UNIFORM_SHADOWLESS_LIGHTS_EXTRA_DATA);
		uniformLocShadowlessLightsColors = program.getUniformLocation(UNIFORM_SHADOWLESS_LIGHTS_COLORS);
		uniformLocFlatColor = program.getUniformLocation(UNIFORM_FLAT_COLOR);
		uniformLocFowSignature = program.getUniformLocation(UNIFORM_FOW_SIGNATURE);
		uniformLocGraySignature = program.getUniformLocation(UNIFORM_GRAY_SIGNATURE);
		uniformLocGrayScale = program.getUniformLocation(UNIFORM_GRAY_SCALE);
		uniformLocShadows = program.getUniformLocation(UNIFORM_SHADOWS);
		uniformLocScreenWidth = program.getUniformLocation(UNIFORM_SCREEN_WIDTH);
		uniformLocScreenHeight = program.getUniformLocation(UNIFORM_SCREEN_HEIGHT);
		uniformLocPlayerScreenCoords = program.getUniformLocation(UNIFORM_PLAYER_SCREEN_COORDS);
		uniformLocMouseScreenCoords = program.getUniformLocation(UNIFORM_MOUSE_SCREEN_COORDS);
	}
}
