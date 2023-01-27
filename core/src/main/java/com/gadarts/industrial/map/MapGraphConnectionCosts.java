package com.gadarts.industrial.map;

public enum MapGraphConnectionCosts {
	CLEAN, HEIGHT_DIFF;

	public float getCostValue( ) {
		return ordinal() + 1;
	}
}
