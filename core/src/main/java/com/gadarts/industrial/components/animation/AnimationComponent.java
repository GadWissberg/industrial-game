package com.gadarts.industrial.components.animation;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.TimeUtils;
import com.gadarts.industrial.components.GameComponent;
import lombok.Getter;
import lombok.Setter;

@Getter
public class AnimationComponent implements GameComponent {

	private Animation<TextureAtlas.AtlasRegion> animation;

	@Setter
	private float stateTime;
	private long lastFrameChange;

	@Override
	public void reset( ) {
		animation.setPlayMode(Animation.PlayMode.NORMAL);
		stateTime = 0;
	}

	public void init(final float frameDuration, final Animation<TextureAtlas.AtlasRegion> animation) {
		this.animation = animation;
		animation.setFrameDuration(frameDuration);
	}

	public TextureAtlas.AtlasRegion calculateFrame( ) {
		double frameDuration = animation.getFrameDuration();
		boolean looping = animation.getPlayMode() == Animation.PlayMode.LOOP || animation.getPlayMode() == Animation.PlayMode.LOOP_PINGPONG;
		TextureAtlas.AtlasRegion result = animation.getKeyFrame(stateTime, looping);
		long now = TimeUtils.millis();
		if (now - lastFrameChange >= frameDuration * 1000.0) {
			lastFrameChange = now;
			stateTime += (float) frameDuration;
		}
		return result;
	}

	public void resetStateTime( ) {
		stateTime = 0;
		lastFrameChange = TimeUtils.millis();
	}
}
