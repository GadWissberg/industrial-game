package com.gadarts.industrial.systems.render;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.TimeUtils;
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.AppendixModelInstanceComponent;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.LightComponent;
import com.gadarts.industrial.components.StaticLightComponent;
import com.gadarts.industrial.components.animation.AnimationComponent;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.CharacterAnimation;
import com.gadarts.industrial.components.character.CharacterAnimations;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.character.CharacterSpriteData;
import com.gadarts.industrial.components.floor.FloorComponent;
import com.gadarts.industrial.components.mi.AdditionalRenderData;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
import com.gadarts.industrial.components.sd.RelatedDecal;
import com.gadarts.industrial.components.sd.SimpleDecalComponent;
import com.gadarts.industrial.components.sll.ShadowlessLightComponent;
import com.gadarts.industrial.components.sll.ShadowlessLightOriginalData;
import com.gadarts.industrial.console.commands.ConsoleCommandParameter;
import com.gadarts.industrial.console.commands.ConsoleCommandResult;
import com.gadarts.industrial.console.commands.ConsoleCommands;
import com.gadarts.industrial.console.commands.ConsoleCommandsList;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import com.gadarts.industrial.shared.utils.CharacterUtils;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.input.InputSystemEventsSubscriber;

import java.util.ArrayList;
import java.util.List;

