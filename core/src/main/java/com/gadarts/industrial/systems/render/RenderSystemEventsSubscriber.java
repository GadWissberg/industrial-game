package com.gadarts.industrial.systems.render;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import com.gadarts.industrial.systems.SystemEventsSubscriber;

public interface RenderSystemEventsSubscriber extends SystemEventsSubscriber {
	default void onFrameChanged(Entity entity, TextureAtlas.AtlasRegion newFrame) {
	}

	default void onSpriteTypeChanged(Entity entity, SpriteType spriteType) {
	}

	default void onAnimationChanged(Entity entity) {
	}
}
