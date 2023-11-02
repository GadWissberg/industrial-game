package com.gadarts.industrial.systems.render.shaders.models;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.components.mi.AdditionalRenderData;
import com.gadarts.industrial.components.sll.ShadowlessLightComponent;

import java.util.List;

import static com.gadarts.industrial.components.ComponentsMapper.shadowlessLight;

public class ModelsShaderLightsHandler {
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final int MAX_LIGHTS = 16;
	private static final int LIGHT_EXTRA_DATA_SIZE = 3;
	private static final Color auxColor = new Color();
	private final float[] lightsPositions = new float[MAX_LIGHTS * 3];
	private final float[] shadowlessLightsExtraData = new float[MAX_LIGHTS * LIGHT_EXTRA_DATA_SIZE];
	private final float[] lightsColors = new float[MAX_LIGHTS * 3];

	private void applyLightsDataUniforms(final AdditionalRenderData renderData, ShaderProgram program, ModelsShaderUniformsLocations locations) {
		int size = renderData.getNearbyLights().size();
		program.setUniform3fv(locations.getUniformLocShadowlessLightsPositions(), lightsPositions, 0, size * 3);
		int extraDataLength = size * LIGHT_EXTRA_DATA_SIZE;
		program.setUniform3fv(locations.getUniformLocShadowlessLightsExtraData(), shadowlessLightsExtraData, 0, extraDataLength);
		program.setUniform3fv(locations.getUniformLocShadowlessLightsColors(), this.lightsColors, 0, size * 3);
	}

	private void insertLightPositionToArray(final List<Entity> nearbyLights, final int i) {
		ShadowlessLightComponent lightComponent = shadowlessLight.get(nearbyLights.get(i));
		Vector3 position = lightComponent.getPosition(auxVector3_1);
		int positionIndex = i * 3;
		lightsPositions[positionIndex] = position.x;
		lightsPositions[positionIndex + 1] = position.y;
		lightsPositions[positionIndex + 2] = position.z;
	}

	private int insertToLightsArray(final List<Entity> nearbyLights, final int i, final int differentColorIndex) {
		insertLightPositionToArray(nearbyLights, i);
		boolean white = insertExtraDataToArray(nearbyLights, i, differentColorIndex);
		if (!white) {
			insertColorToArray(nearbyLights.get(i), differentColorIndex);
		}
		return !white ? differentColorIndex + 1 : differentColorIndex;
	}

	private void insertColorToArray(final Entity light, final int i) {
		ShadowlessLightComponent lightComponent = shadowlessLight.get(light);
		int colorIndex = i * 3;
		Color color = lightComponent.getColor(auxColor);
		lightsColors[colorIndex] = color.r;
		lightsColors[colorIndex + 1] = color.g;
		lightsColors[colorIndex + 2] = color.b;
	}

	private boolean insertExtraDataToArray(final List<Entity> nearbyLights, final int i, final int differentColorIndex) {
		ShadowlessLightComponent lightComponent = shadowlessLight.get(nearbyLights.get(i));
		int extraDataInd = i * LIGHT_EXTRA_DATA_SIZE;
		float intensity = lightComponent.getIntensity();
		float radius = lightComponent.getRadius();
		shadowlessLightsExtraData[extraDataInd] = intensity;
		shadowlessLightsExtraData[extraDataInd + 1] = radius;
		boolean white = lightComponent.getColor(auxColor).equals(Color.WHITE);
		shadowlessLightsExtraData[extraDataInd + 2] = white ? -1F : differentColorIndex;
		return white;
	}

	void applyLights(AdditionalRenderData renderData, ShaderProgram program, ModelsShaderUniformsLocations locations) {
		if (renderData.isAffectedByLight()) {
			program.setUniformi(locations.getUniformLocNumberOfShadowlessLights(), renderData.getNearbyLights().size());
			if (!renderData.getNearbyLights().isEmpty()) {
				int differentColorIndex = 0;
				for (int i = 0; i < Math.min(renderData.getNearbyLights().size(), MAX_LIGHTS); i++) {
					differentColorIndex = insertToLightsArray(renderData.getNearbyLights(), i, differentColorIndex);
				}
				applyLightsDataUniforms(renderData, program, locations);
			}
		}
	}
}
