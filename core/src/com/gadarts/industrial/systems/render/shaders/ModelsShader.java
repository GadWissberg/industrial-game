package com.gadarts.industrial.systems.render.shaders;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.ShadowlessLightComponent;
import com.gadarts.industrial.components.floor.FloorComponent;
import com.gadarts.industrial.components.mi.AdditionalRenderData;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;

import java.util.List;

import static com.badlogic.gdx.graphics.GL20.GL_BACK;
import static com.gadarts.industrial.components.ComponentsMapper.*;

public class ModelsShader extends DefaultShader {

	private static final String UNIFORM_AFFECTED_BY_LIGHT = "u_affectedByLight";
	private static final String UNIFORM_NUMBER_OF_NEARBY_CHARACTERS = "u_numberOfNearbyCharacters";
	private static final String UNIFORM_NEARBY_CHARACTERS_POSITIONS = "u_nearbyCharactersPositions[0]";
	private static final String UNIFORM_FLOOR_AMBIENT_OCCLUSION = "u_floorAmbientOcclusion";
	private static final String UNIFORM_IS_WALL = "u_isWall";
	private static final String UNIFORM_NUMBER_OF_SHADOWLESS_LIGHTS = "u_numberOfShadowlessLights";
	private static final String UNIFORM_SHADOWLESS_LIGHTS_POSITIONS = "u_shadowlessLightsPositions[0]";
	private static final String UNIFORM_SHADOWLESS_LIGHTS_EXTRA_DATA = "u_shadowlessLightsExtraData[0]";
	private static final String UNIFORM_SHADOWLESS_LIGHTS_COLORS = "u_shadowlessLightsColors[0]";
	private static final String UNIFORM_FLAT_COLOR = "u_flatColor";
	private static final String UNIFORM_FOW_SIGNATURE = "u_fowSignature";
	private static final String UNIFORM_MODEL_WIDTH = "u_modelWidth";
	private static final String UNIFORM_MODEL_HEIGHT = "u_modelHeight";
	private static final String UNIFORM_MODEL_DEPTH = "u_modelDepth";
	private static final String UNIFORM_MODEL_X = "u_modelX";
	private static final String UNIFORM_MODEL_Y = "u_modelY";
	private static final String UNIFORM_MODEL_Z = "u_modelZ";
	private static final int MAX_LIGHTS = 16;
	private static final Vector3 auxVector = new Vector3();
	private static final int LIGHT_EXTRA_DATA_SIZE = 3;
	private static final Color auxColor = new Color();
	private static final int MAX_NEARBY_CHARACTERS = 2;
	private static final BoundingBox auxBoundingBox = new BoundingBox();
	private final float[] lightsPositions = new float[MAX_LIGHTS * 3];
	private final float[] lightsExtraData = new float[MAX_LIGHTS * LIGHT_EXTRA_DATA_SIZE];
	private final float[] lightsColors = new float[MAX_LIGHTS * 3];
	private final float[] nearbyCharactersPositions = new float[MAX_NEARBY_CHARACTERS * 2];
	private final FrameBuffer shadowFrameBuffer;
	private int uniformLocAffectedByLight;
	private int uniformLocNumberOfNearbyCharacters;
	private int uniformLocNumberOfShadowlessLights;
	private int uniformLocShadowlessLightsPositions;
	private int uniformLocShadowlessLightsExtraData;
	private int uniformLocShadowlessLightsColors;
	private int uniformLocNearbyCharactersPositions;
	private int uniformLocFloorAmbientOcclusion;
	private int uniformLocModelWidth;
	private int uniformLocModelHeight;
	private int uniformLocModelDepth;
	private int uniformLocModelX;
	private int uniformLocModelY;
	private int uniformLocModelZ;
	private int uniformLocIsWall;
	private int uniformLocFlatColor;
	private int uniformLocFowSignature;

	public ModelsShader(Renderable renderable, Config mainShaderConfig, FrameBuffer shadowFrameBuffer) {
		super(renderable, mainShaderConfig);
		this.shadowFrameBuffer = shadowFrameBuffer;
	}

