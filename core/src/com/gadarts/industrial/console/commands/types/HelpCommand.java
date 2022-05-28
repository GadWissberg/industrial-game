package com.gadarts.industrial.console.commands.types;

import com.badlogic.gdx.utils.StringBuilder;
import com.gadarts.industrial.console.Console;
import com.gadarts.industrial.console.commands.ConsoleCommand;
import com.gadarts.industrial.console.commands.ConsoleCommandResult;
import com.gadarts.industrial.console.commands.ConsoleCommandsList;
import com.gadarts.industrial.systems.SystemsCommonData;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class HelpCommand extends ConsoleCommand {


	private static String output;

	@Override
	protected ConsoleCommandsList getCommandEnumValue( ) {
		return ConsoleCommandsList.HELP;
	}

	@Override
	public ConsoleCommandResult run(Console console, Map<String, String> parameters, SystemsCommonData systemsCommonData) {
		if (Optional.ofNullable(output).isEmpty()) {
			initializeMessage(systemsCommonData.getVersionName());
		}
		ConsoleCommandResult consoleCommandResult = new ConsoleCommandResult();
		consoleCommandResult.setMessage(output);
		return consoleCommandResult;
	}

	private void initializeMessage(String versionName) {
		StringBuilder builder = new StringBuilder();
		Arrays.stream(ConsoleCommandsList.values()).forEach(command -> {
			builder.append(" - ").append(command.name().toLowerCase());
			if (Optional.ofNullable(command.getAlias()).isPresent()) {
				builder.append(" (also '").append(command.getAlias()).append("')");
			}
			builder.append(": ").append(command.getDescription()).append("\n");
		});
		output = String.format("Welcome to necronemes v%s. The command pattern is '<COMMAND_NAME> " +
				"-<PARAMETER_1> <PARAMETER_1_VALUE>'. The following commands are available:\n%s", versionName, builder);
	}
}
