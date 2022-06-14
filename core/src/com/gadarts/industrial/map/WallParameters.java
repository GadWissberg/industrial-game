package com.gadarts.industrial.map;

import com.gadarts.industrial.shared.assets.Assets;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class WallParameters {
	private final float vScale;
	private final float hOffset;
	private final float vOffset;
	private final Assets.SurfaceTextures definition;

	public WallParameters(Assets.SurfaceTextures definition) {
		this(0, 0, 0, definition);
	}
}
