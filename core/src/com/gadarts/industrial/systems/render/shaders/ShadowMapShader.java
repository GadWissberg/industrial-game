package com.gadarts.industrial.systems.render.shaders;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
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

import static com.gadarts.industrial.systems.SystemsCommonData.CAMERA_LIGHT_FAR;

public class ShadowMapShader extends BaseShader {
	public static final String UNIFORM_TYPE = "u_type";
	public static final String UNIFORM_DEPTH_MAP_CUBE = "u_depthMapCube";
	public static final String UNIFORM_CAMERA_FAR = "u_cameraFar";
	public static final String UNIFORM_LIGHT_POSITION = "u_lightPosition";
	private static final Vector3 auxVector = new Vector3();
	private static final String UNIFORM_RADIUS = "u_radius";
	private static final int CUBE_MAP_TEXTURE_NUMBER = 8;
	private final ImmutableArray<Entity> lights;
	public Renderable renderable;

	public ShadowMapShader(final Renderable renderable,
						   final ShaderProgram shaderProgramModelBorder,
						   final ImmutableArray<Entity> lights) {
		this.lights = lights;
		this.renderable = renderable;
		this.program = shaderProgramModelBorder;
		register(DefaultShader.Inputs.worldTrans, DefaultShader.Setters.worldTrans);
		register(DefaultShader.Inputs.projViewTrans, DefaultShader.Setters.projViewTrans);
		register(DefaultShader.Inputs.normalMatrix, DefaultShader.Setters.normalMatrix);

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
		for (int i = 0; i < lights.size(); i++) {
			StaticLightComponent lightComponent = ComponentsMapper.staticLight.get(lights.get(i));
			lightComponent.getShadowFrameBuffer().getColorBufferTexture().bind(CUBE_MAP_TEXTURE_NUMBER);
			setUniforms(lightComponent);
			context.setBlending(true, GL20.GL_ONE, GL20.GL_ONE);
			super.render(renderable, combinedAttributes);
		}
	}

	private void setUniforms(StaticLightComponent lightComponent) {
		program.setUniformf(UNIFORM_TYPE, StaticLightTypes.POINT.ordinal());
		program.setUniformi(UNIFORM_DEPTH_MAP_CUBE, CUBE_MAP_TEXTURE_NUMBER);
		program.setUniformf(UNIFORM_CAMERA_FAR, CAMERA_LIGHT_FAR);
		program.setUniformf(UNIFORM_LIGHT_POSITION, lightComponent.getPosition(auxVector));
		program.setUniformf(UNIFORM_RADIUS, lightComponent.getRadius());
	}

	private enum StaticLightTypes {
		POINT
	}
}