import static com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import static com.gadarts.industrial.shared.model.characters.SpriteType.*;
import static com.gadarts.industrial.systems.SystemsCommonData.CAMERA_LIGHT_FAR;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class RenderSystem extends GameSystem<RenderSystemEventsSubscriber> implements
		InputSystemEventsSubscriber {
	public static final float LIGHT_MAX_RADIUS = 7f;
	public static final float FLICKER_RANDOM_MIN = 0.95F;
	public static final float FLICKER_RANDOM_MAX = 1.05F;
	public static final int DEPTH_MAP_SIZE = 1024;
	public static final float OUTLINE_ALPHA = 0.4F;
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final Vector3 auxVector3_2 = new Vector3();
	private static final Vector3 auxVector3_3 = new Vector3();
	private static final BoundingBox auxBoundingBox = new BoundingBox();
	private static final float DECAL_DARKEST_COLOR = 0.2f;
	private static final Color auxColor = new Color();
	private static final float DECAL_LIGHT_OFFSET = 1.5f;
	private static final List<Entity> auxLightsListToRemove = new ArrayList<>();
	private static final int FLICKER_MAX_INTERVAL = 150;
	private static final Circle auxCircle = new Circle();
	private static final Rectangle auxRect = new Rectangle();
	private static final Color PLAYER_OUTLINE_COLOR = Color.valueOf("#177331");
	private static final Color ENEMY_OUTLINE_COLOR = Color.valueOf("#731717");
	private static final int MAX_SIMPLE_SHADOWS_PER_NODE = 2;
	private final RenderBatches renderBatches = new RenderBatches();
	private final RenderSystemRelevantFamilies families = new RenderSystemRelevantFamilies();
	private final StaticShadowsData staticShadowsData = new StaticShadowsData();
	private final DecalsGroupStrategies strategies = new DecalsGroupStrategies();
	private GameEnvironment environment;
	private boolean frustumCull = !DebugSettings.DISABLE_FRUSTUM_CULLING;

	public RenderSystem(GameAssetManager assetsManager, GameLifeCycleHandler lifeCycleHandler) {
		super(assetsManager, lifeCycleHandler);
	}


	private static CharacterAnimation fetchCharacterAnimationByDirectionAndType(Entity entity,
																				Direction direction,
																				SpriteType sprType) {
		int randomIndex = MathUtils.random(sprType.getVariations() - 1);
		CharacterAnimation animation = null;
		CharacterAnimations animations = ComponentsMapper.characterDecal.get(entity).getAnimations();
		if (animations.contains(sprType)) {
			animation = animations.get(sprType, randomIndex, direction);
		} else if (ComponentsMapper.player.has(entity)) {
			animation = ComponentsMapper.player.get(entity).getGeneralAnimations().get(sprType, randomIndex, direction);
		}
		return animation;
	}

	private void handleFrustumCullingCommand(final ConsoleCommandResult consoleCommandResult) {
		frustumCull = !frustumCull;
		final String MESSAGE = "Frustum culling has been %s.";
		String msg = frustumCull ? String.format(MESSAGE, "activated") : String.format(MESSAGE, "disabled");
		consoleCommandResult.setMessage(msg);
	}

	@Override
	public boolean onCommandRun(final ConsoleCommands command, final ConsoleCommandResult consoleCommandResult) {
		if (command == ConsoleCommandsList.FRUSTUM_CULLING) {
			handleFrustumCullingCommand(consoleCommandResult);
			return true;
		}
		return false;
	}

	@Override
	public void keyDown(int keycode) {
		if (keycode == Input.Keys.T) {
			staticShadowsData.setTake(true);
		}

	}

	private GameEnvironment createEnvironment( ) {
		final GameEnvironment environment;
		environment = new GameEnvironment();
		float ambient = getSystemsCommonData().getMap().getAmbient();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, ambient, ambient, ambient, 0.1f));
		float dirValue = 0.1F;
		environment.add(new DirectionalLight().set(dirValue, dirValue, dirValue, -1F, -1F, -0.5F));
		return environment;
	}

	@Override
	public void onSystemReset(SystemsCommonData systemsCommonData) {
		super.onSystemReset(systemsCommonData);
		environment = createEnvironment();
		getSystemsCommonData().setDrawFlags(new DrawFlags());
		float ambient = getSystemsCommonData().getMap().getAmbient();
		environment.setAmbientColor(new Color(ambient, ambient, ambient, 1F));
		families.init(getEngine());
	}

	@Override
	public Class<RenderSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return RenderSystemEventsSubscriber.class;
	}

	@Override
	public void initializeData( ) {
		GameAssetManager assetsManager = getAssetsManager();
		staticShadowsData.init(assetsManager, families.getStaticLightsEntities());
		renderBatches.createShaderProvider(assetsManager, staticShadowsData.getShadowFrameBuffer());
		strategies.createDecalGroupStrategies(getSystemsCommonData().getCamera(), assetsManager);
		renderBatches.createBatches(
				staticShadowsData, families.getStaticLightsEntities(),
				strategies.getRegularDecalGroupStrategy());
		if (DebugSettings.ALLOW_STATIC_SHADOWS) {
			createShadowMaps();
		}
	}

	private void resetDisplay( ) {
		resetDisplay(1F);
	}

	private void resetDisplay(float alpha) {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		int sam = Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0;
		Gdx.gl.glClearColor(0, 0, 0, alpha);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | sam);
	}

	private boolean isInFrustum(final Camera camera, ModelInstanceComponent modelInstanceComponent) {
		if (!DebugSettings.DISABLE_FRUSTUM_CULLING) return true;
		Vector3 position = modelInstanceComponent.getModelInstance().transform.getTranslation(auxVector3_1);
		AdditionalRenderData additionalRenderData = modelInstanceComponent.getModelInstance().getAdditionalRenderData();
		BoundingBox boundingBox = additionalRenderData.getBoundingBox(auxBoundingBox);
		Vector3 center = boundingBox.getCenter(auxVector3_3);
		Vector3 dim = auxBoundingBox.getDimensions(auxVector3_2);
		return camera.frustum.boundsInFrustum(position.add(center), dim);
	}

	@Override
	public boolean onCommandRun(final ConsoleCommands command,
								final ConsoleCommandResult consoleCommandResult,
								final ConsoleCommandParameter parameter) {
		boolean result = false;
		if (command == ConsoleCommandsList.SKIP_RENDER) {
			getSystemsCommonData().getDrawFlags().applySkipRenderCommand(parameter);
			result = true;
		}
		return result;
	}


	private void renderModels(ModelBatch modelBatch,
							  ImmutableArray<Entity> entitiesToRender,
							  boolean renderLight,
							  Camera camera) {
		renderModels(modelBatch, entitiesToRender, renderLight, camera, true);
	}

	private void renderModels(ModelBatch modelBatch,
							  ImmutableArray<Entity> entitiesToRender,
							  boolean renderShadowlessLights,
							  Camera camera,
							  boolean considerFow) {
		modelBatch.begin(camera);
		for (Entity entity : entitiesToRender) {
			ModelInstanceComponent modelInstanceComponent = ComponentsMapper.modelInstance.get(entity);
			if (tryRenderingModel(modelBatch, camera, entity, renderShadowlessLights, modelInstanceComponent, considerFow)) {
				renderAppendixModelInstance(modelBatch, entity);
			}
		}
		modelBatch.end();
	}

	private void renderAppendixModelInstance(ModelBatch modelBatch, Entity entity) {
		if (ComponentsMapper.appendixModelInstance.has(entity)) {
			AppendixModelInstanceComponent appendix = ComponentsMapper.appendixModelInstance.get(entity);
			renderModel(modelBatch, entity, true, appendix);
		}
	}

	private void applyShadowlessLightsOnModel(final ModelInstanceComponent mic) {
		List<Entity> nearbyLights = mic.getModelInstance().getAdditionalRenderData().getNearbyLights();
		nearbyLights.clear();
		if (!DebugSettings.DISABLE_LIGHTS) {
			if (mic.getModelInstance().getAdditionalRenderData().isAffectedByLight()) {
				for (Entity light : families.getShadowlessLightsEntities()) {
					addLightIfClose(mic.getModelInstance(), nearbyLights, light);
				}
			}
		}
	}

	private void addLightIfClose(final GameModelInstance modelInstance,
								 final List<Entity> nearbyLights,
								 final Entity light) {
		ShadowlessLightComponent lightComponent = ComponentsMapper.shadowlessLight.get(light);
		Vector3 lightPosition = lightComponent.getPosition(auxVector3_1);
		Vector3 modelPosition = modelInstance.transform.getTranslation(auxVector3_2);
		float distance = lightPosition.dst(modelPosition);
		if (distance <= LIGHT_MAX_RADIUS) {
			nearbyLights.add(light);
		}
	}

	private boolean tryRenderingModel(ModelBatch modelBatch,
									  Camera camera,
									  Entity entity,
									  boolean renderLight,
									  ModelInstanceComponent modelInstanceComponent,
									  boolean considerFow) {
		if (shouldSkipRenderModel(camera, entity, modelInstanceComponent, considerFow)) return false;
		renderModel(modelBatch, entity, renderLight, modelInstanceComponent);
		return true;
	}

	private void renderModel(ModelBatch modelBatch,
							 Entity entity,
							 boolean renderLight,
							 ModelInstanceComponent modelInstanceComponent) {
		GameModelInstance modelInstance = modelInstanceComponent.getModelInstance();
		modelBatch.render(modelInstance, environment);
		getSystemsCommonData().setNumberOfVisible(getSystemsCommonData().getNumberOfVisible() + 1);
		applySpecificRendering(entity);
		if (renderLight) {
			applyShadowlessLightsOnModel(modelInstanceComponent);
		}
	}

	private void applySpecificRendering(Entity entity) {
		if (ComponentsMapper.floor.has(entity)) {
			renderSimpleShadowsOnFloor(entity);
		} else if (ComponentsMapper.wall.has(entity)) {
			applySpecificRenderingForWall(entity);
		}
	}


	private void applySpecificRenderingForWall(Entity entity) {
		if (entity == null) return;

		Entity parentNode = ComponentsMapper.wall.get(entity).getParentNode().getEntity();
		ModelInstanceComponent wallModelInstanceComponent = ComponentsMapper.modelInstance.get(entity);
		wallModelInstanceComponent.setFlatColor(null);
		boolean noParentNode = parentNode == null;
		if (noParentNode
				|| (ComponentsMapper.modelInstance.has(parentNode)
				&& ComponentsMapper.modelInstance.get(parentNode).getFlatColor() != null)) {
			wallModelInstanceComponent.setFlatColor(Color.BLACK);
		} else {
			Integer graySignature = ComponentsMapper.modelInstance.get(parentNode).getGraySignature();
			ComponentsMapper.wall.get(entity).setApplyGrayScale((graySignature & 16) == 16);
		}
	}

	private void renderSimpleShadowsOnFloor(Entity entity) {
		List<Entity> nearbyCharacters = ComponentsMapper.floor.get(entity).getNearbySimpleShadows();
		nearbyCharacters.clear();
		if ((ComponentsMapper.modelInstance.get(entity).getGraySignature() & 16) == 0) {
			for (Entity thing : families.getSimpleShadowEntities()) {
				if (checkIfEntityCastsSimpleShadowOnNode(thing, entity)) {
					nearbyCharacters.add(thing);
					if (nearbyCharacters.size() >= MAX_SIMPLE_SHADOWS_PER_NODE) {
						return;
					}
				}
			}
		}
	}

	private boolean checkIfEntityCastsSimpleShadowOnNode(Entity thing, Entity node) {
		Vector3 pos = getPositionOfEntity(thing);
		auxCircle.set(pos.x, pos.z, CharacterComponent.CHAR_RAD * 3F);
		Vector3 floorPos = ComponentsMapper.modelInstance.get(node).getModelInstance().transform.getTranslation(auxVector3_1);
		auxRect.set(floorPos.x, floorPos.z, 1F, 1F);
		return Intersector.overlaps(auxCircle, auxRect);
	}

	private Vector3 getPositionOfEntity(Entity thing) {
		Vector3 pos;
		if (ComponentsMapper.characterDecal.has(thing)) {
			pos = ComponentsMapper.characterDecal.get(thing).getDecal().getPosition();
		} else {
			pos = ComponentsMapper.modelInstance.get(thing).getModelInstance().transform.getTranslation(auxVector3_1);
		}
		return pos;
	}

	private boolean shouldSkipRenderModel(Camera camera,
										  Entity entity,
										  ModelInstanceComponent miComp,
										  boolean considerFow) {
		return (!miComp.isVisible())
				|| !isInFrustum(camera, miComp)
				|| ComponentsMapper.floor.has(entity) && !getSystemsCommonData().getDrawFlags().isDrawGround()
				|| ComponentsMapper.wall.has(entity) && !getSystemsCommonData().getDrawFlags().isDrawWalls()
				|| ComponentsMapper.environmentObject.has(entity) && !getSystemsCommonData().getDrawFlags().isDrawEnv()
				|| getSystemsCommonData().getCursor() == entity && !getSystemsCommonData().getDrawFlags().isDrawCursor()
				|| considerFow && isInFow(entity, miComp.getModelInstance().transform.getTranslation(auxVector3_1));
	}

	private boolean isInFow(Entity modelEntity, Vector3 position) {
		if (ComponentsMapper.floor.has(modelEntity)
				|| ComponentsMapper.wall.has(modelEntity)
				|| modelEntity == getSystemsCommonData().getCursor()) return false;

		MapGraph map = getSystemsCommonData().getMap();
		MapGraphNode node = map.getNode(position);
		if (node == null) return false;
		Entity nodeEntity = node.getEntity();
		if (nodeEntity == null) return true;
		FloorComponent floorComponent = ComponentsMapper.floor.get(nodeEntity);
		return (floorComponent.getFogOfWarSignature() & 16) == 16 && !floorComponent.isDiscovered();
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		updateLights((PooledEngine) getEngine());
		render();
	}

	private void renderShadows( ) {
		if (!DebugSettings.ALLOW_STATIC_SHADOWS) return;
		GameFrameBuffer shadowFrameBuffer = staticShadowsData.getShadowFrameBuffer();
		shadowFrameBuffer.begin();
		resetDisplay(0F);
		Camera cam = getSystemsCommonData().getCamera();
		renderModels(renderBatches.getModelBatchShadows(), families.getModelEntitiesWithShadows(), false, cam, true);
		if (DebugSettings.ALLOW_SCREEN_SHOT_OF_DEPTH_MAP) {
			staticShadowsData.handleScreenshot(shadowFrameBuffer);
		}
		shadowFrameBuffer.end();
	}

	private void render( ) {
		getSystemsCommonData().setNumberOfVisible(0);
		renderShadows();
		resetDisplay();
		renderModels(renderBatches.getModelBatch(), families.getModelEntities(), true, getSystemsCommonData().getCamera());
		renderDecals();
		renderParticleEffects();
		getSystemsCommonData().getUiStage().draw();
	}

	private void renderParticleEffects( ) {
		renderBatches.getModelBatch().begin(getSystemsCommonData().getCamera());
		renderBatches.getModelBatch().render(getSystemsCommonData().getParticleSystem(), environment);
		renderBatches.getModelBatch().end();
	}

	private void renderDecals( ) {
		Gdx.gl.glDepthMask(false);
		DecalBatch decalBatch = renderBatches.getDecalBatch();
		decalBatch.setGroupStrategy(strategies.getRegularDecalGroupStrategy());
		renderSimpleDecals();
		renderLiveCharacters(environment.getAmbientColor());
		decalBatch.flush();
		renderCharactersOutline();
		Gdx.gl.glDepthMask(true);
	}

	private void renderCharactersOutline( ) {
		DecalBatch decalBatch = renderBatches.getDecalBatch();
		decalBatch.setGroupStrategy(strategies.getOutlineDecalGroupStrategy());
		renderLiveCharacters(null, OUTLINE_ALPHA, false);
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glDepthFunc(GL20.GL_GREATER);
		decalBatch.flush();
		Gdx.gl.glDepthFunc(GL20.GL_LESS);
		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
	}

	private void renderSimpleDecals( ) {
		for (Entity entity : families.getSimpleDecalsEntities()) {
			renderSimpleDecal(renderBatches.getDecalBatch(), entity);
		}
	}

	private void handleSimpleDecalAnimation(final Entity entity, final SimpleDecalComponent simpleDecalComponent) {
		if (ComponentsMapper.animation.has(entity) && simpleDecalComponent.isAnimatedByAnimationComponent()) {
			AnimationComponent animationComponent = ComponentsMapper.animation.get(entity);
			simpleDecalComponent.getDecal().setTextureRegion(animationComponent.calculateFrame());
		}
	}

	private void faceDecalToCamera(final SimpleDecalComponent simpleDecal, final Decal decal) {
		if (simpleDecal.isBillboard()) {
			Camera camera = getSystemsCommonData().getCamera();
			decal.lookAt(auxVector3_1.set(decal.getPosition()).sub(camera.direction), camera.up);
		}
	}

	private void renderSimpleDecal(final DecalBatch decalBatch, final Entity entity) {
		SimpleDecalComponent component = ComponentsMapper.simpleDecal.get(entity);
		if (component != null && component.isVisible() && !isInFow(entity, component.getDecal().getPosition())) {
			handleSimpleDecalAnimation(entity, component);
			faceDecalToCamera(component, component.getDecal());
			decalBatch.add(component.getDecal());
			renderRelatedDecals(decalBatch, component);
		}
	}

	private void renderRelatedDecals(final DecalBatch decalBatch, final SimpleDecalComponent hudDecal) {
		List<RelatedDecal> relatedDecals = hudDecal.getRelatedDecals();
		if (!relatedDecals.isEmpty()) {
			for (RelatedDecal relatedDecal : relatedDecals) {
				if (relatedDecal.isVisible()) {
					faceDecalToCamera(hudDecal, relatedDecal);
					decalBatch.add(relatedDecal);
				}
			}
		}
	}

	private void renderLiveCharacters(Color color) {
		renderLiveCharacters(color, 1F, true);
	}

	private void renderLiveCharacters(Color color, float alpha, boolean updateCharacterDecal) {
		for (Entity entity : families.getCharacterDecalsEntities()) {
			Vector3 position = ComponentsMapper.characterDecal.get(entity).getDecal().getPosition();
			Entity floorEntity = getSystemsCommonData().getMap().getNode(position).getEntity();
			if (updateCharacterDecal) {
				updateCharacterDecal(entity);
			}
			if ((DebugSettings.DISABLE_FOW || isNodeRevealed(floorEntity)) && (shouldRenderPlayer(entity) || shouldRenderEnemy(entity))) {
				renderCharacterDecal(entity, color, alpha);
			}
		}
	}

	private boolean isNodeRevealed(Entity floorEntity) {
		if (floorEntity == null) return false;
		ModelInstanceComponent modelInstanceComponent = ComponentsMapper.modelInstance.get(floorEntity);
		return modelInstanceComponent.getFlatColor() == null && (modelInstanceComponent.getGraySignature() & 16) == 0;
	}

	private boolean shouldRenderEnemy(Entity entity) {
		return ComponentsMapper.enemy.has(entity) && getSystemsCommonData().getDrawFlags().isDrawEnemy();
	}

	private boolean shouldRenderPlayer(Entity entity) {
		return ComponentsMapper.player.has(entity) && !ComponentsMapper.player.get(entity).isDisabled();
	}

	private boolean shouldApplyLightsOnCharacterDecal(final Entity entity,
													  final CharacterSpriteData spriteData) {
		Decal decal = ComponentsMapper.characterDecal.get(entity).getDecal();
		TextureAtlas.AtlasRegion textureRegion = (TextureAtlas.AtlasRegion) decal.getTextureRegion();
		boolean notInHitFrameIndex = textureRegion.index != spriteData.getPrimaryAttackHitFrameIndex();
		boolean noPrimaryAttack = spriteData.getSpriteType() != ATTACK_PRIMARY || notInHitFrameIndex;
		boolean meleeWeapon = getSystemsCommonData().getStorage().getSelectedWeapon().isMelee();
		return noPrimaryAttack || meleeWeapon;
	}

	void setDecalColorAccordingToLights(final Entity entity) {
		Decal decal = ComponentsMapper.characterDecal.get(entity).getDecal();
		if (shouldApplyLightsOnCharacterDecal(entity, ComponentsMapper.character.get(entity).getCharacterSpriteData())) {
			findClosestLight(decal);
			float ambient = getSystemsCommonData().getMap().getAmbient();
			Color color = decal.getColor().add(auxColor.set(ambient, ambient, ambient, ambient));
			decal.setColor(color);
		} else {
			decal.setColor(Color.WHITE);
		}
	}

	private void findClosestLight(Decal decal) {
		float minDistance = Float.MAX_VALUE;
		minDistance = applyLightsOnDecal(decal, minDistance);
		if (minDistance == Float.MAX_VALUE) {
			decal.setColor(DECAL_DARKEST_COLOR, DECAL_DARKEST_COLOR, DECAL_DARKEST_COLOR, 1f);
		}
	}

	private float convertDistanceToColorValueForDecal(final float maxLightDistanceForDecal, final float distance) {
		return MathUtils.map(
				0,
				(maxLightDistanceForDecal - DECAL_LIGHT_OFFSET),
				DECAL_DARKEST_COLOR,
				1f,
				maxLightDistanceForDecal - distance);
	}

	private float calculateDecalColorAffectedByLight(final Decal d,
													 float minDistance,
													 final float distance,
													 final float maxLightDistanceForDecal) {
		float newC = convertDistanceToColorValueForDecal(maxLightDistanceForDecal, distance);
		Color c = d.getColor();
		if (minDistance == Float.MAX_VALUE) {
			d.setColor(min(newC, 1f), min(newC, 1f), min(newC, 1f), 1f);
		} else {
			d.setColor(min(max(c.r, newC), 1f), min(max(c.g, newC), 1f), min(max(c.b, newC), 1f), 1f);
		}
		minDistance = min(minDistance, distance);
		return minDistance;
	}

	private void createShadowMaps( ) {
		PerspectiveCamera cameraLight = new PerspectiveCamera(90f, DEPTH_MAP_SIZE, DEPTH_MAP_SIZE);
		cameraLight.near = 0.01F;
		cameraLight.far = CAMERA_LIGHT_FAR;
		for (Entity light : families.getStaticLightsEntities()) {
			createShadowMapForLight(light, cameraLight);
		}
	}

	private void createShadowMapForLight(final Entity light,
										 final PerspectiveCamera cameraLight) {
		GameFrameBufferCubeMap frameBuffer = new GameFrameBufferCubeMap(
				Format.RGBA8888,
				DEPTH_MAP_SIZE,
				DEPTH_MAP_SIZE,
				true);

		cameraLight.direction.set(0, 0, -1);
		cameraLight.up.set(0, 1, 0);
		cameraLight.position.set(ComponentsMapper.staticLight.get(light).getPosition(auxVector3_1));
		cameraLight.rotate(Vector3.Y, 0);
		cameraLight.update();
		StaticLightComponent lightComponent = ComponentsMapper.staticLight.get(light);
		resetDisplay();
		ShaderProgram depthShaderProgram = staticShadowsData.getDepthShaderProgram();
		depthShaderProgram.bind();
		depthShaderProgram.setUniformf("u_cameraFar", cameraLight.far);
		depthShaderProgram.setUniformf("u_lightPosition", cameraLight.position);
		for (int s = 0; s <= 5; s++) {
			Cubemap.CubemapSide side = Cubemap.CubemapSide.values()[s];
			frameBuffer.begin();
			frameBuffer.bindSide(side, cameraLight);
			int sam = Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0;
			Gdx.gl.glClearColor(0F, 0F, 0F, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | sam);
			renderModels(
					renderBatches.getDepthModelBatch(),
					families.getModelEntitiesWithShadows(),
					false,
					cameraLight,
					false);
		}
		frameBuffer.end();
		lightComponent.setShadowFrameBuffer(frameBuffer);
	}

	private float applyLightOnDecal(final Decal decal, float minDistance, LightComponent lightComponent) {
		float distance = lightComponent.getPosition(auxVector3_1).dst(decal.getPosition());
		float maxLightDistanceForDecal = lightComponent.getRadius();
		if (distance <= maxLightDistanceForDecal) {
			minDistance = calculateDecalColorAffectedByLight(decal, minDistance, distance, maxLightDistanceForDecal);
		}
		return minDistance;
	}

	private float applyLightsOnDecal(final Decal decal, float minDistance) {
		for (Entity light : families.getShadowlessLightsEntities()) {
			minDistance = applyLightOnDecal(decal, minDistance, ComponentsMapper.shadowlessLight.get(light));
		}
		for (Entity light : families.getStaticLightsEntities()) {
			minDistance = applyLightOnDecal(decal, minDistance, ComponentsMapper.staticLight.get(light));
		}
		return minDistance;
	}

	private void renderCharacterDecal(Entity entity, Color color, float alpha) {
		if (DebugSettings.HIDE_CHARACTERS) return;

		Decal decal = ComponentsMapper.characterDecal.get(entity).getDecal();
		Vector3 decalPosition = decal.getPosition();
		Camera camera = getSystemsCommonData().getCamera();
		if (color == null) {
			color = ComponentsMapper.player.has(entity) ? PLAYER_OUTLINE_COLOR : ENEMY_OUTLINE_COLOR;
			decal.setColor(color.r, color.g, color.b, alpha);
		} else {
			decal.setColor(color.r, color.g, color.b, alpha);
			setDecalColorAccordingToLights(entity);
		}
		decal.lookAt(auxVector3_1.set(decalPosition).sub(camera.direction), camera.up);
		renderBatches.getDecalBatch().add(decal);
	}

	private void updateLights(final PooledEngine engine) {
		for (Entity light : families.getShadowlessLightsEntities()) {
			updateLight(light);
		}
		if (!auxLightsListToRemove.isEmpty()) {
			for (Entity light : auxLightsListToRemove) {
				engine.removeEntity(light);
			}
			auxLightsListToRemove.clear();
		}
	}

	private void updateFlicker(final ShadowlessLightComponent lc, final long now) {
		if (lc.isFlicker() && now >= lc.getNextFlicker()) {
			ShadowlessLightOriginalData originalData = lc.getShadowlessLightOriginalData();
			lc.setIntensity(MathUtils.random(FLICKER_RANDOM_MIN, FLICKER_RANDOM_MAX) * originalData.getOriginalIntensity());
			lc.setRadius(MathUtils.random(FLICKER_RANDOM_MIN, FLICKER_RANDOM_MAX) * originalData.getOriginalRadius());
			lc.setNextFlicker(now + MathUtils.random(FLICKER_MAX_INTERVAL));
		}
	}

	private void updateLight(final Entity light) {
		ShadowlessLightComponent lc = ComponentsMapper.shadowlessLight.get(light);
		long now = TimeUtils.millis();
		updateFlicker(lc, now);
		if (ComponentsMapper.modelInstance.has(light)) {
			GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(lc.getParent()).getModelInstance();
			lc.setPosition(modelInstance.transform.getTranslation(auxVector3_1));
		}
		float duration = lc.getDuration();
		if (duration > 0 && TimeUtils.timeSinceMillis(lc.getBeginTime()) >= (duration * 1000F)) {
			auxLightsListToRemove.add(light);
		}
	}

	private void updateCharacterDecal(Entity entity) {
		Camera camera = getSystemsCommonData().getCamera();
		CharacterComponent characterComp = ComponentsMapper.character.get(entity);
		CharacterSpriteData charSpriteData = characterComp.getCharacterSpriteData();
		Direction direction = CharacterUtils.calculateDirectionSeenFromCamera(camera, characterComp.getFacingDirection());
		SpriteType spriteType = charSpriteData.getSpriteType();
		boolean sameSpriteType = spriteType.equals(ComponentsMapper.characterDecal.get(entity).getSpriteType());
		if ((!sameSpriteType || !ComponentsMapper.characterDecal.get(entity).getDirection().equals(direction))) {
			updateCharacterDecalSprite(entity, direction, spriteType, sameSpriteType);
		} else if (spriteType != RUN || getSystemsCommonData().getTurnsQueue().first() == entity) {
			updateCharacterDecalFrame(entity, characterComp, spriteType);
		}
	}

	private void updateCharacterDecalFrame(Entity entity,
										   CharacterComponent characterComponent,
										   SpriteType spriteType) {
		CharacterDecalComponent characterDecalComponent = ComponentsMapper.characterDecal.get(entity);
		AnimationComponent aniComp = ComponentsMapper.animation.get(entity);
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		Animation<AtlasRegion> anim = aniComp.getAnimation();
		if (!systemsCommonData.getMenuTable().isVisible() && ComponentsMapper.animation.has(entity) && anim != null) {
			if (spriteType == IDLE && anim.isAnimationFinished(aniComp.getStateTime())) {
				if (anim.getPlayMode() == Animation.PlayMode.NORMAL) {
					anim.setPlayMode(Animation.PlayMode.REVERSED);
				} else {
					anim.setPlayMode(Animation.PlayMode.NORMAL);
					Direction direction = CharacterUtils.calculateDirectionSeenFromCamera(
							systemsCommonData.getCamera(),
							characterComponent.getFacingDirection());
					CharacterAnimation animation = fetchCharacterAnimationByDirectionAndType(
							entity,
							direction,
							spriteType);
					aniComp.init(0, animation);
				}
				aniComp.resetStateTime();
			}
			AtlasRegion currentFrame = (AtlasRegion) characterDecalComponent.getDecal().getTextureRegion();
			AtlasRegion newFrame = calculateCharacterDecalNewFrame(entity, aniComp, currentFrame);
			if (characterDecalComponent.getSpriteType() == spriteType && currentFrame != newFrame) {
				Decal decal = characterDecalComponent.getDecal();
				decal.setTextureRegion(newFrame);
			}
		}
	}

	private AtlasRegion calculateCharacterDecalNewFrame(Entity entity,
														AnimationComponent animationComponent,
														AtlasRegion currentFrame) {
		AtlasRegion newFrame = animationComponent.calculateFrame();
		if (currentFrame.index != newFrame.index) {
			for (RenderSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onFrameChanged(entity, newFrame);
			}
		}
		return newFrame;
	}

	private void updateCharacterDecalSprite(Entity entity,
											Direction direction,
											SpriteType spriteType,
											boolean sameSpriteType) {
		CharacterDecalComponent characterDecalComponent = ComponentsMapper.characterDecal.get(entity);
		SpriteType prevSprite = characterDecalComponent.getSpriteType();
		characterDecalComponent.initializeSprite(spriteType, direction);
		if (ComponentsMapper.animation.has(entity)) {
			initializeCharacterAnimationBySpriteType(entity, direction, spriteType, sameSpriteType);
		}
		if (prevSprite != characterDecalComponent.getSpriteType()) {
			subscribers.forEach(sub -> sub.onSpriteTypeChanged(entity, characterDecalComponent.getSpriteType()));
		}
	}

	private void initializeCharacterAnimationBySpriteType(Entity entity,
														  Direction direction,
														  SpriteType spriteType,
														  boolean sameSpriteType) {
		AnimationComponent animationComponent = ComponentsMapper.animation.get(entity);
		direction = forceDirectionForAnimationInitialization(direction, spriteType, animationComponent);
		Animation<AtlasRegion> oldAnimation = ComponentsMapper.animation.get(entity).getAnimation();
		CharacterAnimation newAnimation = fetchCharacterAnimationByDirectionAndType(entity, direction, spriteType);
		if (newAnimation != null) {
			boolean isIdle = spriteType == IDLE;
			animationComponent.init(isIdle ? 0 : spriteType.getFrameDuration(), newAnimation);
			boolean differentAnimation = oldAnimation != newAnimation;
			if (!sameSpriteType || isIdle) {
				if (spriteType.getPlayMode() != Animation.PlayMode.LOOP) {
					newAnimation.setPlayMode(Animation.PlayMode.NORMAL);
				}
				animationComponent.resetStateTime();
			} else if (differentAnimation) {
				newAnimation.setPlayMode(oldAnimation.getPlayMode());
			}
			if (differentAnimation) {
				subscribers.forEach(sub -> sub.onAnimationChanged(entity));
			}
		}
	}

	private static Direction forceDirectionForAnimationInitialization(Direction direction, SpriteType spriteType, AnimationComponent animationComponent) {
		if (spriteType.isSingleDirection()) {
			if (!animationComponent.getAnimation().isAnimationFinished(animationComponent.getStateTime())) {
				direction = Direction.SOUTH;
			}
		}
		return direction;
	}

	@Override
	public void dispose( ) {
		staticShadowsData.dispose();
		renderBatches.dispose();
	}
}
