package com.gadarts.industrial.console;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.TimeUtils;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetManager;

import java.text.SimpleDateFormat;
import java.util.Optional;

import static com.gadarts.industrial.console.Console.OUTPUT_COLOR;
import static com.gadarts.industrial.console.Console.TEXT_VIEW_NAME;

public class ConsoleTextData implements Disposable {
	private static final String TIME_COLOR = "SKY";

	private final BitmapFont font;
	private final float fontHeight;
	private final StringBuilder stringBuilder = new StringBuilder();
	private final ConsoleTextTimeData consoleTextTimeData = new ConsoleTextTimeData();
	private final Label.LabelStyle textStyle;
	private Stage stage;

	public ConsoleTextData(final GameAssetManager assetManager) {
		font = assetManager.getFont(Assets.Fonts.CONSOLA);
		font.getData().markupEnabled = true;
		textStyle = new Label.LabelStyle(font, Color.WHITE);
		GlyphLayout layout = new GlyphLayout();
		layout.setText(font, "test");
		fontHeight = layout.height;
	}

	public float getFontHeight( ) {
		return fontHeight;
	}

	public StringBuilder getStringBuilder( ) {
		return stringBuilder;
	}

	public Label.LabelStyle getTextStyle( ) {
		return textStyle;
	}

	public BitmapFont getFont( ) {
		return font;
	}

	public void insertNewLog(final String text, final boolean logTime, final String color) {
		consoleTextTimeData.getTimeStamp().setTime(TimeUtils.millis());
		String colorText = Optional.ofNullable(color).isPresent() ? color : OUTPUT_COLOR;
		if (logTime) {
			appendTextWithTime(text, colorText);
		} else stringBuilder.append(colorText).append(text).append('\n');
		stringBuilder.append(OUTPUT_COLOR);
		((Label) stage.getRoot().findActor(TEXT_VIEW_NAME)).setText(stringBuilder);
	}

	private void appendTextWithTime(final String text, final String colorText) {
		SimpleDateFormat date = consoleTextTimeData.getDate();
		stringBuilder.append("[").append(TIME_COLOR).append("]")
				.append(" [").append(date.format(consoleTextTimeData.getTimeStamp())).append("]: ")
				.append(colorText)
				.append(text).append('\n');
	}

	@Override
	public void dispose( ) {
	}

	public void setStage(final Stage stage) {
		this.stage = stage;
	}
}
