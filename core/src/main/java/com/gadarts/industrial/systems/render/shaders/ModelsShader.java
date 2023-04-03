package com.gadarts.industrial.systems.render.shaders;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.sll.ShadowlessLightComponent;
import com.gadarts.industrial.components.floor.FloorComponent;
import com.gadarts.industrial.components.mi.AdditionalRenderData;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;

import java.util.List;

import static com.gadarts.industrial.components.ComponentsMapper.*;

public class ModelsShader extends DefaultShader {

	public static final int NEARBY_SIMPLE_SHADOW_VECTOR_SIZE = 3;
	private static final int MAX_LIGHTS = 16;
	private static final int LIGHT_EXTRA_DATA_SIZE = 3;
	private static final int MAX_NEARBY_CHARACTERS = 2;
	private static final Vector3 auxVector = new Vector3();
	private static final Color auxColor = new Color();
	private static final BoundingBox auxBoundingBox = new BoundingBox();
	private final float[] lightsPositions = new float[MAX_LIGHTS * 3];
	private final float[] shadowlessLightsExtraData = new float[MAX_LIGHTS * LIGHT_EXTRA_DATA_SIZE];
	private final float[] lightsColors = new float[MAX_LIGHTS * 3];
	private final float[] nearbySimpleShadowsData = new float[MAX_NEARBY_CHARACTERS * NEARBY_SIMPLE_SHADOW_VECTOR_SIZE];
	private final FrameBuffer shadowFrameBuffer;
	private final ModelsShaderUniformsLocations locations = new ModelsShaderUniformsLocations();

	public ModelsShader(Renderable renderable, Config mainShaderConfig, FrameBuffer shadowFrameBuffer) {
		super(renderable, mainShaderConfig);
		this.shadowFrameBuffer = shadowFrameBuffer;
	}

	private static Vector3 getNearbySimpleShadowPosition(Entity nearby) {
		Vector3 pos;
		if (ComponentsMapper.characterDecal.has(nearby)) {
			pos = ComponentsMapper.characterDecal.get(nearby).getDecal().getPosition();
		} else {
			pos = ComponentsMapper.modelInstance.get(nearby).getModelInstance().transform.getTranslation(auxVector);
		}
		return pos;
	}

	@Override
	public void init( ) {
		super.init();
		locations.init(program);
		if (program.getLog().length() != 0) {
			Gdx.app.log("Shader Compilation:", program.getLog());
		}
	}

