package com.gadarts.industrial.console.commands;

import com.badlogic.gdx.utils.StringBuilder;
import com.gadarts.industrial.console.Console;
import com.gadarts.industrial.systems.SystemsCommonData;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.IntStream;

public abstract class ConsoleCommand {
	private static final String NO_PARAMETERS = "No parameters were supplied. Please supply one or more of the following: %s";
	private final StringBuilder stringBuilder = new StringBuilder();

	public ConsoleCommandResult run(final Console console, final Map<String, String> parameters, SystemsCommonData systemsCommonData) {
		ConsoleCommandsList commandDef = getCommandEnumValue();
		ConsoleCommandResult result = console.notifyCommandExecution(commandDef);
		if (!parameters.isEmpty()) {
			parameters.forEach((key, value) -> Arrays.stream(commandDef.getParameters()).forEach(parameter -> {
				if (key.equalsIgnoreCase(parameter.getAlias())) {
					parameter.run(value, console);
				}
			}));
		} else if (commandDef.getParameters().length > 0) {
			printNoParameters(result);
		}
		return result;
	}

	private void printNoParameters(final ConsoleCommandResult result) {
		int length = getCommandEnumValue().getParameters().length;
		IntStream.range(0, length).forEach(i -> {
			stringBuilder.append(getCommandEnumValue().getParameters()[i].getAlias());
			if (i < length - 1) {
				stringBuilder.append(", ");
			}
		});
		result.setMessage(String.format(NO_PARAMETERS, stringBuilder));
	}

	protected abstract ConsoleCommandsList getCommandEnumValue();

}
