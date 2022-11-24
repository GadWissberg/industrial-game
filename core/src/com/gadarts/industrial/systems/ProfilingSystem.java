package com.gadarts.industrial.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.StringBuilder;
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
import com.gadarts.industrial.console.commands.ConsoleCommandParameter;
import com.gadarts.industrial.console.commands.ConsoleCommandResult;
import com.gadarts.industrial.console.commands.ConsoleCommands;
import com.gadarts.industrial.console.commands.ConsoleCommandsList;
import com.gadarts.industrial.console.commands.types.ProfilerCommand;
import com.gadarts.industrial.shared.assets.GameAssetsManager;

public class ProfilingSystem extends GameSystem<SystemEventsSubscriber> {
	public static final String WARNING_COLOR = "[RED]";
	public static final int LABELS_ORIGIN_OFFSET_FROM_TOP = 200;
	private static final String VISIBLE_OBJECTS_STRING = "Visible objects: ";
	private static final String LABEL_FPS = "FPS: ";
	private static final String LABEL_JAVA_HEAP_USAGE = "Java heap usage: ";
	private static final String LABEL_NATIVE_HEAP_USAGE = "Native heap usage: ";
	private static final String LABEL_GL_CALL = "Total openGL calls: ";
	private static final String LABEL_GL_DRAW_CALL = "Draw calls: ";
	private static final String LABEL_GL_SHADER_SWITCHES = "Shader switches: ";
	private static final String LABEL_GL_TEXTURE_BINDINGS = "Texture bindings: ";
	private static final String LABEL_GL_VERTEX_COUNT = "Vertex count: ";
	private static final String SUFFIX_MB = "MB";
	private static final String LABEL_VERSION = "Version: ";
	private static final int VERTEX_COUNT_WARNING_LIMIT = 40000;
	private static final char SEPARATOR = '/';
	private final GLProfiler glProfiler;
	private final StringBuilder stringBuilder;
	private Label label;
	private ImmutableArray<Entity> modelInstanceEntities;
	private BitmapFont font;

	public ProfilingSystem(GameAssetsManager assetsManager,
						   GameLifeCycleHandler lifeCycleHandler) {
		super(assetsManager, lifeCycleHandler);
		glProfiler = new GLProfiler(Gdx.graphics);
		stringBuilder = new StringBuilder();
		setGlProfiler();
	}

	@Override
	public boolean onCommandRun(final ConsoleCommands command, final ConsoleCommandResult consoleCommandResult) {
		return onCommandRun(command, consoleCommandResult, null);
	}

	/**
	 * Toggles the GLProfiler.
	 */
	private void toggle( ) {
		if (glProfiler.isEnabled()) {
			glProfiler.disable();
		} else {
			glProfiler.enable();
			reset();
		}
		stringBuilder.clear();
		label.setVisible(glProfiler.isEnabled());
	}

	private String reactToCommand(final ConsoleCommands command) {
		String msg = null;
		if (command == ConsoleCommandsList.PROFILER) {
			toggle();
			msg = glProfiler.isEnabled() ? ProfilerCommand.PROFILING_ACTIVATED : ProfilerCommand.PROFILING_DEACTIVATED;
		}
		return msg;
	}

	@Override
	public boolean onCommandRun(final ConsoleCommands command,
								final ConsoleCommandResult consoleCommandResult,
								final ConsoleCommandParameter parameter) {
		String msg = reactToCommand(command);
		boolean result = false;
		if (msg != null) {
			consoleCommandResult.setMessage(msg);
			result = true;
		}
		return result;
	}

	/**
	 * Resets the GLProfiler.
	 */
	@Override
	public void reset( ) {
		glProfiler.reset();
	}

	private Label addLabel(BitmapFont font) {
		final Label label;
		label = new Label(stringBuilder, new Label.LabelStyle(font, Color.WHITE));
		label.setPosition(0, Gdx.graphics.getHeight() - LABELS_ORIGIN_OFFSET_FROM_TOP);
		getSystemsCommonData().getUiStage().addActor(label);
		label.setZIndex(0);
		return label;
	}

	private void setGlProfiler( ) {
		if (Gdx.app.getLogLevel() == Application.LOG_DEBUG && DebugSettings.SHOW_GL_PROFILING) {
			glProfiler.enable();
		}
	}

	@Override
	public void onSystemReset(SystemsCommonData systemsCommonData) {
		super.onSystemReset(systemsCommonData);
		modelInstanceEntities = getEngine().getEntitiesFor(Family.all(ModelInstanceComponent.class).get());
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		if (Gdx.app.getLogLevel() == Application.LOG_DEBUG && glProfiler.isEnabled()) {
			displayLabels();
		}
	}

	private void displayMemoryLabels( ) {
		displayLine(LABEL_JAVA_HEAP_USAGE, Gdx.app.getJavaHeap() / (1024L * 1024L), false);
		stringBuilder.append(' ').append(SUFFIX_MB).append('\n');
		displayLine(LABEL_NATIVE_HEAP_USAGE, Gdx.app.getNativeHeap() / (1024L * 1024L), false);
		stringBuilder.append(' ').append(SUFFIX_MB).append('\n');
	}

	private void displayLine(final String label, final Object value, final boolean addEndOfLine) {
		stringBuilder.append(label);
		stringBuilder.append(value);
		if (addEndOfLine) {
			stringBuilder.append('\n');
		}
	}

	private void displayLabels( ) {
		stringBuilder.setLength(0);
		displayLine(LABEL_FPS, Gdx.graphics.getFramesPerSecond());
		displayMemoryLabels();
		displayGlProfiling();
		stringBuilder.append("\n").append(LABEL_VERSION).append(getSystemsCommonData().getVersionName());
		label.setText(stringBuilder);
	}

	private void displayGlProfiling( ) {
		displayLine(LABEL_GL_CALL, glProfiler.getCalls());
		displayLine(LABEL_GL_DRAW_CALL, glProfiler.getDrawCalls());
		displayLine(LABEL_GL_SHADER_SWITCHES, glProfiler.getShaderSwitches());
		displayLine(LABEL_GL_TEXTURE_BINDINGS, glProfiler.getTextureBindings() - 1);
		displayLine(LABEL_GL_VERTEX_COUNT, glProfiler.getVertexCount().total, VERTEX_COUNT_WARNING_LIMIT);
		displayNumberOfVisibleObjects();
		glProfiler.reset();
	}

	private void displayNumberOfVisibleObjects( ) {
		stringBuilder.append(VISIBLE_OBJECTS_STRING);
		stringBuilder.append(getSystemsCommonData().getNumberOfVisible());
		stringBuilder.append(SEPARATOR);
		stringBuilder.append(modelInstanceEntities.size());
		stringBuilder.append('\n');
	}

	private void displayLine(final String label, final Object value, final int warningThreshold) {
		stringBuilder.append(label);
		boolean displayWarning = value instanceof Float && warningThreshold <= ((float) value);
		if (displayWarning) {
			stringBuilder.append(WARNING_COLOR);
		}
		stringBuilder.append(value);
		if (displayWarning) {
			stringBuilder.append("[WHITE]");
		}
		stringBuilder.append('\n');
	}

	private void displayLine(final String label, final Object value) {
		displayLine(label, value, true);
	}

	@Override
	public Class<SystemEventsSubscriber> getEventsSubscriberClass( ) {
		return null;
	}

	@Override
	public void initializeData( ) {
		font = new BitmapFont();
		font.getData().markupEnabled = true;
		label = addLabel(font);
	}

	@Override
	public void dispose( ) {
		font.dispose();
	}
}
