package com.gadarts.industrial.systems.character;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.TimeUtils;
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.BulletComponent;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.animation.AnimationComponent;
import com.gadarts.industrial.components.character.*;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.map.MapGraphPath;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.Assets.Sounds;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.assets.SurfaceType;
import com.gadarts.industrial.shared.assets.declarations.pickups.weapons.WeaponDeclaration;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.commands.CharacterCommand;
import com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.character.commands.CommandStates;
import com.gadarts.industrial.systems.enemy.EnemySystemEventsSubscriber;
import com.gadarts.industrial.systems.enemy.ai.EnemyAiStatus;
import com.gadarts.industrial.systems.player.PlayerSystemEventsSubscriber;
import com.gadarts.industrial.systems.projectiles.AttackSystemEventsSubscriber;
import com.gadarts.industrial.systems.render.RenderSystemEventsSubscriber;
import com.gadarts.industrial.systems.turns.GameMode;
import com.gadarts.industrial.systems.turns.TurnsSystemEventsSubscriber;
import com.gadarts.industrial.utils.EntityBuilder;
import com.gadarts.industrial.utils.GameUtils;

import java.util.Map;

import static com.gadarts.industrial.components.ComponentsMapper.character;
import static com.gadarts.industrial.components.player.PlayerComponent.PLAYER_HEIGHT;
import static com.gadarts.industrial.shared.model.characters.Direction.findDirection;
import static com.gadarts.industrial.shared.model.characters.SpriteType.*;

