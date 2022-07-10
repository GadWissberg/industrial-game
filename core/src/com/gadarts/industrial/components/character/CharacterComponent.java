package com.gadarts.industrial.components.character;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.gadarts.industrial.components.GameComponent;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.model.characters.enemies.WeaponsDefinitions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharacterComponent implements GameComponent {
	public final static float CHAR_RAD = 0.3f;

	private static final Vector2 auxVector = new Vector2();
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private MapGraphNode nextNode;
	private CharacterMotivationData motivationData = new CharacterMotivationData();
	private Entity target;
	private CharacterRotationData rotationData = new CharacterRotationData();
	private CharacterSpriteData characterSpriteData;
	private CharacterSoundData soundData = new CharacterSoundData();
	private CharacterSkills skills = new CharacterSkills();

	@Setter(AccessLevel.NONE)
	private OnGoingAttack onGoingAttack = new OnGoingAttack();
	private WeaponsDefinitions primaryAttack;

	public MapGraphNode getNextNode( ) {
		return nextNode;
	}

	public void setNextNode(final MapGraphNode newValue) {
		this.nextNode = newValue;
	}

	public void setMotivation(final CharacterMotivation characterMotivation) {
		setMotivation(characterMotivation, null);
	}

	public void setMotivation(final CharacterMotivation characterMotivation, final Object additionalData) {
		motivationData.setMotivation(characterMotivation);
		motivationData.setMotivationAdditionalData(additionalData);
	}

	@Override
	public void reset( ) {
		nextNode = null;
		motivationData.reset();
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
