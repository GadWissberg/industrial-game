package com.gadarts.industrial.systems.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
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
	private final SoundPlayer soundPlayer;
	private final Stage stage;
	private final Texture cursorTexture;
	private Table menuTable;
	private ShaderProgram shaderProgram;
	private FrameBuffer frameBuffer;
	private Sprite crtEffect;
	private String currentMenu;

	public MenuHandlerImpl(GameAssetManager assetsManager, SoundPlayer soundPlayer) {
		this.assetsManager = assetsManager;
		this.soundPlayer = soundPlayer;
		stage = new Stage();
		stage.addActor(new Image(assetsManager.getTexture(Assets.UiTextures.MENU_BACKGROUND)));
		stage.setDebugAll(DebugSettings.DISPLAY_USER_INTERFACE_OUTLINES);
		createMenu();
		createCrtEffect();
		Gdx.input.setInputProcessor(stage);
		stage.addListener(new InputListener() {
			@Override
			public boolean keyDown(InputEvent event, int keycode) {
				if (keycode == Input.Keys.ESCAPE && !currentMenu.equals(MainMenuOptions.MAIN_MENU_NAME)) {
					createMenu();
				}
				return true;
			}
		});
		cursorTexture = assetsManager.getTexture(Assets.UiTextures.CURSOR);
	}

	private void createCrtEffect( ) {
		shaderProgram = new ShaderProgram(
				assetsManager.getShader(Assets.Shaders.BASIC_VERTEX),
				assetsManager.getShader(Assets.Shaders.MENU_CRT_FRAGMENT));
		stage.getBatch().setShader(shaderProgram);
		frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
		crtEffect = new Sprite(frameBuffer.getColorBufferTexture());
		crtEffect.flip(false, true);
	}

	private void createMenu( ) {
		if (menuTable != null) {
			menuTable.remove();
		}
		final Table menuTable;
		menuTable = new Table();
		menuTable.add(new Image(assetsManager.getTexture(Assets.UiTextures.LOGO))).row();
		menuTable.setSize(stage.getWidth(), stage.getHeight());
		this.menuTable = menuTable;
		applyMenuOptions(MainMenuOptions.values(), false);
		stage.addActor(menuTable);
	}

	@Override
	public void applyMenuOptions(MenuOptionDefinition[] options, boolean clearTableBefore) {
		if (clearTableBefore) {
			menuTable.clear();
		}
		currentMenu = options[0].getMenuName();
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
		batch.begin();
		batch.draw(cursorTexture, Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY() - cursorTexture.getHeight());
		batch.end();
		frameBuffer.end();
		frameBuffer.getColorBufferTexture().bind(0);
		shaderProgram.bind();
		shaderProgram.setUniformi("u_texture", 0);
		shaderProgram.setUniformi("u_time", (int) TimeUtils.nanoTime());
		batch.setShader(shaderProgram);
		batch.begin();
		crtEffect.setSize(fboWidth, fboHeight);
		crtEffect.draw(batch);
		batch.end();
		batch.setShader(null);
	}

	@Override
	public void dispose( ) {
		stage.dispose();
		shaderProgram.dispose();
		frameBuffer.dispose();
	}
}
