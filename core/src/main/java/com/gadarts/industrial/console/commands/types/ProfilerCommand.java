package com.gadarts.industrial.console.commands.types;

import com.gadarts.industrial.console.Console;
import com.gadarts.industrial.console.commands.ConsoleCommand;
import com.gadarts.industrial.console.commands.ConsoleCommandResult;
import com.gadarts.industrial.console.commands.ConsoleCommandsList;
import com.gadarts.industrial.systems.SystemsCommonData;

import java.util.Map;

public class ProfilerCommand extends ConsoleCommand {
	public static final String PROFILING_ACTIVATED = "Profiling info is displayed.";
	public static final String PROFILING_DEACTIVATED = "Profiling info is hidden.";

	@Override
	public ConsoleCommandResult run(final Console console, final Map<String, String> parameters, SystemsCommonData systemsCommonData) {
		return super.run(console, parameters, systemsCommonData);
	}

	@Override
	protected ConsoleCommandsList getCommandEnumValue( ) {
		return ConsoleCommandsList.PROFILER;
	}
}
