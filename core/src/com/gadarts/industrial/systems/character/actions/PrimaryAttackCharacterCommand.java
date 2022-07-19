package com.gadarts.industrial.systems.character.actions;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.character.OnGoingAttack;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.model.characters.enemies.WeaponsDefinitions;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterCommandContext;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;

import java.util.List;

public class PrimaryAttackCharacterCommand implements CharacterCommandImplementation {

	private final static Vector3 auxVector3_1 = new Vector3();
	private final static Vector3 auxVector3_2 = new Vector3();

	@Override
	public void initialize(Entity character,
						   SystemsCommonData commonData,
						   Object additionalData,
						   List<CharacterSystemEventsSubscriber> subscribers) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		Entity target = characterComponent.getTarget();
		if (target != null) {
			characterComponent.getRotationData().setRotating(true);
		}
		CharacterComponent charComp = ComponentsMapper.character.get(character);
		WeaponsDefinitions primary = charComp.getPrimaryAttack();
		int bulletsToShoot = MathUtils.random(primary.getMinNumberOfBullets(), primary.getMaxNumberOfBullets());
		charComp.getOnGoingAttack().initialize(CharacterComponent.AttackType.PRIMARY, bulletsToShoot);
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

	private boolean engagePrimaryAttack(Entity character,
										TextureAtlas.AtlasRegion newFrame,
										SystemsCommonData commonData,
										List<CharacterSystemEventsSubscriber> subscribers) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		OnGoingAttack onGoingAttack = characterComponent.getOnGoingAttack();
		if (onGoingAttack.isDone()) return false;

		if (newFrame.index == characterComponent.getCharacterSpriteData().getPrimaryAttackHitFrameIndex()) {
			CharacterDecalComponent charDecalComp = ComponentsMapper.characterDecal.get(character);
			MapGraphNode positionNode = commonData.getMap().getNode(charDecalComp.getDecal().getPosition());
			Vector3 positionNodeCenterPosition = positionNode.getCenterPosition(auxVector3_1);
			Vector3 direction = calculateDirectionToTarget(characterComponent, positionNodeCenterPosition, commonData);
			for (CharacterSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onCharacterEngagesPrimaryAttack(character, direction, positionNodeCenterPosition);
			}
			onGoingAttack.bulletShot();
		}
		return false;
	}

	@Override
	public boolean reactToFrameChange(SystemsCommonData systemsCommonData,
									  Entity character,
									  TextureAtlas.AtlasRegion newFrame,
									  List<CharacterSystemEventsSubscriber> subscribers,
									  CharacterCommandContext commandContext) {
		return engagePrimaryAttack(character, newFrame, systemsCommonData, subscribers);
	}
}
