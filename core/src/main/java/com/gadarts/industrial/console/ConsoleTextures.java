package com.gadarts.industrial.console;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;

public class ConsoleTextures implements Disposable {
	public static final Color ARROW_COLOR = Color.CHARTREUSE;
	public static final int CURSOR_WIDTH = 10;
	public static final int CURSOR_HEIGHT = 10;
	private static final Color CONSOLE_BACKGROUND_COLOR = new Color(0, 0.1f, 0, 1f);
	private static final Color TEXT_BACKGROUND_COLOR = new Color(0, 0.2f, 0, 0.8f);

	private Texture backgroundTexture;
	private Texture textBackgroundTexture;
	private Texture cursorTexture;
	private Texture arrowTexture;

	private void generateTextBackgroundTexture() {
		Pixmap textBackground = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		textBackground.setColor(TEXT_BACKGROUND_COLOR);
		textBackground.fill();
		textBackgroundTexture = new Texture(textBackground);
		textBackground.dispose();
	}

	private void generateCursorTexture() {
		Pixmap cursorPixmap = new Pixmap(CURSOR_WIDTH, CURSOR_HEIGHT, Pixmap.Format.RGBA8888);
		cursorPixmap.setColor(Color.YELLOW);
		cursorPixmap.fill();
		cursorTexture = new Texture(cursorPixmap);
		cursorPixmap.dispose();
	}

	private void generateArrowTexture() {
		Pixmap arrowUpPixmap = new Pixmap(CURSOR_WIDTH, CURSOR_HEIGHT, Pixmap.Format.RGBA8888);
		arrowUpPixmap.setColor(ARROW_COLOR);
		arrowUpPixmap.fillTriangle(CURSOR_WIDTH, CURSOR_HEIGHT, CURSOR_WIDTH / 2, 0, 0, CURSOR_HEIGHT);
		arrowTexture = new Texture(arrowUpPixmap);
		arrowUpPixmap.dispose();
	}

	public Texture getBackgroundTexture() {
		return backgroundTexture;
	}

	private void generateBackgroundTexture(final int height) {
		Pixmap pixmap = new Pixmap(Gdx.graphics.getWidth(), height, Pixmap.Format.RGBA8888);
		pixmap.setColor(CONSOLE_BACKGROUND_COLOR);
		pixmap.fillRectangle(0, 0, Gdx.graphics.getWidth(), height);
		backgroundTexture = new Texture(pixmap);
		pixmap.dispose();
	}

	public Texture getTextBackgroundTexture() {
		return textBackgroundTexture;
	}

	public Texture getCursorTexture() {
		return cursorTexture;
	}

	public void init(final int height) {
		generateBackgroundTexture(height);
		generateTextBackgroundTexture();
		generateCursorTexture();
		generateArrowTexture();
	}

	@Override
	public void dispose() {
		textBackgroundTexture.dispose();
		backgroundTexture.dispose();
		cursorTexture.dispose();
		arrowTexture.dispose();
	}

	public Texture getArrowTexture() {
		return arrowTexture;
	}
}