	@Override
	public void init( ) {
		super.init();
		final int textureNum = 30;
		shadowFrameBuffer.getColorBufferTexture().bind(textureNum);
		program.bind();
		program.setUniformi("u_shadows", textureNum);
		program.setUniformf("u_screenWidth", Gdx.graphics.getWidth());
		program.setUniformf("u_screenHeight", Gdx.graphics.getHeight());
		uniformLocAffectedByLight = program.getUniformLocation(UNIFORM_AFFECTED_BY_LIGHT);
		uniformLocNumberOfNearbyCharacters = program.getUniformLocation(UNIFORM_NUMBER_OF_NEARBY_CHARACTERS);
		uniformLocNearbyCharactersPositions = program.getUniformLocation(UNIFORM_NEARBY_CHARACTERS_POSITIONS);
		uniformLocFloorAmbientOcclusion = program.getUniformLocation(UNIFORM_FLOOR_AMBIENT_OCCLUSION);
		uniformLocIsWall = program.getUniformLocation(UNIFORM_IS_WALL);
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
		if (program.getLog().length() != 0) {
			Gdx.app.log("Shader Compilation:", program.getLog());
		}
	}

	private void applyLights(final AdditionalRenderData renderData) {
		if (renderData.isAffectedByLight()) {
			program.setUniformi(uniformLocNumberOfShadowlessLights, renderData.getNearbyLights().size());
			if (!renderData.getNearbyLights().isEmpty()) {
				int differentColorIndex = 0;
				for (int i = 0; i < Math.min(renderData.getNearbyLights().size(), MAX_LIGHTS); i++) {
					differentColorIndex = insertToLightsArray(renderData.getNearbyLights(), i, differentColorIndex);
				}
				applyLightsDataUniforms(renderData);
			}
		}
	}

	private void applyLightsDataUniforms(final AdditionalRenderData renderData) {
		int size = renderData.getNearbyLights().size();
		program.setUniform3fv(uniformLocShadowlessLightsPositions, lightsPositions, 0, size * 3);
		int extraDataLength = size * LIGHT_EXTRA_DATA_SIZE;
		program.setUniform3fv(uniformLocShadowlessLightsExtraData, lightsExtraData, 0, extraDataLength);
		program.setUniform3fv(uniformLocShadowlessLightsColors, this.lightsColors, 0, size * 3);
	}

	private void insertLightPositionToArray(final List<Entity> nearbyLights, final int i) {
		ShadowlessLightComponent lightComponent = shadowlessLight.get(nearbyLights.get(i));
		Vector3 position = lightComponent.getPosition(auxVector);
		int positionIndex = i * 3;
		lightsPositions[positionIndex] = position.x;
		lightsPositions[positionIndex + 1] = position.y;
		lightsPositions[positionIndex + 2] = position.z;
	}

	private int insertToLightsArray(final List<Entity> nearbyLights, final int i, final int differentColorIndex) {
		insertLightPositionToArray(nearbyLights, i);
		boolean notWhite = insertExtraDataToArray(nearbyLights, i, differentColorIndex);
		if (notWhite) {
			insertColorToArray(nearbyLights.get(i), differentColorIndex);
		}
		return notWhite ? differentColorIndex + 1 : differentColorIndex;
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
		lightsExtraData[extraDataInd] = intensity;
		lightsExtraData[extraDataInd + 1] = radius;
		boolean notWhite = lightComponent.getColor(auxColor).equals(Color.WHITE);
		lightsExtraData[extraDataInd + 2] = notWhite ? differentColorIndex : -1F;
		return notWhite;
	}

	@Override
	public void render(Renderable renderable) {
		Entity entity = (Entity) renderable.userData;
		insertAdditionalRenderData(renderable, entity);
		boolean isWall = wall.has(entity);
		if (isWall) {
			Gdx.gl.glDisable(GL20.GL_CULL_FACE);
		}
		super.render(renderable);
		if (isWall) {
			Gdx.gl.glEnable(GL20.GL_CULL_FACE);
		}
	}