public class CharacterSystem extends GameSystem<CharacterSystemEventsSubscriber> implements
		PlayerSystemEventsSubscriber,
		RenderSystemEventsSubscriber,
		EnemySystemEventsSubscriber,
		AttackSystemEventsSubscriber,
		CharacterSystemEventsSubscriber,
		TurnsSystemEventsSubscriber {
	public static final long MAX_IDLE_ANIMATION_INTERVAL = 10000L;
	public static final int CHARACTER_ROTATION_INTERVAL = 125;
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final Vector3 auxVector3_2 = new Vector3();
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final Vector2 auxVector2_2 = new Vector2();
	private static final Vector2 auxVector2_3 = new Vector2();
	private static final long CHARACTER_PAIN_DURATION = 1000L;
	private static final long MIN_IDLE_ANIMATION_INTERVAL = 2000L;
	private ParticleEffect bloodSplatterEffect;
	private ImmutableArray<Entity> characters;
	private ParticleEffect smallExpEffect;
	private Map<SurfaceType, Sounds> surfaceTypeToStepSound;
	private ImmutableArray<Entity> livingBullets;

	public CharacterSystem(GameAssetManager assetsManager,
						   GameLifeCycleHandler lifeCycleHandler) {
		super(assetsManager, lifeCycleHandler);
	}

	@Override
	public void onNewTurn(Entity entity) {
		if (!character.has(entity)) return;

		character.get(entity).getCharacterSpriteData().setSpriteType(IDLE);
	}

	@Override
	public void onSystemReset(SystemsCommonData systemsCommonData) {
		super.onSystemReset(systemsCommonData);
		characters = getEngine().getEntitiesFor(Family.all(CharacterComponent.class).get());
		surfaceTypeToStepSound = Map.of(
				SurfaceType.CONCRETE, Sounds.STEP_CONCRETE,
				SurfaceType.METAL, Sounds.STEP_METAL);
	}

	@Override
	public Class<CharacterSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return CharacterSystemEventsSubscriber.class;
	}

	@Override
	public void initializeData( ) {
		bloodSplatterEffect = getAssetsManager().getParticleEffect(Assets.ParticleEffects.BLOOD_SPLATTER);
		smallExpEffect = getAssetsManager().getParticleEffect(Assets.ParticleEffects.SMALL_EXP);
		livingBullets = getEngine().getEntitiesFor(Family.all(BulletComponent.class).get());
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		if (getSystemsCommonData().getCurrentGameMode() != GameMode.EXPLORE) {
			Entity current = getSystemsCommonData().getTurnsQueue().first();
			if (character.has(current)) {
				CharacterComponent characterComponent = character.get(current);
				Queue<CharacterCommand> commands = characterComponent.getCommands();
				CharacterAttributes attributes = characterComponent.getAttributes();
				if (attributes.getActionPoints() <= 0 && (commands.isEmpty() || commands.first().getState() == CommandStates.ENDED)) {
					attributes.resetActionPoints();
					characterComponent.getCharacterSpriteData().setSpriteType(IDLE);
					subscribers.forEach(CharacterSystemEventsSubscriber::onCharacterFinishedTurn);
				} else {
					handleCharacterCommand(current);
				}
			}
		} else {
			character.get(getSystemsCommonData().getPlayer()).getAttributes().resetActionPoints();
			handleCharacterCommand(getSystemsCommonData().getPlayer());
		}
		updateCharacters();
	}

	@Override
	public void onEnemyFinishedTurn( ) {
		character.get(getSystemsCommonData().getTurnsQueue().first()).getAttributes().resetActionPoints();
	}

	@Override
	public void onPlayerFinishedTurn( ) {
		character.get(getSystemsCommonData().getTurnsQueue().first()).getAttributes().resetActionPoints();
	}

	private void beginProcessingCommand(Entity character,
										CharacterCommand currentCommand) {
		currentCommand.setState(CommandStates.RUNNING);
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		characterComponent.getRotationData().setRotating(true);
		SystemsCommonData data = getSystemsCommonData();
		boolean alreadyDone = currentCommand.initialize(character, data);
		if (alreadyDone) {
			currentCommand.setState(CommandStates.ENDED);
			characterComponent.getAttributes().setActionPoints(0);
		}
	}

	private void updateCharacters( ) {
		long now = TimeUtils.millis();
		for (int i = 0; i < characters.size(); i++) {
			Entity character = characters.get(i);
			handleIdle(character, now);
			handlePain(character);
		}
	}

	private void handleIdle(Entity character, long now) {
		CharacterSpriteData spriteData = ComponentsMapper.character.get(character).getCharacterSpriteData();
		SpriteType spriteType = spriteData.getSpriteType();
		if (spriteType == IDLE && spriteData.getNextIdleAnimationPlay() < now) {
			long random = MathUtils.random(MAX_IDLE_ANIMATION_INTERVAL - MIN_IDLE_ANIMATION_INTERVAL);
			spriteData.setNextIdleAnimationPlay(now + MIN_IDLE_ANIMATION_INTERVAL + random);
			ComponentsMapper.animation.get(character).getAnimation().setFrameDuration(spriteType.getFrameDuration());
		}
	}

	private void handlePain(final Entity character) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		long lastDamage = characterComponent.getAttributes().getHealthData().getLastDamage();
		CharacterSpriteData spriteData = characterComponent.getCharacterSpriteData();
		if (spriteData.getSpriteType() == PAIN && TimeUtils.timeSinceMillis(lastDamage) > CHARACTER_PAIN_DURATION) {
			spriteData.setSpriteType(IDLE);
		}
	}

	private void applyDamageToCharacter(Entity attacked,
										int damage) {
		applyDamageToCharacter(attacked, damage, null);
	}

	private void applyDamageToCharacter(Entity attacked,
										int damage,
										ModelInstanceComponent bulletModelInstanceComponent) {
		CharacterComponent characterComponent = character.get(attacked);
		int originalValue = characterComponent.getAttributes().getHealthData().getHp();
		if ((ComponentsMapper.player.has(attacked) && !DebugSettings.GOD_MODE)
				|| ComponentsMapper.enemy.has(attacked) && !DebugSettings.ENEMY_INVULNERABLE) {
			characterComponent.dealDamage(damage);
		}
		handleDeath(attacked, originalValue);
		if (ComponentsMapper.player.has(attacked) || ComponentsMapper.enemy.get(attacked).getEnemyDeclaration().human()) {
			addSplatterEffect(bulletModelInstanceComponent, attacked);
		}
	}

	private void addSplatterEffect(final ModelInstanceComponent bulletModelInstanceComponent, Entity attacked) {
		Vector3 position = positionBloodSplatter(bulletModelInstanceComponent, attacked);
		EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
				.addParticleEffectComponent(bloodSplatterEffect, position)
				.finishAndAddToEngine();
	}

	private Vector3 positionBloodSplatter(ModelInstanceComponent bulletModelInstanceComponent, Entity attacked) {
		Vector3 position;
		if (bulletModelInstanceComponent != null) {
			position = bulletModelInstanceComponent.getModelInstance().transform.getTranslation(auxVector3_1);
		} else {
			Decal decal = ComponentsMapper.characterDecal.get(attacked).getDecal();
			MapGraph map = getSystemsCommonData().getMap();
			position = map.getNode(decal.getPosition()).getCenterPosition(auxVector3_2).add(0F, PLAYER_HEIGHT, 0F);
		}
		return position;
	}

	private void handleDeath(final Entity character, int originalValue) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		CharacterHealthData healthData = characterComponent.getAttributes().getHealthData();
		CharacterSoundData soundData = characterComponent.getSoundData();
		if (healthData.getHp() <= 0) {
			characterDies(character, characterComponent, soundData);
		} else {
			characterInPain(character, characterComponent, soundData, originalValue);
		}
	}

	private void characterInPain(Entity character,
								 CharacterComponent characterComponent,
								 CharacterSoundData soundData, int originalValue) {
		getSystemsCommonData().getSoundPlayer().playSound(soundData.getPainSound());
		characterComponent.getCharacterSpriteData().setSpriteType(PAIN);
		for (CharacterSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onCharacterGotDamage(character, originalValue);
		}
	}

	private void characterDies(Entity character, CharacterComponent characterComponent, CharacterSoundData soundData) {
		CharacterSpriteData charSpriteData = characterComponent.getCharacterSpriteData();
		charSpriteData.setSpriteType(LIGHT_DEATH);
		if (ComponentsMapper.animation.has(character)) {
			ComponentsMapper.animation.get(character).resetStateTime();
		}
		createExplosionOnCharacterDeath(character);
		getSystemsCommonData().getSoundPlayer().playSound(soundData.getDeathSound());
		for (CharacterSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onCharacterDies(character);
		}
	}

	private void createExplosionOnCharacterDeath(Entity character) {
		if (ComponentsMapper.enemy.has(character) && !ComponentsMapper.enemy.get(character).getEnemyDeclaration().human()) {
			getSystemsCommonData().getSoundPlayer().playSound(Sounds.SMALL_EXP);
			Decal decal = ComponentsMapper.characterDecal.get(character).getDecal();
			MapGraphNode node = getSystemsCommonData().getMap().getNode(decal.getPosition());
			float height = ComponentsMapper.enemy.get(character).getEnemyDeclaration().height();
			Vector3 pos = node.getCenterPosition(auxVector3_1).add(0F, height / 4F, 0F);
			EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
					.addParticleEffectComponent(smallExpEffect, pos)
					.finishAndAddToEngine();
		}
	}

	@Override
	public void onMeleeAttackAppliedOnTarget(Entity character, Entity target, WeaponDeclaration primaryAttack) {
		MapGraph map = getSystemsCommonData().getMap();
		MapGraphNode srcNode = map.getNode(ComponentsMapper.characterDecal.get(character).getDecal().getPosition());
		MapGraphNode targetNode = map.getNode(ComponentsMapper.characterDecal.get(target).getDecal().getPosition());
		float height;
		height = GameUtils.calculateCharacterHeight(character);
		if (map.areNodesAdjacent(srcNode, targetNode, height / 2F)) {
			applyDamageToCharacter(target, primaryAttack.damage());
		}
	}

	@Override
	public void onBulletSetDestroyed(Entity bullet) {
		commandDone(ComponentsMapper.bullet.get(bullet).getOwner());
	}

	public void commandDone(Entity character) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		characterComponent.getCharacterSpriteData().setSpriteType(IDLE);
		if (getSystemsCommonData().getCurrentGameMode() != GameMode.EXPLORE) {
			for (CharacterSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onCharacterCommandDone(character);
			}
		}
	}

	private void handleModeWithNonLoopingAnimation(final Entity character) {
		AnimationComponent animationComponent = ComponentsMapper.animation.get(character);
		Animation<TextureAtlas.AtlasRegion> animation = animationComponent.getAnimation();
		if (animation.isAnimationFinished(animationComponent.getStateTime())) {
			animationEnded(character, animationComponent, animation);
		}
	}

	private void animationEnded(Entity character,
								AnimationComponent animationComponent,
								Animation<TextureAtlas.AtlasRegion> animation) {
		CharacterComponent characterComp = ComponentsMapper.character.get(character);
		SpriteType spriteType = characterComp.getCharacterSpriteData().getSpriteType();
		OnGoingAttack onGoingAttack = characterComp.getOnGoingAttack();
		if (spriteType.isAddReverse()) {
			handleAnimationReverse(character, animationComponent, animation, spriteType);
		} else if (onGoingAttack.getType() != null && characterComp.getCharacterSpriteData().getSpriteType() == ATTACK_PRIMARY) {
			SystemsCommonData commonData = getSystemsCommonData();
			int primaryAttackHitFrameIndex = GameUtils.getPrimaryAttackHitFrameIndexForCharacter(character, commonData);
			if (onGoingAttack.isDone()) {
				onGoingAttack.setType(null);
				if (!characterComp.getPrimaryAttack().melee()) {
					animationComponent.setStateTime(0);
					animation.setPlayMode(Animation.PlayMode.REVERSED);
				} else {
					animation.setPlayMode(Animation.PlayMode.NORMAL);
					commandDone(character);
				}
			} else {
				animationComponent.setStateTime((primaryAttackHitFrameIndex) * animation.getFrameDuration());
			}
		} else {
			animation.setPlayMode(Animation.PlayMode.NORMAL);
			ComponentsMapper.character.get(character).getCommands().first().setState(CommandStates.ENDED);
			commandDone(character);
		}
	}


	private void handleAnimationReverse(Entity character,
										AnimationComponent animationComponent,
										Animation<TextureAtlas.AtlasRegion> animation,
										SpriteType spriteType) {
		if (animationComponent.getAnimation().getPlayMode() == Animation.PlayMode.REVERSED) {
			if (spriteType.isCommandDoneOnReverseEnd()) {
				CharacterComponent characterComponent = ComponentsMapper.character.get(character);
				characterComponent.getCharacterSpriteData().setSpriteType(IDLE);
				characterComponent.getCommands().first().setState(CommandStates.ENDED);
			}
			animation.setPlayMode(Animation.PlayMode.NORMAL);
		} else {
			animation.setFrameDuration(spriteType.getFrameDuration());
			applyAnimationToReverse(animationComponent);
		}
	}

	private void applyAnimationToReverse(AnimationComponent animationComponent) {
		animationComponent.getAnimation().setPlayMode(Animation.PlayMode.REVERSED);
		animationComponent.resetStateTime();
	}

	private void handleCharacterCommand(Entity character) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		if (characterComponent.getCharacterSpriteData().getSpriteType() == PAIN) return;

		if (!characterComponent.getCommands().isEmpty()) {
			CharacterCommand currentCommand = characterComponent.getCommands().first();
			if (currentCommand.getState() == CommandStates.READY) {
				currentCommand.setState(CommandStates.RUNNING);
				beginProcessingCommand(character, currentCommand);
			} else if (currentCommand.getState() == CommandStates.RUNNING) {
				SpriteType spriteType = characterComponent.getCharacterSpriteData().getSpriteType();
				if (spriteType == PICKUP || spriteType == ATTACK_PRIMARY || spriteType == RELOAD) {
					handleModeWithNonLoopingAnimation(character);
				} else {
					handleRotation(currentCommand, character);
				}
			} else if (currentCommand.getState() == CommandStates.ENDED) {
				if (livingBullets.size() == 0) {
					Pools.free(characterComponent.getCommands().removeFirst());
					commandDone(character);
				}
			}
		} else if (ComponentsMapper.enemy.has(character)) {
			commandDone(character);
		}
	}

	@Override
	public void onEnemyAwaken(Entity enemy, EnemyAiStatus prevAiStatus, boolean wokeBySpottingPlayer) {
		if (!wokeBySpottingPlayer) return;

		SystemsCommonData systemsCommonData = getSystemsCommonData();
		if (systemsCommonData.getCurrentGameMode() == GameMode.EXPLORE) {
			Entity player = systemsCommonData.getPlayer();
			Queue<CharacterCommand> commands = character.get(player).getCommands();
			if (!commands.isEmpty()) {
				commandDone(player);
			}
		}
	}

	private Direction calculateDirectionToDestination(CharacterCommand currentCommand) {
		Entity character = currentCommand.getCharacter();
		Vector3 characterPos = auxVector3_1.set(ComponentsMapper.characterDecal.get(character).getDecal().getPosition());
		Vector2 destPos = currentCommand.getPath().get(currentCommand.getNextNodeIndex()).getCenterPosition(auxVector2_2);
		Vector2 directionToDest = destPos.sub(characterPos.x, characterPos.z).nor();
		return findDirection(directionToDest);
	}


	private void handleRotation(CharacterCommand currentCommand,
								Entity character) {
		CharacterComponent charComp = ComponentsMapper.character.get(character);
		if (charComp.getCharacterSpriteData().getSpriteType() == PAIN) return;

		CharacterRotationData rotData = charComp.getRotationData();
		Vector3 characterPosition = ComponentsMapper.characterDecal.get(character).getDecal().getPosition();
		MapGraphPath commandPath = currentCommand.getPath();
		MapGraphNode characterNode = getSystemsCommonData().getMap().getNode(characterPosition);
		CharacterCommandsDefinitions definition = currentCommand.getDefinition();
		if (!definition.isRotationForbidden() && definition.isRequiresMovement()) {
			if (!characterNode.equals(commandPath.nodes.get(commandPath.getCount() - 1))) {
				if (rotData.isRotating() && TimeUtils.timeSinceMillis(rotData.getLastRotation()) > CHARACTER_ROTATION_INTERVAL) {
					rotate(charComp, rotData, currentCommand);
				}
			}
		} else {
			if (charComp.getTarget() != null) {
				Vector3 targetPosition = ComponentsMapper.characterDecal.get(charComp.getTarget()).getDecal().getPosition();
				Vector3 direction = auxVector3_1.set(targetPosition).sub(characterPosition).nor();
				charComp.setFacingDirection(findDirection(auxVector2_1.set(direction.x, direction.z)));
			}
			rotationDone(rotData, charComp.getCharacterSpriteData());
		}
	}

	private void rotate(CharacterComponent charComponent,
						CharacterRotationData rotationData,
						CharacterCommand currentCommand) {
		rotationData.setLastRotation(TimeUtils.millis());
		Direction directionToDest = calculateDirectionToDestination(currentCommand);
		if (charComponent.getFacingDirection() != directionToDest) {
			Vector2 currentDirVec = charComponent.getFacingDirection().getDirection(auxVector2_1);
			float diff = directionToDest.getDirection(auxVector2_2).angleDeg() - currentDirVec.angleDeg();
			int side = auxVector2_3.set(1, 0).setAngleDeg(diff).angleDeg() > 180 ? -1 : 1;
			Vector3 charPos = ComponentsMapper.characterDecal.get(currentCommand.getCharacter()).getDecal().getPosition();
			Entity floorEntity = getSystemsCommonData().getMap().getNode(charPos).getEntity();
			int fogOfWarSig = floorEntity != null ? ComponentsMapper.floor.get(floorEntity).getFogOfWarSignature() : 0;
			boolean isNodeHidden = (fogOfWarSig & 16) == 16;
			Direction newDir = isNodeHidden ? directionToDest : findDirection(currentDirVec.rotateDeg(45f * side));
			charComponent.setFacingDirection(newDir);
		} else {
			rotationDone(rotationData, charComponent.getCharacterSpriteData());
		}
	}

	@Override
	public void onCharacterNodeChanged(Entity entity, MapGraphNode oldNode, MapGraphNode newNode) {
		CharacterComponent characterComponent = character.get(entity);
		characterComponent.getRotationData().setRotating(true);
	}

	@Override
	public void dispose( ) {

	}

	@Override
	public void onFrameChanged(final Entity character, final TextureAtlas.AtlasRegion newFrame) {
		SpriteType spriteType = ComponentsMapper.characterDecal.get(character).getSpriteType();
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		if (spriteType == SpriteType.RUN) {
			boolean isPlayer = ComponentsMapper.player.has(character);
			if ((newFrame.index == 0 || newFrame.index == 5)) {
				if (isPlayer || ComponentsMapper.enemy.get(character).getEnemyDeclaration().human()) {
					Vector3 position = ComponentsMapper.characterDecal.get(character).getDecal().getPosition();
					Entity entity = systemsCommonData.getMap().getNode(position).getEntity();
					if (entity != null) {
						SurfaceType surfaceType = ComponentsMapper.floor.get(entity).getDefinition().getSurfaceType();
						Sounds stepSound = surfaceTypeToStepSound.get(surfaceType);
						systemsCommonData.getSoundPlayer().playSound(stepSound);
					}
				} else {
					Sounds stepSound = ComponentsMapper.enemy.get(character).getEnemyDeclaration().soundStep();
					systemsCommonData.getSoundPlayer().playSound(stepSound);
				}
			}
		}

		CharacterComponent characterComp = ComponentsMapper.character.get(character);
		Entity turn = systemsCommonData.getTurnsQueue().first();
		Queue<CharacterCommand> commands = characterComp.getCommands();
		if (turn == character && !commands.isEmpty()) {
			CharacterCommand currentCommand = commands.first();
			if (currentCommand.getState() == CommandStates.RUNNING) {
				boolean commandDone = currentCommand.reactToFrameChange(systemsCommonData, character, newFrame, subscribers);
				if (commandDone) {
					currentCommand.setState(CommandStates.ENDED);
				}
			}
		}
	}

	private void rotationDone(CharacterRotationData rotationData,
							  CharacterSpriteData charSpriteData) {
		rotationData.setRotating(false);
		CharacterCommand command = character.get(getSystemsCommonData().getTurnsQueue().first()).getCommands().first();
		charSpriteData.setSpriteType(command.getDefinition().getSpriteType());
	}

	@Override
	public void onProjectileCollisionWithAnotherEntity(final Entity bullet, final Entity collidable) {
		if (character.has(collidable)) {
			ModelInstanceComponent modelInstanceComponent = ComponentsMapper.modelInstance.get(bullet);
			applyDamageToCharacter(collidable, ComponentsMapper.bullet.get(bullet).getDamage(), modelInstanceComponent);
		}
	}
}
