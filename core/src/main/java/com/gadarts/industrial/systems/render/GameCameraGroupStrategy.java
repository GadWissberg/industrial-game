package com.gadarts.industrial.systems.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalMaterial;
import com.badlogic.gdx.graphics.g3d.decals.GroupStrategy;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;

import java.util.Comparator;

public class GameCameraGroupStrategy implements GroupStrategy, Disposable {
	public static final String UNIFORM_COLOR_NOT_AFFECTED_BY_LIGHT = "u_colorNotAffectedByLight";
	public static final String MSG_FAILED_TO_COMPILE = "couldn't compile shader: %s";
	private static final int GROUP_OPAQUE = 0;
	private static final int GROUP_BLEND = 1;
	private static final Color colorNotAffectedByLight = Color.valueOf("E3E700");
	private static final float[] colorNotAffectedByLightArray = new float[]{
			colorNotAffectedByLight.r,
			colorNotAffectedByLight.g,
			colorNotAffectedByLight.b
	};

	private final Comparator<Decal> cameraSorter;
	private final Pool<Array<Decal>> arrayPool = new Pool<>(16) {
		@Override
		protected Array<Decal> newObject( ) {
			return new Array<>();
		}
	};
	private final Array<Array<Decal>> usedArrays = new Array<>();
	private final ObjectMap<DecalMaterial, Array<Decal>> materialGroups = new ObjectMap<>();
	ShaderProgram shader;
	Camera camera;

	public GameCameraGroupStrategy(final Camera camera, final GameAssetsManager assetsManager) {
		this(camera, assetsManager, (o1, o2) -> {
			float dist1 = camera.position.dst(o1.getPosition());
			float dist2 = camera.position.dst(o2.getPosition());
			return (int) Math.signum(dist2 - dist1);
		});
	}

	public GameCameraGroupStrategy(final Camera camera,
								   final GameAssetsManager assetsManager,
								   final Comparator<Decal> sorter) {
		this.camera = camera;
		this.cameraSorter = sorter;
		createDefaultShader(assetsManager);
	}

	@Override
	public int decideGroup(final Decal decal) {
		return decal.getMaterial().isOpaque() ? GROUP_OPAQUE : GROUP_BLEND;
	}

	@Override
	public void beforeGroup(final int group, final Array<Decal> contents) {
		if (group == GROUP_BLEND) {
			Gdx.gl.glEnable(GL20.GL_BLEND);
			contents.sort(cameraSorter);
		} else {

			for (int i = 0, n = contents.size; i < n; i++) {
				Decal decal = contents.get(i);
				Array<Decal> materialGroup = materialGroups.get(decal.getMaterial());
				if (materialGroup == null) {
					materialGroup = arrayPool.obtain();
					materialGroup.clear();
					usedArrays.add(materialGroup);
					materialGroups.put(decal.getMaterial(), materialGroup);
				}
				materialGroup.add(decal);
			}

			contents.clear();
			for (Array<Decal> materialGroup : materialGroups.values()) {
				contents.addAll(materialGroup);
			}

			materialGroups.clear();
			arrayPool.freeAll(usedArrays);
			usedArrays.clear();
		}
	}

	@Override
	public void afterGroup(final int group) {
		if (group == GROUP_BLEND) {
			Gdx.gl.glDisable(GL20.GL_BLEND);
		}
	}

	@Override
	public void beforeGroups( ) {
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
		shader.bind();
		shader.setUniformMatrix("u_projectionViewMatrix", camera.combined);
		shader.setUniformi("u_texture", 0);
		shader.setUniform3fv(
				UNIFORM_COLOR_NOT_AFFECTED_BY_LIGHT,
				colorNotAffectedByLightArray,
				0,
				colorNotAffectedByLightArray.length
		);
	}

	@Override
	public void afterGroups( ) {
		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
	}

	void createDefaultShader(final GameAssetsManager assetsManager) {
		String vertexShader = assetsManager.getShader(Assets.Shaders.DECAL_VERTEX);
		String fragmentShader = assetsManager.getShader(Assets.Shaders.DECAL_FRAGMENT);
		shader = new ShaderProgram(vertexShader, fragmentShader);
		if (!shader.isCompiled())
			throw new IllegalArgumentException(String.format(MSG_FAILED_TO_COMPILE, shader.getLog()));
	}

	@Override
	public ShaderProgram getGroupShader(final int group) {
		return shader;
	}

	@Override
	public void dispose( ) {
		if (shader != null) shader.dispose();
	}
}
