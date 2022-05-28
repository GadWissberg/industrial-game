package com.gadarts.industrial.console.commands;

import com.gadarts.industrial.console.InputParsingFailureException;
import com.gadarts.industrial.console.commands.types.*;
import com.gadarts.industrial.console.commands.types.SkipRenderCommand;

import java.util.Arrays;
import java.util.Optional;

public enum ConsoleCommandsList implements ConsoleCommands {
	PROFILER(new ProfilerCommand(), "Toggles profiler and GL operations stats."),
	SFX("snd", new SfxCommand(), "Toggles sound effects."),
	MELODY("msc", new MusicCommand(), "Toggles background melody."),
	BORDERS("brd", new BordersCommand(), "Toggles UI elements borders visibility."),
	SKIP_RENDER("skp", new SkipRenderCommand(), "Toggles drawing skipping mode for given categories.",
			new SkipRenderCommand.GroundParameter(),
			new SkipRenderCommand.EnemyParameter(),
			new SkipRenderCommand.EnvironmentObjectParameter()),
	FRUSTUM_CULLING("fc", new FrustumCullingCommand(), "Toggles frustum culling."),
	HELP("?", new HelpCommand(), "Displays commands list.");

	public static final String DESCRIPTION_PARAMETERS = " Parameters:%s";
	private final ConsoleCommand command;
	private final String alias;
	private final ConsoleCommandParameter[] parameters;
	private String description;

	ConsoleCommandsList(final ConsoleCommand command, final String description) {
		this(null, command, description);
	}

	ConsoleCommandsList(final String alias, final ConsoleCommand command, final String description, final ConsoleCommandParameter... parameters) {
		this.alias = alias;
		this.command = command;
		this.parameters = parameters;
		this.description = description;
		extendDescriptionWithParameters(parameters);
	}

	public static ConsoleCommandsList findCommandByNameOrAlias(final String input) throws InputParsingFailureException {
		Optional<ConsoleCommandsList> result;
		try {
			result = Optional.of(valueOf(input));
		} catch (IllegalArgumentException e) {
			ConsoleCommandsList[] values = values();
			result = Arrays.stream(values).filter(command ->
					Optional.ofNullable(command.getAlias()).isPresent() &&
							command.getAlias().equalsIgnoreCase(input)).findFirst();
			if (result.isEmpty()) {
				String lowerCase = input.toLowerCase();
				throw new InputParsingFailureException(String.format("'%s' is not recognized as a command.", lowerCase));
			}
		}
		return result.get();
	}

	private void extendDescriptionWithParameters(final ConsoleCommandParameter[] parameters) {
		if (parameters.length > 0) {
			StringBuilder stringBuilder = new StringBuilder();
			Arrays.stream(parameters).forEach(parameter -> stringBuilder
					.append("\n")
					.append("   * ")
					.append(parameter.getAlias())
					.append(": ")
					.append(parameter.getDescription()));
			this.description += String.format(DESCRIPTION_PARAMETERS, stringBuilder);
		}
	}

	public String getAlias( ) {
		return alias;
	}

	public ConsoleCommand getCommandImpl( ) {
		return command;
	}

	public ConsoleCommandParameter[] getParameters( ) {
		return parameters;
	}

	public String getDescription( ) {
		return description;
	}
}
