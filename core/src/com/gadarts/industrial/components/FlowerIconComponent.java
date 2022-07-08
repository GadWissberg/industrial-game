package com.gadarts.industrial.components;

import lombok.Getter;

@Getter
public class FlowerIconComponent implements GameComponent {
	private long timeOfCreation;

	@Override
	public void reset( ) {

	}

	public void init(final long timeOfCreation) {
		this.timeOfCreation = timeOfCreation;
	}
}