	private void applyLights(final AdditionalRenderData renderData) {
		if (renderData.isAffectedByLight()) {
			program.setUniformi(locations.getUniformLocNumberOfShadowlessLights(), renderData.getNearbyLights().size());
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
		program.setUniform3fv(locations.getUniformLocShadowlessLightsPositions(), lightsPositions, 0, size * 3);
		int extraDataLength = size * LIGHT_EXTRA_DATA_SIZE;
		program.setUniform3fv(locations.getUniformLocShadowlessLightsExtraData(), shadowlessLightsExtraData, 0, extraDataLength);
		program.setUniform3fv(locations.getUniformLocShadowlessLightsColors(), this.lightsColors, 0, size * 3);
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

	@Override
	public void render(Renderable renderable) {
		int textureNum = context.textureBinder.bind(shadowFrameBuffer.getColorBufferTexture());
		program.bind();
		program.setUniformi("u_shadows", textureNum);
		program.setUniformf("u_screenWidth", Gdx.graphics.getWidth());
		program.setUniformf("u_screenHeight", Gdx.graphics.getHeight());
		Entity entity = (Entity) renderable.userData;
		insertAdditionalRenderData(renderable, entity);
		super.render(renderable);
	}

	private void insertAdditionalRenderData(Renderable renderable,
											Entity entity) {
		ModelInstanceComponent modelInstanceComponent = modelInstance.get(entity);
		AdditionalRenderData additionalRenderData = modelInstanceComponent.getModelInstance().getAdditionalRenderData();
		applyLights(additionalRenderData);
		program.setUniformf(locations.getUniformLocAffectedByLight(), additionalRenderData.isAffectedByLight() ? 1F : 0F);
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
		program.setUniformf(locations.getUniformLocFlatColor(), flatColorVector);
	}

	private void insertDataForSpecificModels(Renderable renderable) {
		insertDataForFloor(renderable);
		insertDataForWallAndDoor(renderable);
	}

	private void insertDataForWallAndDoor(Renderable renderable) {
		int type;
		if (ComponentsMapper.wall.has((Entity) renderable.userData)) {
			type = 1;
		} else if (door.has((Entity) renderable.userData)) {
			type = 2;
		} else {
			type = 0;
		}
		program.setUniformi(locations.getUniformLocEntityType(), type);
	}

	private void insertModelDimensions(AdditionalRenderData additionalRenderData, Entity entity) {
		float width = additionalRenderData.getBoundingBox(auxBoundingBox).getWidth();
		float height = additionalRenderData.getBoundingBox(auxBoundingBox).getHeight();
		float depth = additionalRenderData.getBoundingBox(auxBoundingBox).getDepth();
		program.setUniformf(locations.getUniformLocModelWidth(), width);
		program.setUniformf(locations.getUniformLocModelHeight(), height);
		program.setUniformf(locations.getUniformLocModelDepth(), depth);
		Vector3 translation = modelInstance.get(entity).getModelInstance().transform.getTranslation(auxVector);
		insertModelPosition(entity, translation);
	}

	private void insertModelPosition(Entity entity, Vector3 translation) {
		program.setUniformf(locations.getUniformLocModelX(), translation.x);
		insertModelY(entity, translation);
		program.setUniformf(locations.getUniformLocModelZ(), translation.z);
	}

	private void insertModelY(Entity entity, Vector3 translation) {
		float y;
		if (!wall.has(entity)) {
			y = translation.y;
		} else {
			y = wall.get(entity).getParentNode().getHeight();
		}
		program.setUniformf(locations.getUniformLocModelY(), y);
	}

	private void insertDataForFloor(Renderable renderable) {
		Entity entity = (Entity) renderable.userData;
		Integer graySignature = 0;
		if (modelInstance.has(entity)) {
			graySignature = ComponentsMapper.modelInstance.get(entity).getGraySignature();
		}
		program.setUniformi(locations.getUniformLocGraySignature(), graySignature != null ? graySignature : 0);
		if (floor.has(entity)) {
			FloorComponent floorComponent = floor.get(entity);
			int size = floorComponent.getNearbySimpleShadows().size();
			program.setUniformi(locations.getUniformLocNumberOfNearbySimpleShadows(), size);
			initializeNearbySimpleShadowsPositions(renderable, size);
			int length = size * NEARBY_SIMPLE_SHADOW_VECTOR_SIZE;
			program.setUniform3fv(locations.getUniformLocNearbyCharsData(), this.nearbySimpleShadowsData, 0, length);
			int nodeAmbientOcclusionValue = floorComponent.getNode().getNodeAmbientOcclusionValue();
			program.setUniformi(locations.getUniformLocFloorAmbientOcclusion(), nodeAmbientOcclusionValue);
			int signature = floorComponent.getFogOfWarSignature();
			program.setUniformi(locations.getUniformLocFowSignature(), signature);
		} else {
			program.setUniformi(locations.getUniformLocNumberOfNearbySimpleShadows(), 0);
			program.setUniformi(locations.getUniformLocFloorAmbientOcclusion(), 0);
			program.setUniformi(locations.getUniformLocFowSignature(), 0);
		}
	}

	private void initializeNearbySimpleShadowsPositions(Renderable renderable, int size) {
		for (int i = 0; i < size; i++) {
			FloorComponent floorComponent = floor.get((Entity) renderable.userData);
			Entity nearby = floorComponent.getNearbySimpleShadows().get(i);
			Vector3 pos = getNearbySimpleShadowPosition(nearby);
			nearbySimpleShadowsData[i * NEARBY_SIMPLE_SHADOW_VECTOR_SIZE] = pos.x;
			nearbySimpleShadowsData[i * NEARBY_SIMPLE_SHADOW_VECTOR_SIZE + 1] = pos.z;
			nearbySimpleShadowsData[i * NEARBY_SIMPLE_SHADOW_VECTOR_SIZE + 2] = simpleShadow.get(nearby).getRadius();
		}
	}
}
