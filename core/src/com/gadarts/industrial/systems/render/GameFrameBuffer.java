package com.gadarts.industrial.systems.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

public class GameFrameBuffer extends FrameBuffer {
	public GameFrameBuffer(Pixmap.Format format, int width, int height, boolean hasDepth) {
		super(format, width, height, hasDepth);
	}

	@Override
	public void bind( ) {
		Gdx.gl30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferHandle);
	}

	@Override
	public void end(int x, int y, int width, int height) {
		Gdx.gl30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, defaultFramebufferHandle);
		Gdx.gl30.glViewport(x, y, width, height);
	}

	@Override
	protected void setFrameBufferViewport( ) {
		Gdx.gl30.glViewport(0, 0, getWidth(), getHeight());
	}

}
