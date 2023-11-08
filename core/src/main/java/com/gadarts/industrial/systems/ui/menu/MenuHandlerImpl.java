package com.gadarts.industrial.systems.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetManager;

import java.util.Arrays;


public class MenuHandlerImpl implements MenuHandler, Disposable {

	private final GameAssetManager assetsManager;
	private final Table menuTable;
	private final SoundPlayer soundPlayer;
	private final Stage stage;
	private final ShaderProgram shaderProgram;
	private final FrameBuffer frameBuffer;
	private final Sprite sprite;

	public MenuHandlerImpl(GameAssetManager assetsManager, SoundPlayer soundPlayer) {
		this.assetsManager = assetsManager;
		this.soundPlayer = soundPlayer;
		stage = new Stage();
		Image backgroundImage = new Image(assetsManager.getTexture(Assets.UiTextures.MENU_BACKGROUND));
		stage.addActor(backgroundImage);
		stage.setDebugAll(DebugSettings.DISPLAY_USER_INTERFACE_OUTLINES);
		Image logo = new Image(assetsManager.getTexture(Assets.UiTextures.LOGO));
		menuTable = new Table();
		menuTable.add(logo).row();
		menuTable.setSize(stage.getWidth(), stage.getHeight());
		applyMenuOptions(MainMenuOptions.values(), assetsManager);
		stage.addActor(menuTable);
		shaderProgram = new ShaderProgram(
				assetsManager.getShader(Assets.Shaders.BASIC_VERTEX),
				assetsManager.getShader(Assets.Shaders.MENU_CRT_FRAGMENT));
		stage.getBatch().setShader(shaderProgram);
		int fboWidth = Gdx.graphics.getWidth();
		int fboHeight = Gdx.graphics.getHeight();
		frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, fboWidth, fboHeight, false);
		sprite = new Sprite(frameBuffer.getColorBufferTexture());
		sprite.flip(false, true);
	}

	@Override
	public void applyMenuOptions(MenuOptionDefinition[] options) {
		menuTable.clear();
		BitmapFont smallFont = assetsManager.getFont(Assets.Fonts.ARIAL_MT_BOLD_SMALL);
		Label.LabelStyle style = new Label.LabelStyle(smallFont, MenuOption.FONT_COLOR_REGULAR);
		Arrays.stream(options).forEach(o -> menuTable.add(new MenuOption(o, style, soundPlayer, this)).row());
	}

	@Override
	public void render(float delta) {
		frameBuffer.begin();
		int fboWidth = Gdx.graphics.getWidth();
		int fboHeight = Gdx.graphics.getHeight();
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		Batch batch = stage.getBatch();
		batch.setShader(null);
		stage.act(delta);
		stage.draw();
		frameBuffer.end();
		frameBuffer.getColorBufferTexture().bind(0);
		shaderProgram.bind();
		shaderProgram.setUniformi("u_texture", 0);
		shaderProgram.setUniformi("u_time", (int) TimeUtils.nanoTime());
		batch.setShader(shaderProgram);
		batch.begin();
		sprite.setSize(fboWidth, fboHeight);
		sprite.draw(batch);
		batch.end();
		batch.setShader(null);
	}

	private void applyMenuOptions(MenuOptionDefinition[] options,
								  GameAssetManager assetsManager) {
		BitmapFont smallFont = assetsManager.getFont(Assets.Fonts.ARIAL_MT_BOLD_SMALL);
		Label.LabelStyle style = new Label.LabelStyle(smallFont, MenuOption.FONT_COLOR_REGULAR);
		Arrays.stream(options).forEach(o -> menuTable.add(new MenuOption(o, style, soundPlayer, this)).row());
	}

	@Override
	public void dispose( ) {
		stage.dispose();
		shaderProgram.dispose();
		frameBuffer.dispose();
	}
}
