package com.gadarts.industrial.components.cd;

import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.components.GameComponent;
import com.gadarts.industrial.components.character.CharacterAnimations;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import lombok.Getter;

import static com.gadarts.industrial.shared.model.characters.CharacterTypes.BILLBOARD_SCALE;


@Getter
public class CharacterDecalComponent implements GameComponent {
	private static final Vector3 auxVector3 = new Vector3();
	private static final Vector2 auxVector2 = new Vector2();
	private Decal decal;
	private CharacterAnimations animations;
	private SpriteType spriteType;
	private Direction direction;

	@Override
	public void reset( ) {
	}

	public void init(final CharacterAnimations animations,
					 final SpriteType type,
					 final Direction direction,
					 final Vector3 position) {
		this.animations = animations;
		this.direction = direction;
		this.spriteType = type;
		createCharacterDecal(animations, type, direction, position);
	}

	private void createCharacterDecal(final CharacterAnimations animations,
									  final SpriteType type,
									  final Direction direction,
									  final Vector3 position) {
		decal = Decal.newDecal(animations.get(type, direction).getKeyFrames()[0], true);//Optimize this - it creates an object each time.
		decal.setScale(BILLBOARD_SCALE);
		decal.setPosition(position);
	}


	public void initializeSprite(final SpriteType type) {
		initializeSprite(type, direction);
	}

	public void initializeSprite(final SpriteType type, final Direction direction) {
		this.spriteType = type;
		this.direction = direction;
	}

	public Vector2 getNodePosition(final Vector2 output) {
		Vector3 position = decal.getPosition();
		Vector2 decalPosition = auxVector2.set(position.x, position.z);
		return output.set(decalPosition.set(MathUtils.floor(auxVector2.x), MathUtils.floor(auxVector2.y)));
	}

	public Vector3 getNodePosition(final Vector3 output) {
		Vector3 position = decal.getPosition();
		Vector3 decalPosition = auxVector3.set(position.x, 0, position.z);
		return output.set(decalPosition.set(MathUtils.floor(auxVector3.x), 0, MathUtils.floor(auxVector3.z)));
	}
}
