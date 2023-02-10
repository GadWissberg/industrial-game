package com.gadarts.industrial.map;

import com.badlogic.gdx.graphics.Color;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class ColorDeserializer implements JsonDeserializer<Color> {
	@Override
	public Color deserialize(JsonElement json,
							 Type typeOfT,
							 JsonDeserializationContext context) throws JsonParseException {
		return Color.valueOf(json.getAsString());
	}
}
