package com.gadarts.industrial.systems.character.commands;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pools;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.character.OnGoingAttack;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.declarations.weapons.WeaponDeclaration;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.utils.GameUtils;

import java.util.List;

/**
 * A character's command that engages its primary attack.
 */
public class PrimaryAttackCharacterCommand extends CharacterCommand {
	private final static Vector3 auxVector3_1 = new Vector3();

	private final static Vector3 auxVector3_2 = new Vector3();
	private final static Vector2 auxVector2 = new Vector2();

	private static int randomNumberOfBullets(WeaponDeclaration primary) {
		return MathUtils.random(primary.numberOfBulletsMin(), primary.numberOfBulletsMax());
	}

	@Override
	public boolean initialize(Entity character,
							  SystemsCommonData commonData,
							  Object additionalData,
							  List<CharacterSystemEventsSubscriber> subscribers) {
		if (checkAdjacentForMelee(character, commonData)) return true;
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		if (characterComponent.getTarget() != null) {
			characterComponent.getRotationData().setRotating(true);
		}
		WeaponDeclaration primary = characterComponent.getPrimaryAttack();
		int bulletsToShoot = primary.melee() ? 1 : randomNumberOfBullets(primary);
		characterComponent.getOnGoingAttack().initialize(CharacterComponent.AttackType.PRIMARY, bulletsToShoot);
		return false;
	}

	@Override
	public void reset( ) {

	}

	@Override
	public boolean reactToFrameChange(SystemsCommonData systemsCommonData,
									  Entity character,
									  TextureAtlas.AtlasRegion newFrame,
									  List<CharacterSystemEventsSubscriber> subscribers) {
		engagePrimaryAttack(character, newFrame, systemsCommonData, subscribers);
		return false;
	}

	@Override
	public void free( ) {
		Pools.get(PrimaryAttackCharacterCommand.class).free(this);
	}

	private boolean checkAdjacentForMelee(Entity character, SystemsCommonData commonData) {
		if (!ComponentsMapper.character.get(character).getPrimaryAttack().melee()) return false;
		Entity target = ComponentsMapper.character.get(character).getTarget();
		Decal targetDecal = ComponentsMapper.characterDecal.get(target).getDecal();
		MapGraph map = commonData.getMap();
		MapGraphNode targetNode = map.getNode(targetDecal.getPosition());
		MapGraphNode characterNode = map.getNode(ComponentsMapper.characterDecal.get(character).getDecal().getPosition());
		return !map.isNodesAdjacent(characterNode, targetNode, GameUtils.calculateCharacterHeight(character) / 2F);
	}

	private Vector3 calculateDirectionToTarget(CharacterComponent characterComp,
											   Vector3 positionNodeCenterPosition,
											   SystemsCommonData commonData) {
		CharacterDecalComponent targetDecalComp = ComponentsMapper.characterDecal.get(characterComp.getTarget());
		MapGraphNode targetNode = commonData.getMap().getNode(targetDecalComp.getDecal().getPosition());
		Vector3 targetNodeCenterPosition = targetNode.getCenterPosition(auxVector3_2);
		targetNodeCenterPosition.y += 0.5f;
		return targetNodeCenterPosition.sub(positionNodeCenterPosition);
	}

	private void engagePrimaryAttack(Entity character,
									 TextureAtlas.AtlasRegion newFrame,
									 SystemsCommonData commonData,
									 List<CharacterSystemEventsSubscriber> subscribers) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		OnGoingAttack onGoingAttack = characterComponent.getOnGoingAttack();
		if (onGoingAttack.isDone()) return;

		int primaryAttackHitFrameIndex = GameUtils.getPrimaryAttackHitFrameIndexForCharacter(character, commonData);
		if (newFrame.index == primaryAttackHitFrameIndex) {
			CharacterDecalComponent charDecalComp = ComponentsMapper.characterDecal.get(character);
			MapGraphNode positionNode = commonData.getMap().getNode(charDecalComp.getDecal().getPosition());
			Vector3 positionNodeCenterPosition = positionNode.getCenterPosition(auxVector3_1);
			Vector3 direction = calculateDirectionToTarget(characterComponent, positionNodeCenterPosition, commonData);
			characterComponent.setFacingDirection(Direction.findDirection(auxVector2.set(direction.x, direction.z)));
			for (CharacterSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onCharacterEngagesPrimaryAttack(character, direction, positionNodeCenterPosition);
			}
			onGoingAttack.bulletShot();
			if (onGoingAttack.getBulletsToShoot() <= 0) {
				WeaponDeclaration primaryAttack = characterComponent.getPrimaryAttack();
				consumeTurnTime(character, primaryAttack.duration());
			}
		}
	}

}
