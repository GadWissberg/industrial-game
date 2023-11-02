package com.gadarts.industrial.systems.render.shaders.models;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.floor.FloorComponent;
import com.gadarts.industrial.components.mi.AdditionalRenderData;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
import com.gadarts.industrial.systems.render.shaders.ShaderUtils;

import static com.gadarts.industrial.components.ComponentsMapper.*;
import static com.gadarts.industrial.systems.render.shaders.ShaderUtils.X_RAY_PLAYER_DISTANCE_CHECK_BIAS;

public class ModelsShader extends DefaultShader {
	public static final int NEARBY_SIMPLE_SHADOW_VECTOR_SIZE = 3;
	private static final int MAX_NEARBY_CHARACTERS = 2;
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final BoundingBox auxBoundingBox = new BoundingBox();
	private static final Vector2 auxVector2 = new Vector2();
	private final ModelsShaderLightsHandler modelsShaderLightsHandler = new ModelsShaderLightsHandler();
	private final float[] nearbySimpleShadowsData = new float[MAX_NEARBY_CHARACTERS * NEARBY_SIMPLE_SHADOW_VECTOR_SIZE];
	private final FrameBuffer shadowFrameBuffer;
	private final ModelsShaderUniformsLocations locations = new ModelsShaderUniformsLocations();
	private final Decal playerDecal;
	private final ModelInstance cursorModelInstance;

	public ModelsShader(Renderable renderable,
						Config mainShaderConfig,
						FrameBuffer shadowFrameBuffer,
						Decal playerDecal,
						ModelInstance cursorModelInstance) {
		super(renderable, mainShaderConfig);
		this.shadowFrameBuffer = shadowFrameBuffer;
		this.playerDecal = playerDecal;
		this.cursorModelInstance = cursorModelInstance;
	}

	private static Vector3 getNearbySimpleShadowPosition(Entity nearby) {
		Vector3 pos;
		if (ComponentsMapper.characterDecal.has(nearby)) {
			pos = ComponentsMapper.characterDecal.get(nearby).getDecal().getPosition();
		} else {
			pos = ComponentsMapper.modelInstance.get(nearby).getModelInstance().transform.getTranslation(auxVector3_1);
		}
		return pos;
	}

	@Override
	public void init( ) {
		super.init();
		locations.init(program);
		if (!program.getLog().isEmpty()) {
			Gdx.app.log("Shader Compilation:", program.getLog());
		}
	}

	@Override
	public void render(Renderable renderable) {
		int textureNum = context.textureBinder.bind(shadowFrameBuffer.getColorBufferTexture());
		program.bind();
		program.setUniformi(locations.getUniformLocShadows(), textureNum);
		program.setUniformf(locations.getUniformLocScreenWidth(), Gdx.graphics.getWidth());
		program.setUniformf(locations.getUniformLocScreenHeight(), Gdx.graphics.getHeight());
		Entity entity = (Entity) renderable.userData;
		insertAdditionalRenderData(renderable, entity);
		super.render(renderable);
	}


	private void insertAdditionalRenderData(Renderable renderable,
											Entity renderedEntity) {
		if (renderedEntity == null) return;

		ModelInstanceComponent modelInstanceComponent = modelInstance.get(renderedEntity);
		AdditionalRenderData additionalRenderData = modelInstanceComponent.getModelInstance().getAdditionalRenderData();
		modelsShaderLightsHandler.applyLights(additionalRenderData, program, locations);
		program.setUniformf(locations.getUniformLocAffectedByLight(), additionalRenderData.isAffectedByLight() ? 1F : 0F);
		insertModelDimensions(additionalRenderData, renderedEntity);
		insertDataForSpecificModels(renderable);
		insertFlatColor(modelInstanceComponent);
		int uniformLocPlayerScreenCoords = locations.getUniformLocPlayerScreenCoords();
		Vector3 position = playerDecal.getPosition();
		Vector2 playerXray = ShaderUtils.calculateXRay(renderedEntity, position, camera, auxVector2, X_RAY_PLAYER_DISTANCE_CHECK_BIAS);
		program.setUniformf(uniformLocPlayerScreenCoords, playerXray);
		int uniformLocMouseScreenCoords = locations.getUniformLocMouseScreenCoords();
		Vector3 cursorPosition = cursorModelInstance.transform.getTranslation(auxVector3_1);
		Vector2 mouseXray = ShaderUtils.calculateXRay(renderedEntity, cursorPosition, camera, auxVector2, 0F);
		program.setUniformf(uniformLocMouseScreenCoords, mouseXray);
	}

	private void insertFlatColor(ModelInstanceComponent modelInstanceComponent) {
		Color flatColor = modelInstanceComponent.getFlatColor();
		Vector3 flatColorVector;
		if (flatColor != null) {
			flatColorVector = auxVector3_1.set(flatColor.r, flatColor.g, flatColor.b);
		} else {
			flatColorVector = auxVector3_1.set(-1F, -1F, -1F);
		}
		program.setUniformf(locations.getUniformLocFlatColor(), flatColorVector);
	}

	private void insertDataForSpecificModels(Renderable renderable) {
		insertDataForFloor(renderable);
		insertDataForWallAndDoor(renderable);
	}

	private void insertDataForWallAndDoor(Renderable renderable) {
		int type;
		boolean grayScale = false;
		Entity entity = (Entity) renderable.userData;
		if (ComponentsMapper.wall.has(entity)) {
			type = 1;
			grayScale = wall.get(entity).isApplyGrayScale();
		} else if (door.has(entity)) {
			type = 2;
		} else {
			type = 0;
		}
		program.setUniformi(locations.getUniformLocEntityType(), type);
		program.setUniformi(locations.getUniformLocGrayScale(), grayScale ? 1 : 0);
	}

	private void insertModelDimensions(AdditionalRenderData additionalRenderData, Entity entity) {
		float width = additionalRenderData.getBoundingBox(auxBoundingBox).getWidth();
		float height = additionalRenderData.getBoundingBox(auxBoundingBox).getHeight();
		float depth = additionalRenderData.getBoundingBox(auxBoundingBox).getDepth();
		program.setUniformf(locations.getUniformLocModelWidth(), width);
		program.setUniformf(locations.getUniformLocModelHeight(), height);
		program.setUniformf(locations.getUniformLocModelDepth(), depth);
		Vector3 translation = modelInstance.get(entity).getModelInstance().transform.getTranslation(auxVector3_1);
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
