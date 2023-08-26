package com.gadarts.industrial.systems.render.shaders.shadow;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.StaticLightComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.systems.render.RenderSystem;

import static com.gadarts.industrial.systems.SystemsCommonData.CAMERA_LIGHT_FAR;

public class ShadowMapShader extends BaseShader {
	public static final float BIAS_MIN_WALL = 0.0021F;
	public static final float BIAS_MAX_WALL = 0.0022F;
	public static final float BIAS_MAX_GENERAL = 0.0018F;
	public static final float BIAS_MIN_GENERAL = 0F;
	private static final Vector3 auxVector1 = new Vector3();
	private static final Vector3 auxVector2 = new Vector3();
	private static final int CUBE_MAP_TEXTURE_NUMBER = 8;
	private static final float BIAS_MAX_FLOOR = 0.00185F;
	private static final Color auxColor = new Color();
	private final ImmutableArray<Entity> lights;
	private final float[] lightColor = new float[3];
	private final ShadowMapShaderUniforms uniforms = new ShadowMapShaderUniforms();
	public Renderable renderable;

	public ShadowMapShader(final Renderable renderable,
						   final ShaderProgram shaderProgramModelBorder,
						   final ImmutableArray<Entity> staticLights) {
		this.lights = staticLights;
		this.renderable = renderable;
		this.program = shaderProgramModelBorder;
		register(DefaultShader.Inputs.worldTrans, DefaultShader.Setters.worldTrans);
		register(DefaultShader.Inputs.projViewTrans, DefaultShader.Setters.projViewTrans);
		register(DefaultShader.Inputs.normalMatrix, DefaultShader.Setters.normalMatrix);
		uniforms.fetchUniformsLocations(program);
	}

	private void fillStaticLightColorArray(Color color) {
		lightColor[0] = color.r;
		lightColor[1] = color.g;
		lightColor[2] = color.b;
	}


	@Override
	public void begin(final Camera camera, final RenderContext context) {
		super.begin(camera, context);
		context.setDepthTest(GL20.GL_LEQUAL);
		context.setCullFace(GL20.GL_BACK);
	}

	@Override
	public void init( ) {
		final ShaderProgram program = this.program;
		this.program = null;
		init(program, renderable);
		renderable = null;
	}

	@Override
	public int compareTo(final Shader other) {
		return 0;
	}

	@Override
	public boolean canRender(final Renderable instance) {
		return true;
	}

	@Override
	public void render(final Renderable renderable, final Attributes combinedAttributes) {
		boolean firstCall = true;
		Entity entity = (Entity) renderable.userData;
		for (int i = 0; i < lights.size(); i++) {
			GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(entity).getModelInstance();
			Vector3 position = modelInstance.transform.getTranslation(auxVector1);
			StaticLightComponent lightComponent = ComponentsMapper.staticLight.get(lights.get(i));
			if (position.dst2(lightComponent.getPosition(auxVector2)) <= lightComponent.getRadius() * 5) {
				renderLightForShadow(renderable, combinedAttributes, firstCall, i);
				firstCall = false;
			}
		}
	}

	private void renderLightForShadow(Renderable renderable,
									  Attributes combinedAttributes,
									  boolean firstCall,
									  int lightIndex) {
		initializeLightForRendering(lightIndex);
		setBias((Entity) renderable.userData);
		if (firstCall) {
			context.setDepthTest(GL20.GL_LEQUAL);
			context.setBlending(false, GL20.GL_ONE, GL20.GL_ONE);
		} else {
			context.setBlending(true, GL20.GL_ONE, GL20.GL_ONE);
		}
		super.render(renderable, combinedAttributes);
	}

	private void setBias(Entity entity) {
		float maxBias = BIAS_MAX_GENERAL;
		float minBias = BIAS_MIN_GENERAL;
		if (ComponentsMapper.wall.has(entity)) {
			minBias = BIAS_MIN_WALL;
			maxBias = BIAS_MAX_WALL;
		} else if (ComponentsMapper.floor.has(entity)) {
			maxBias = BIAS_MAX_FLOOR;
		}
		program.setUniformf(uniforms.getUniformLocMinBias(), minBias);
		program.setUniformf(uniforms.getUniformLocMaxBias(), maxBias);
	}

	private void initializeLightForRendering(int lightIndex) {
		StaticLightComponent lightComponent = ComponentsMapper.staticLight.get(lights.get(lightIndex));
		fillStaticLightColorArray(lightComponent.getColor(auxColor));
		lightComponent.getShadowFrameBuffer().getColorBufferTexture().bind(CUBE_MAP_TEXTURE_NUMBER);
		setUniforms(lightComponent);
	}

	private void setUniforms(StaticLightComponent lightComponent) {
		program.setUniformi(uniforms.getUniformLocDepthMapCube(), CUBE_MAP_TEXTURE_NUMBER);
		program.setUniformf(uniforms.getUniformLocCameraFar(), CAMERA_LIGHT_FAR);
		program.setUniformf(uniforms.getUniformLocLightPosition(), lightComponent.getPosition(auxVector1));
		program.setUniformf(uniforms.getUniformLocRadius(), lightComponent.getRadius());
		program.setUniform3fv(uniforms.getUniformLocLightColor(), lightColor, 0, 3);
		program.setUniformf(uniforms.getUniformLocIntensity(), lightComponent.getIntensity());
		program.setUniformf(uniforms.getUniformLocDepthMapSize(), RenderSystem.DEPTH_MAP_SIZE);
	}

}
