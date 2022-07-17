package com.gadarts.industrial.systems.character.actions;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterCommandContext;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;

import java.util.List;

public interface CharacterCommandImplementation {
	void initialize(Entity character,
					SystemsCommonData commonData,
					Object additionalData,
					List<CharacterSystemEventsSubscriber> subscribers);

	boolean reactToFrameChange(SystemsCommonData systemsCommonData,
							   Entity character,
							   TextureAtlas.AtlasRegion newFrame,
							   List<CharacterSystemEventsSubscriber> subscribers,
							   CharacterCommandContext commandContext);
}
