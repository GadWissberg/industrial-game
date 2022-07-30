package com.gadarts.industrial.components.character;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.gadarts.industrial.components.GameComponent;
import com.gadarts.industrial.shared.model.characters.enemies.WeaponsDefinitions;
import com.gadarts.industrial.systems.character.commands.CharacterCommand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharacterComponent implements GameComponent {
	public final static float CHAR_RAD = 0.3f;

	private static final Vector2 auxVector = new Vector2();

	private CharacterCommand command;
	private Entity target;
	private CharacterRotationData rotationData = new CharacterRotationData();
	private CharacterSpriteData characterSpriteData;
	private CharacterSoundData soundData = new CharacterSoundData();
	private CharacterSkills skills = new CharacterSkills();

	@Setter(AccessLevel.NONE)
	private OnGoingAttack onGoingAttack = new OnGoingAttack();
	private WeaponsDefinitions primaryAttack;

	@Override
	public void reset( ) {
		target = null;
		rotationData.reset();
	}

	public void init(CharacterSpriteData characterSpriteData,
					 CharacterSoundData soundData,
					 CharacterSkillsParameters skills,
					 WeaponsDefinitions primaryAttack) {
		this.characterSpriteData = characterSpriteData;
		this.skills.applyParameters(skills);
		this.soundData.set(soundData);
		this.primaryAttack = primaryAttack;
	}

	public void dealDamage(final int damagePoints) {
		skills.getHealthData().dealDamage(damagePoints);
	}

	public enum AttackType {
		PRIMARY, SECONDARY
	}

}