	private void insertAdditionalRenderData(Renderable renderable,
											Entity entity) {
		ModelInstanceComponent modelInstanceComponent = modelInstance.get(entity);
		AdditionalRenderData additionalRenderData = modelInstanceComponent.getModelInstance().getAdditionalRenderData();
		applyLights(additionalRenderData);
		program.setUniformf(uniformLocAffectedByLight, additionalRenderData.isAffectedByLight() ? 1F : 0F);
		insertModelDimensions(additionalRenderData, entity);
		insertDataForSpecificModels(renderable);
		insertFlatColor(modelInstanceComponent);
	}

	private void insertFlatColor(ModelInstanceComponent modelInstanceComponent) {
		Color flatColor = modelInstanceComponent.getFlatColor();
		Vector3 flatColorVector;
		if (flatColor != null) {
			flatColorVector = auxVector.set(flatColor.r, flatColor.g, flatColor.b);
		} else {
			flatColorVector = auxVector.set(-1F, -1F, -1F);
		}
		program.setUniformf(uniformLocFlatColor, flatColorVector);
	}

	private void insertDataForSpecificModels(Renderable renderable) {
		insertDataForFloor(renderable);
		insertDataForWall(renderable);
	}

	private void insertDataForWall(Renderable renderable) {
		Entity wall = (Entity) renderable.userData;
		if (ComponentsMapper.wall.has(wall)) {
			program.setUniformi(uniformLocIsWall, 1);
		} else {
			program.setUniformi(uniformLocIsWall, 0);
		}
	}

	private void insertModelDimensions(AdditionalRenderData additionalRenderData, Entity entity) {
		float width = additionalRenderData.getBoundingBox(auxBoundingBox).getWidth();
		float height = additionalRenderData.getBoundingBox(auxBoundingBox).getHeight();
		float depth = additionalRenderData.getBoundingBox(auxBoundingBox).getDepth();
		program.setUniformf(uniformLocModelWidth, width);
		program.setUniformf(uniformLocModelHeight, height);
		program.setUniformf(uniformLocModelDepth, depth);
		Vector3 translation = modelInstance.get(entity).getModelInstance().transform.getTranslation(auxVector);
		insertModelPosition(entity, translation);
	}

	private void insertModelPosition(Entity entity, Vector3 translation) {
		program.setUniformf(uniformLocModelX, translation.x);
		insertModelY(entity, translation);
		program.setUniformf(uniformLocModelZ, translation.z);
	}

	private void insertModelY(Entity entity, Vector3 translation) {
		float y;
		if (!wall.has(entity)) {
			y = translation.y;
		} else {
			y = wall.get(entity).getParentNode().getHeight();
		}
		program.setUniformf(uniformLocModelY, y);
	}

	private void insertDataForFloor(Renderable renderable) {
		if (floor.has((Entity) renderable.userData)) {
			FloorComponent floorComponent = floor.get((Entity) renderable.userData);
			int size = floorComponent.getNearbyCharacters().size();
			program.setUniformi(uniformLocNumberOfNearbyCharacters, size);
			initializeNearbyCharactersPositions(renderable, size);
			program.setUniform2fv(uniformLocNearbyCharactersPositions, this.nearbyCharactersPositions, 0, size * 2);
			program.setUniformi(uniformLocFloorAmbientOcclusion, floorComponent.getNode().getNodeAmbientOcclusionValue());
			program.setUniformi(uniformLocFowSignature, floorComponent.getFogOfWarSignature());
		} else {
			program.setUniformi(uniformLocNumberOfNearbyCharacters, 0);
			program.setUniformi(uniformLocFloorAmbientOcclusion, 0);
			program.setUniformi(uniformLocFowSignature, 0);
		}
	}

	private void initializeNearbyCharactersPositions(Renderable renderable, int size) {
		for (int i = 0; i < size; i++) {
			FloorComponent floorComponent = floor.get((Entity) renderable.userData);
			Vector3 position = characterDecal.get(floorComponent.getNearbyCharacters().get(i)).getDecal().getPosition();
			nearbyCharactersPositions[i * 2] = position.x;
			nearbyCharactersPositions[i * 2 + 1] = position.z;
		}
	}
}
