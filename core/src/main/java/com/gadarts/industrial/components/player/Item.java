package com.gadarts.industrial.components.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Pool;
import com.gadarts.industrial.shared.model.pickups.ItemDefinition;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Item implements Pool.Poolable {
	private ItemDefinition definition;
	private Texture image;

	@Setter
	private int row;

	@Setter
	private int col;

	public void init(final ItemDefinition definition, final int row, final int col, final Texture image) {
		this.definition = definition;
		this.row = row;
		this.col = col;
		this.image = image;
	}

	public boolean isWeapon( ) {
		return false;
	}

	@Override
	public void reset( ) {

	}

}
