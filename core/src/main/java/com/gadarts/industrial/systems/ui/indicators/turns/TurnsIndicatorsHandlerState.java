package com.gadarts.industrial.systems.ui.indicators.turns;

import com.badlogic.ashley.core.Entity;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class TurnsIndicatorsHandlerState {
	private final Map<Entity, TurnsIndicatorIcon> iconsMap = new HashMap<>();
	private final List<TurnsIndicatorIcon> iconsList = new ArrayList<>();
	private Entity currentBorder;

}
