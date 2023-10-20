package com.gadarts.industrial.console;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.StringBuilder;
import com.gadarts.industrial.console.commands.ConsoleCommandsList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.gadarts.industrial.console.Console.INPUT_FIELD_NAME;
import static com.gadarts.industrial.console.Console.TEXT_VIEW_NAME;

@RequiredArgsConstructor
public class ConsoleComponents {
	private static final String INPUT_SIGN = ">";
	private static final float PADDING = 10f;
	private static final float INPUT_HEIGHT = 20f;

	private final Console console;
	private final ConsoleTextures consoleTextures;
	private final StringBuilder stringBuilder = new StringBuilder();
	private ConsoleScrollPane consoleScrollPane;
	private Image arrow;
	@Getter
	private TextField input;


	public void showArrow(boolean show) {
		arrow.setVisible(show);
	}

	public void update( ) {
		if (consoleScrollPane.isScrollToEnd() || consoleScrollPane.isBottomEdge()) {
			consoleScrollPane.setScrollToEnd(true);
			consoleScrollPane.setScrollPercentY(1);
			arrow.setVisible(false);
		} else if (!arrow.isVisible()) {
			arrow.setVisible(true);
		}
	}

	private void addTextView(TextureRegionDrawable textBackgroundTexture,
							 int consoleHeight,
							 ConsoleTextData consoleTextData,
							 ConsoleTextures consoleTextures,
							 Table console) {
		Label.LabelStyle textStyle = consoleTextData.getTextStyle();
		textStyle.background = textBackgroundTexture;
		float width = Gdx.graphics.getWidth() - PADDING * 2;
		float height = consoleHeight - (INPUT_HEIGHT);
		Label textView = new Label(consoleTextData.getStringBuilder(), textStyle);
		textView.setAlignment(Align.bottomLeft);
		textView.setName(TEXT_VIEW_NAME);
		textView.setWrap(true);
		consoleScrollPane = new ConsoleScrollPane(textView);
		consoleScrollPane.setTouchable(Touchable.disabled);
		Stack textWindowStack = new Stack(consoleScrollPane);
		arrow = new Image(consoleTextures.getArrowTexture());
		arrow.setAlign(Align.bottomRight);
		textWindowStack.add(arrow);
		arrow.setScaling(Scaling.none);
		arrow.setFillParent(false);
		arrow.setVisible(false);
		console.add(textWindowStack).colspan(2).size(width, height).align(Align.bottomLeft).padRight(PADDING).padLeft(PADDING).row();
	}

	private void addInputField(TextureRegionDrawable textBackgroundTexture, ConsoleTextData consoleTextData, Table console) {
		TextureRegionDrawable cursor = new TextureRegionDrawable(consoleTextures.getCursorTexture());
		TextField.TextFieldStyle style = new TextField.TextFieldStyle(
				consoleTextData.getFont(),
				Color.YELLOW,
				cursor,
				null,
				textBackgroundTexture);
		input = new TextField("", style);
		input.setName(INPUT_FIELD_NAME);
		Label arrow = new Label(INPUT_SIGN, consoleTextData.getTextStyle());
		console.add(arrow).padBottom(PADDING).padLeft(PADDING).size(10f, INPUT_HEIGHT);
		console.add(input)
				.size(Gdx.graphics.getWidth() - PADDING * 3, INPUT_HEIGHT)
				.padBottom(PADDING)
				.padRight(PADDING)
				.align(Align.left)
				.row();
		input.setFocusTraversal(false);
	}

	public void applyFoundCommandByTab(java.util.List<ConsoleCommandsList> options) {
		input.setText(options.get(0).name().toLowerCase());
		input.setCursorPosition(input.getText().length());
	}

	public void init(float height,
					 TextField.TextFieldListener textFieldListener,
					 ConsoleTextData consoleTextData,
					 ConsoleInputHistoryHandler consoleInputHistoryHandler,
					 Table console) {
		TextureRegionDrawable textBackgroundTextureRegionDrawable = new TextureRegionDrawable(consoleTextures.getTextBackgroundTexture());
		addTextView(textBackgroundTextureRegionDrawable, (int) height, consoleTextData, consoleTextures, console);
		addInputField(textBackgroundTextureRegionDrawable, consoleTextData, console);
		input.setTextFieldFilter((textField, c) -> c != '\t');
		input.setTextFieldListener(textFieldListener);
		input.addCaptureListener(new ConsoleInputListener(
				this.console,
				consoleTextData,
				consoleInputHistoryHandler,
				input,
				stringBuilder,
				consoleScrollPane));
	}


	public void setScrollToEnd(boolean b) {
		this.consoleScrollPane.setScrollToEnd(b);
	}
}
