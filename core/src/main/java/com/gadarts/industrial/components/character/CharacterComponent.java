package com.gadarts.industrial.components.character;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Queue;
import com.gadarts.industrial.components.GameComponent;
import com.gadarts.industrial.shared.assets.declarations.weapons.WeaponDeclaration;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.systems.character.commands.CharacterCommand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharacterComponent implements GameComponent {
	public final static float CHAR_RAD = 0.3f;
	public static final float TURN_DURATION = 1F;
	public static final float PASSABLE_MAX_HEIGHT_DIFF = 0.3f;
	private static final Vector2 auxVector = new Vector2();
	@Setter(AccessLevel.NONE)
	private Queue<CharacterCommand> commands = new Queue<>();
	private Entity target;
	private CharacterRotationData rotationData = new CharacterRotationData();
	private CharacterSpriteData characterSpriteData;
	private CharacterSoundData soundData = new CharacterSoundData();
	private CharacterAttributes attributes = new CharacterAttributes();
	@Setter(AccessLevel.NONE)
	private OnGoingAttack onGoingAttack = new OnGoingAttack();
	private WeaponDeclaration primaryAttack;
	private float turnTimeLeft;
	private Direction facingDirection;

	@Override
	public void reset( ) {
		commands.clear();
		target = null;
		rotationData.reset();
	}

	public void init(CharacterSpriteData characterSpriteData,
					 CharacterSoundData soundData,
					 CharacterSkillsParameters skills,
					 WeaponDeclaration primaryAttack,
					 Direction direction) {
		this.characterSpriteData = characterSpriteData;
		this.attributes.applyParameters(skills);
		this.soundData.set(soundData);
		this.primaryAttack = primaryAttack;
		this.turnTimeLeft = TURN_DURATION;
		this.facingDirection = direction;
	}

	public void dealDamage(final int damagePoints) {
		attributes.getHealthData().dealDamage(damagePoints);
	}

	public enum AttackType {
		PRIMARY, SECONDARY
	}

}
