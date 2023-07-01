package com.gadarts.industrial.components.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Pool;
import com.gadarts.industrial.shared.assets.declarations.pickups.ItemDeclaration;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Item implements Pool.Poolable {
	private ItemDeclaration declaration;
	private Texture image;

	@Setter
	private int row;

	@Setter
	private int col;

	public void init(ItemDeclaration definition, int row, int col, Texture image) {
		this.declaration = definition;
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
