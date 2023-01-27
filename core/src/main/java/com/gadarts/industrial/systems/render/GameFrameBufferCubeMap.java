package com.gadarts.industrial.systems.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBufferCubemap;

public class GameFrameBufferCubeMap extends FrameBufferCubemap {
	public GameFrameBufferCubeMap(final Pixmap.Format format,
								  final int width,
								  final int height,
								  final boolean hasDepth) {
		super(format, width, height, hasDepth);
	}

	public void bindSide(final Cubemap.CubemapSide side) {
		super.bindSide(side);
	}

	public void bindSide(final Cubemap.CubemapSide side, final Camera camera) {
		switch (side) {
			case NegativeX -> {
				camera.up.set(0, -1, 0);
				camera.direction.set(-1, 0, 0);
			}
			case NegativeY -> {
				camera.up.set(0, 0, -1);
				camera.direction.set(0, -1, 0);
			}
			case NegativeZ -> {
				camera.up.set(0, -1, 0);
				camera.direction.set(0, 0, -1);
			}
			case PositiveX -> {
				camera.up.set(0, -1, 0);
				camera.direction.set(1, 0, 0);
			}
			case PositiveY -> {
				camera.up.set(0, 0, 1);
				camera.direction.set(0, 1, 0);
			}
			case PositiveZ -> {
				camera.up.set(0, -1, 0);
				camera.direction.set(0, 0, 1);
			}
			default -> {
			}
		}
		camera.update();
		bindSide(side);
	}
}
