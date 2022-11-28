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
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.TimeUtils;
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.animation.AnimationComponent;
import com.gadarts.industrial.components.character.*;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import com.gadarts.industrial.shared.model.characters.enemies.WeaponsDefinitions;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.commands.CharacterCommand;
import com.gadarts.industrial.systems.character.commands.CommandStates;
import com.gadarts.industrial.systems.enemy.EnemySystemEventsSubscriber;
import com.gadarts.industrial.systems.player.PlayerSystemEventsSubscriber;
import com.gadarts.industrial.systems.projectiles.AttackSystemEventsSubscriber;
import com.gadarts.industrial.systems.render.RenderSystemEventsSubscriber;
import com.gadarts.industrial.systems.turns.TurnsSystemEventsSubscriber;
import com.gadarts.industrial.utils.EntityBuilder;
import com.gadarts.industrial.utils.GameUtils;

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
	private static final int ROTATION_INTERVAL = 125;
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final Vector3 auxVector3_2 = new Vector3();
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final Vector2 auxVector2_2 = new Vector2();
	private static final Vector2 auxVector2_3 = new Vector2();
	private static final long CHARACTER_PAIN_DURATION = 1000L;
	private static final long MIN_IDLE_ANIMATION_INTERVAL = 2000L;
	private static final Queue<CharacterCommand> auxCommandQueue = new Queue<>();
	private ParticleEffect bloodSplatterEffect;
	private ImmutableArray<Entity> characters;
	private ParticleEffect smallExpEffect;

	public CharacterSystem(GameAssetsManager assetsManager,
						   GameLifeCycleHandler lifeCycleHandler) {
		super(assetsManager, lifeCycleHandler);
	}

	private static void freeEndedCommand(Queue<CharacterCommand> commands) {
		if (commands.isEmpty()) return;
		while (!commands.isEmpty()) {
			CharacterCommand currentCommand = commands.first();
			if (currentCommand.getState() == CommandStates.ENDED) {
				currentCommand.free();
				commands.removeFirst();
			} else {
				break;
			}
		}
	}

	@Override
	public void onCharacterStillHasTime(Entity character) {
		Queue<CharacterCommand> commands = ComponentsMapper.character.get(character).getCommands();
		freeEndedCommand(commands);
		if (!commands.isEmpty()) {
			CharacterCommand command = commands.first();
			if (command.getState() == CommandStates.READY) {
				beginProcessingCommand(character, command);
			}
		}
	}

	@Override
	public void onNewTurn(Entity entity) {
		if (!character.has(entity)) return;

		if (ComponentsMapper.player.has(entity)) {
			freeEndedCommand(character.get(getSystemsCommonData().getPlayer()).getCommands());
			Queue<CharacterCommand> commands = ComponentsMapper.character.get(entity).getCommands();
			if (!commands.isEmpty()) {
				CharacterCommand currentCommand = commands.first();
				if (commands.first().getState() == CommandStates.READY) {
					beginProcessingCommand(entity, currentCommand);
				}
			}
		}
	}

	@Override
	public void onSystemReset(SystemsCommonData systemsCommonData) {
		super.onSystemReset(systemsCommonData);
		characters = getEngine().getEntitiesFor(Family.all(CharacterComponent.class).get());
	}

	@Override
	public Class<CharacterSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return CharacterSystemEventsSubscriber.class;
	}

	@Override
	public void initializeData( ) {
		bloodSplatterEffect = getAssetsManager().getParticleEffect(Assets.ParticleEffects.BLOOD_SPLATTER);
		smallExpEffect = getAssetsManager().getParticleEffect(Assets.ParticleEffects.SMALL_EXP);
	}

	/**
	 * Applies the given commands sequence on the given character.
	 *
	 * @param commands
	 * @param character
	 */
	@SuppressWarnings("JavaDoc")
	public void applyCommands(Queue<CharacterCommand> commands,
							  Entity character) {
		if (commands.isEmpty()) return;
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		characterComponent.setCommands(commands);
		CharacterCommand currentCommand = ComponentsMapper.character.get(character).getCommands().first();
		currentCommand.setState(CommandStates.READY);
		if (characterComponent.getCharacterSpriteData().getSpriteType() != PAIN) {
			beginProcessingCommand(character, currentCommand);
		}
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		if (character.has(getSystemsCommonData().getTurnsQueue().first())) {
			Queue<CharacterCommand> commands = character.get(getSystemsCommonData().getTurnsQueue().first()).getCommands();
			freeEndedCommand(commands);
			if (!commands.isEmpty()) {
				CharacterCommand currentCommand = commands.first();
				handleCurrentCommand(currentCommand);
			}
		}
		updateCharacters();
	}

	private void beginProcessingCommand(Entity character,
										CharacterCommand currentCommand) {
		currentCommand.setState(CommandStates.RUNNING);
		ComponentsMapper.character.get(character).getRotationData().setRotating(true);
		SystemsCommonData data = getSystemsCommonData();
		Object additionalData = currentCommand.getAdditionalData();
		boolean alreadyDone = currentCommand.initialize(character, data, additionalData, getSubscribers());
		subscribers.forEach(s -> s.onCommandInitialized(character, currentCommand));
		if (alreadyDone) {
			commandDone(character);
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
		long lastDamage = characterComponent.getSkills().getHealthData().getLastDamage();
		CharacterSpriteData spriteData = characterComponent.getCharacterSpriteData();
		if (spriteData.getSpriteType() == PAIN && TimeUtils.timeSinceMillis(lastDamage) > CHARACTER_PAIN_DURATION) {
			painDone(character, spriteData);
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
		if (!DebugSettings.GOD_MODE || !ComponentsMapper.player.has(attacked)) {
			characterComponent.dealDamage(damage);
		}
		handleDeath(attacked);
		if (ComponentsMapper.player.has(attacked) || ComponentsMapper.enemy.get(attacked).getEnemyDefinition().isHuman()) {
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

	private void handleDeath(final Entity character) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		CharacterHealthData healthData = characterComponent.getSkills().getHealthData();
		CharacterSoundData soundData = characterComponent.getSoundData();
		if (healthData.getHp() <= 0) {
			characterDies(character, characterComponent, soundData);
		} else {
			characterInPain(character, characterComponent, soundData);
		}
	}

	private void characterInPain(Entity character,
								 CharacterComponent characterComponent,
								 CharacterSoundData soundData) {
		getSystemsCommonData().getSoundPlayer().playSound(soundData.getPainSound());
		characterComponent.getCharacterSpriteData().setSpriteType(PAIN);
		for (CharacterSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onCharacterGotDamage(character);
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
		if (ComponentsMapper.enemy.has(character) && !ComponentsMapper.enemy.get(character).getEnemyDefinition().isHuman()) {
			Decal decal = ComponentsMapper.characterDecal.get(character).getDecal();
			MapGraphNode node = getSystemsCommonData().getMap().getNode(decal.getPosition());
			float height = ComponentsMapper.enemy.get(character).getEnemyDefinition().getHeight();
			Vector3 pos = node.getCenterPosition(auxVector3_1).add(0F, height / 4F, 0F);
			EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
					.addParticleEffectComponent(smallExpEffect, pos)
					.finishAndAddToEngine();
		}
	}

	@Override
	public void onMeleeAttackAppliedOnTarget(Entity character, Entity target, WeaponsDefinitions primaryAttack) {
		MapGraph map = getSystemsCommonData().getMap();
		MapGraphNode srcNode = map.getNode(ComponentsMapper.characterDecal.get(character).getDecal().getPosition());
		MapGraphNode targetNode = map.getNode(ComponentsMapper.characterDecal.get(target).getDecal().getPosition());
		float height;
		height = GameUtils.calculateCharacterHeight(character);
		if (map.isNodesAdjacent(srcNode, targetNode, height / 2F)) {
			applyDamageToCharacter(target, primaryAttack.getDamage());
		}
	}

	@Override
	public void onBulletSetDestroyed(Entity bullet) {
		commandDone(ComponentsMapper.bullet.get(bullet).getOwner());
	}

	public void commandDone(Entity character) {
		commandDone(character, true, true);
	}

	public void commandDone(Entity character, boolean informTurnTimeLeftStatus, boolean setToIdleSprite) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		Queue<CharacterCommand> commands = characterComponent.getCommands();
		if (commands.isEmpty()) return;

		if (setToIdleSprite) {
			characterComponent.getCharacterSpriteData().setSpriteType(SpriteType.IDLE);
		}
		CharacterCommand lastCommand = commands.first();
		lastCommand.setState(CommandStates.ENDED);
		characterComponent.getRotationData().setRotating(false);
		for (CharacterSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onCharacterCommandDone(character, lastCommand);
		}
		if (informTurnTimeLeftStatus) {
			if (characterComponent.getTurnTimeLeft() > 0 && characterComponent.getSkills().getHealthData().getHp() > 0) {
				subscribers.forEach(subscriber -> subscriber.onCharacterStillHasTime(character));
			} else {
				subscribers.forEach(subscriber -> subscriber.onCharacterFinishedTurn(character));
			}
		}
	}

	private void painDone(Entity character, CharacterSpriteData spriteData) {
		spriteData.setSpriteType(IDLE);
		Entity currentTurn = getSystemsCommonData().getTurnsQueue().first();
		if (ComponentsMapper.character.has(currentTurn)) {
			Queue<CharacterCommand> commands = ComponentsMapper.character.get(currentTurn).getCommands();
			if (!commands.isEmpty()) {
				CharacterCommand command = commands.first();
				if (command.getState() == CommandStates.READY) {
					beginProcessingCommand(character, command);
				}
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
		} else if (onGoingAttack.getType() != null) {
			SystemsCommonData commonData = getSystemsCommonData();
			int primaryAttackHitFrameIndex = GameUtils.getPrimaryAttackHitFrameIndexForCharacter(character, commonData);
			if (onGoingAttack.isDone()) {
				onGoingAttack.setType(null);
				if (!characterComp.getPrimaryAttack().isMelee()) {
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
			commandDone(character);
		}
	}


	private void handleAnimationReverse(Entity character, AnimationComponent animationComponent,
										Animation<TextureAtlas.AtlasRegion> animation,
										SpriteType spriteType) {
		if (animationComponent.getAnimation().getPlayMode() == Animation.PlayMode.REVERSED) {
			if (spriteType.isCommandDoneOnReverseEnd()) {
				commandDone(character);
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
		animationComponent.getAnimation().getPlayMode();
	}

	private void handleCurrentCommand(final CharacterCommand currentCommand) {
		Entity character = currentCommand.getCharacter();
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		SpriteType spriteType = characterComponent.getCharacterSpriteData().getSpriteType();
		if (spriteType == PICKUP || spriteType == ATTACK_PRIMARY) {
			handleModeWithNonLoopingAnimation(character);
		} else {
			handleRotation(currentCommand, character);
		}
	}

	private Direction calculateDirectionToDestination(CharacterCommand currentCommand) {
		Entity character = currentCommand.getCharacter();
		Vector3 characterPos = auxVector3_1.set(ComponentsMapper.characterDecal.get(character).getDecal().getPosition());
		Vector2 destPos = currentCommand.getNextNode().getCenterPosition(auxVector2_2);
		Vector2 directionToDest = destPos.sub(characterPos.x, characterPos.z).nor();
		return findDirection(directionToDest);
	}


	private void handleRotation(CharacterCommand currentCommand,
								Entity character) {
		CharacterComponent charComp = ComponentsMapper.character.get(character);
		if (charComp.getCharacterSpriteData().getSpriteType() == PAIN) return;

		CharacterRotationData rotData = charComp.getRotationData();
		if (!currentCommand.getDefinition().isRotationForbidden()) {
			if (rotData.isRotating() && TimeUtils.timeSinceMillis(rotData.getLastRotation()) > ROTATION_INTERVAL) {
				for (CharacterSystemEventsSubscriber subscriber : subscribers) {
					subscriber.onCharacterRotated(currentCommand.getCharacter());
				}
				rotData.setLastRotation(TimeUtils.millis());
				Direction directionToDest = calculateDirectionToDestination(currentCommand);
				rotate(charComp, rotData, directionToDest, currentCommand.getCharacter());
			}
		} else {
			Vector3 targetPosition = ComponentsMapper.characterDecal.get(charComp.getTarget()).getDecal().getPosition();
			Vector3 characterPosition = ComponentsMapper.characterDecal.get(character).getDecal().getPosition();
			Vector3 direction = auxVector3_1.set(targetPosition).sub(characterPosition).nor();
			charComp.getCharacterSpriteData().setFacingDirection(findDirection(auxVector2_1.set(direction.x, direction.z)));
			rotationDone(rotData, charComp.getCharacterSpriteData());
		}
	}

	private void rotate(CharacterComponent charComponent,
						CharacterRotationData rotationData,
						Direction directionToDest,
						Entity character) {
		if (charComponent.getCharacterSpriteData().getFacingDirection() != directionToDest) {
			CharacterSpriteData characterSpriteData = charComponent.getCharacterSpriteData();
			Vector2 currentDirVec = characterSpriteData.getFacingDirection().getDirection(auxVector2_1);
			float diff = directionToDest.getDirection(auxVector2_2).angleDeg() - currentDirVec.angleDeg();
			int side = auxVector2_3.set(1, 0).setAngleDeg(diff).angleDeg() > 180 ? -1 : 1;
			Vector3 charPos = ComponentsMapper.characterDecal.get(character).getDecal().getPosition();
			Entity floorEntity = getSystemsCommonData().getMap().getNode(charPos).getEntity();
			int fogOfWarSig = floorEntity != null ? ComponentsMapper.floor.get(floorEntity).getFogOfWarSignature() : 0;
			boolean isNodeHidden = (fogOfWarSig & 16) == 16;
			Direction newDir = isNodeHidden ? directionToDest : findDirection(currentDirVec.rotateDeg(45f * side));
			characterSpriteData.setFacingDirection(newDir);
		} else {
			rotationDone(rotationData, charComponent.getCharacterSpriteData());
		}
	}

	@Override
	public void onCharacterNodeChanged(Entity entity, MapGraphNode oldNode, MapGraphNode newNode) {
		character.get(entity).getRotationData().setRotating(true);
	}

	@Override
	public void dispose( ) {

	}

	@Override
	public void onPlayerPathCreated(MapGraphNode destination) {

	}

	@Override
	public void onEnemyAppliedCommand(CharacterCommand command, Entity enemy) {
		auxCommandQueue.clear();
		auxCommandQueue.addLast(command);
		onCharacterAppliedCommand(auxCommandQueue, enemy);
	}

	@Override
	public void onPlayerAppliedCommand(Queue<CharacterCommand> commands) {
		onCharacterAppliedCommand(commands, getSystemsCommonData().getPlayer());
	}

	@Override
	public void onFrameChanged(final Entity character, final float deltaTime, final TextureAtlas.AtlasRegion newFrame) {
		SystemsCommonData commonData = getSystemsCommonData();
		CharacterComponent characterComp = ComponentsMapper.character.get(character);
		Entity turn = commonData.getTurnsQueue().first();
		Queue<CharacterCommand> commands = characterComp.getCommands();
		if (turn == character && !commands.isEmpty()) {
			CharacterCommand currentCommand = commands.first();
			if (currentCommand.getState() == CommandStates.RUNNING) {
				if (currentCommand.reactToFrameChange(commonData, character, newFrame, subscribers)) {
					commandDone(character);
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

	private void onCharacterAppliedCommand(Queue<CharacterCommand> commands, Entity character) {
		applyCommands(commands, character);
	}

	@Override
	public void onProjectileCollisionWithAnotherEntity(final Entity bullet, final Entity collidable) {
		if (character.has(collidable)) {
			ModelInstanceComponent modelInstanceComponent = ComponentsMapper.modelInstance.get(bullet);
			applyDamageToCharacter(collidable, ComponentsMapper.bullet.get(bullet).getDamage(), modelInstanceComponent);
		}
	}
}
