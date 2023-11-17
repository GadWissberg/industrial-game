package com.gadarts.industrial.systems.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.TimeUtils;
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.GameStates;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.screens.GameLifeCycleManager;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetManager;

import java.util.Arrays;


public class MenuHandlerImpl implements MenuHandler {

	private final GameAssetManager assetsManager;
	private final SoundPlayer soundPlayer;
	private final Stage stage;
	private final Texture cursorTexture;
	private final GameLifeCycleManager gameLifeCycleManager;
	private Table menuTable;
	private ShaderProgram shaderProgram;
	private FrameBuffer frameBuffer;
	private Sprite crtEffect;
	private String currentMenu;
	private int uniformLocationCrtBend;
	private int uniformLocationTime;
	private int uniformLocationNoise;
	private float crtIntroEffectProgress;
	private Table menuOptionsTable;
	private MenuOption continueOption;
	private float menuOptionsTableHeight;

	public MenuHandlerImpl(GameAssetManager assetsManager,
						   SoundPlayer soundPlayer,
						   String versionName,
						   GameLifeCycleManager gameLifeCycleManager) {
		this.assetsManager = assetsManager;
		this.soundPlayer = soundPlayer;
		this.gameLifeCycleManager = gameLifeCycleManager;
		soundPlayer.playSound(Assets.Sounds.INTRO_WHITE_NOISE);
		soundPlayer.playSound(Assets.Sounds.MENU_LOOP);
		stage = new Stage();
		Image uci = new Image(assetsManager.getTexture(Assets.UiTextures.UCI));
		uci.setPosition(100, 100);
		stage.addActor(uci);
		stage.addActor(new Image(assetsManager.getTexture(Assets.UiTextures.MENU_BACKGROUND)));
		stage.setDebugAll(DebugSettings.DISPLAY_USER_INTERFACE_OUTLINES);
		createMenu(versionName);
		createCrtEffect();
		stage.addListener(new InputListener() {
			@Override
			public boolean keyDown(InputEvent event, int keycode) {
				if (keycode == Input.Keys.ESCAPE && !currentMenu.equals(MainMenuOptions.MAIN_MENU_NAME)) {
					applyMenuOptions(MainMenuOptions.values());
					return true;
				}
				return false;
			}
		});
		cursorTexture = assetsManager.getTexture(Assets.UiTextures.CURSOR);
	}

	private void createCrtEffect( ) {
		shaderProgram = new ShaderProgram(
				assetsManager.getShader(Assets.Shaders.BASIC_VERTEX),
				assetsManager.getShader(Assets.Shaders.MENU_CRT_FRAGMENT));
		uniformLocationCrtBend = shaderProgram.getUniformLocation("u_crtBend");
		uniformLocationTime = shaderProgram.getUniformLocation("u_time");
		uniformLocationNoise = shaderProgram.getUniformLocation("u_noise");
		shaderProgram.setUniformi("u_texture", 0);
		stage.getBatch().setShader(shaderProgram);
		frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
		crtEffect = new Sprite(frameBuffer.getColorBufferTexture());
		crtEffect.flip(false, true);
	}

	private void createMenu(String versionName) {
		if (menuTable != null) {
			menuTable.remove();
		}
		final Table menuTable;
		menuTable = new Table();
		menuTable.setDebug(DebugSettings.DISPLAY_USER_INTERFACE_OUTLINES);
		menuTable.add(new Image(assetsManager.getTexture(Assets.UiTextures.LOGO))).row();
		menuTable.add(new Label(versionName, new Label.LabelStyle(assetsManager.getFont(Assets.Fonts.CONSOLA), FONT_COLOR_REGULAR)))
				.left()
				.row();
		menuTable.setSize(stage.getWidth(), stage.getHeight());
		this.menuTable = menuTable;
		menuOptionsTable = new Table();
		this.menuTable.add(menuOptionsTable);
		stage.addActor(menuTable);
	}


	@Override
	public void applyMenuOptions(MenuOptionDefinition[] options) {
		menuOptionsTable.clear();
		currentMenu = options[0].getMenuName();
		BitmapFont smallFont = assetsManager.getFont(Assets.Fonts.MENU);
		Label.LabelStyle style = new Label.LabelStyle(smallFont, FONT_COLOR_REGULAR);
		Arrays.stream(options).forEach(o -> {
			MenuOption menuOption = new MenuOption(
					o,
					style,
					soundPlayer,
					this,
					gameLifeCycleManager);
			menuOptionsTable.add(menuOption).row();
			if (o == MainMenuOptions.CONTINUE) {
				continueOption = menuOption;
			}
		});
		handleContinueOptionVisibility();
		if (menuOptionsTableHeight > 0) {
			menuTable.getCell(menuOptionsTable).height(menuOptionsTableHeight);
		}
	}

	@Override
	public void render(float delta) {
		frameBuffer.begin();
		int fboWidth = Gdx.graphics.getWidth();
		int fboHeight = Gdx.graphics.getHeight();
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.getBatch().setShader(null);
		stage.act(delta);
		stage.draw();
		stage.getBatch().begin();
		stage.getBatch().draw(cursorTexture, Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY() - cursorTexture.getHeight());
		stage.getBatch().end();
		frameBuffer.end();
		frameBuffer.getColorBufferTexture().bind(0);
		renderCrtEffect(fboWidth, fboHeight);
		stage.getBatch().setShader(null);
	}

	@Override
	public void show( ) {
		applyMenuOptions(MainMenuOptions.values());
		menuOptionsTableHeight = menuOptionsTable.getPrefHeight();
		handleContinueOptionVisibility();
		Gdx.input.setInputProcessor(stage);
	}

	private void handleContinueOptionVisibility( ) {
		boolean isPaused = gameLifeCycleManager.getGameState() == GameStates.GAME_PAUSED;
		continueOption.setVisible(isPaused);
	}

	private void renderCrtEffect(int fboWidth, int fboHeight) {
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		shaderProgram.bind();
		if (crtIntroEffectProgress < 1) {
			shaderProgram.setUniformf(uniformLocationCrtBend, Interpolation.exp10.apply(0F, 3F, crtIntroEffectProgress));
			shaderProgram.setUniformf(uniformLocationNoise, Interpolation.exp10.apply(1F, 0.02F, crtIntroEffectProgress));
			crtIntroEffectProgress += 0.01F;
		}
		shaderProgram.setUniformi(uniformLocationTime, (int) TimeUtils.nanoTime());
		stage.getBatch().setShader(shaderProgram);
		stage.getBatch().begin();
		crtEffect.setSize(fboWidth, fboHeight);
		crtEffect.draw(stage.getBatch());
		stage.getBatch().end();
	}

	@Override
	public void dispose( ) {
		stage.dispose();
		shaderProgram.dispose();
		frameBuffer.dispose();
	}
}
