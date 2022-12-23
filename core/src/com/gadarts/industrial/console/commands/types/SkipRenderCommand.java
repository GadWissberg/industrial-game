package com.gadarts.industrial.console.commands.types;

import com.gadarts.industrial.console.Console;
import com.gadarts.industrial.console.commands.ConsoleCommand;
import com.gadarts.industrial.console.commands.ConsoleCommandParameter;
import com.gadarts.industrial.console.commands.ConsoleCommandsList;
import com.gadarts.industrial.systems.render.DrawFlagSet;
import com.gadarts.industrial.systems.render.DrawFlags;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SkipRenderCommand extends ConsoleCommand {
	private static final Map<String, DrawFlagSet> map = new HashMap<>();

	public static Map<String, DrawFlagSet> getMap( ) {
		return map;
	}

	@Override
	protected ConsoleCommandsList getCommandEnumValue( ) {
		return ConsoleCommandsList.SKIP_RENDER;
	}

	public static class GroundParameter extends SkipRenderCommandParameter {

		public static final String ALIAS = "ground";

		public GroundParameter( ) {
			super(DESCRIPTION, ALIAS, DrawFlags::setDrawGround);
		}

	}

	public static class WallsParameter extends SkipRenderCommandParameter {

		public static final String ALIAS = "walls";

		public WallsParameter( ) {
			super(DESCRIPTION, ALIAS, DrawFlags::setDrawWalls);
		}

	}

	public static class EnemyParameter extends SkipRenderCommandParameter {

		public static final String ALIAS = "enemy";

		public EnemyParameter( ) {
			super(DESCRIPTION, ALIAS, DrawFlags::setDrawEnemy);
		}

	}

	public static class EnvironmentObjectParameter extends SkipRenderCommandParameter {

		public static final String ALIAS = "env";

		public EnvironmentObjectParameter( ) {
			super(DESCRIPTION, ALIAS, DrawFlags::setDrawEnv);
		}

	}

	private abstract static class SkipRenderCommandParameter extends ConsoleCommandParameter {
		public static final String DESCRIPTION = "0 - Renders as normal. 1 - Skips.";

		public SkipRenderCommandParameter(final String description, final String alias, final DrawFlagSet drawFlagSet) {
			super(description, alias);
			Optional.ofNullable(drawFlagSet).ifPresent(set -> map.put(alias, set));
		}

		@Override
		public void run(final String value, final Console console) {
			String alias = getAlias();
			defineParameterValue(
					value,
					console,
					String.format("%s rendering is skipped.", alias),
					String.format("%s rendering is back to normal.", alias)
			);
			console.notifyCommandExecution(ConsoleCommandsList.SKIP_RENDER, this);
		}
	}
}
