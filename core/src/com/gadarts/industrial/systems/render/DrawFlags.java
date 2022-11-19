package com.gadarts.industrial.systems.render;

import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.console.commands.ConsoleCommandParameter;
import com.gadarts.industrial.console.commands.types.SkipRenderCommand;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class DrawFlags {

	private boolean drawGround = !DebugSettings.HIDE_GROUND;
	private boolean drawWalls = !DebugSettings.HIDE_WALLS;
	private boolean drawEnemy = !DebugSettings.HIDE_ENEMIES;
	private boolean drawEnv = !DebugSettings.HIDE_ENVIRONMENT_OBJECTS;
	private boolean drawCursor = !DebugSettings.HIDE_CURSOR;

	void applySkipRenderCommand(final ConsoleCommandParameter parameter) {
		String alias = parameter.getAlias();
		boolean value = !parameter.getParameterValue();
		Map<String, DrawFlagSet> map = SkipRenderCommand.getMap();
		if (map.containsKey(alias)) {
			map.get(alias).run(this, value);
		}
	}
}
