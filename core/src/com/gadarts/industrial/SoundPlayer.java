package com.gadarts.industrial;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.console.ConsoleEventsSubscriber;
import com.gadarts.industrial.console.commands.ConsoleCommandParameter;
import com.gadarts.industrial.console.commands.ConsoleCommandResult;
import com.gadarts.industrial.console.commands.ConsoleCommands;
import com.gadarts.industrial.console.commands.ConsoleCommandsList;
import com.gadarts.industrial.utils.GeneralUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static com.badlogic.gdx.math.MathUtils.random;
import static com.badlogic.gdx.math.MathUtils.randomBoolean;

public class SoundPlayer implements ConsoleEventsSubscriber {
	private static final float MELODY_VOLUME = 0.4f;
	private static final float PITCH_OFFSET = 0.1f;
	private final GameAssetsManager assetManager;
	private final List<Sound> loopingSounds = new ArrayList<>();
	@Getter
	@Setter
	private boolean sfxEnabled;
	@Getter
	private boolean musicEnabled;

	public SoundPlayer(final GameAssetsManager assetManager) {
		this.assetManager = assetManager;
		setSfxEnabled(DefaultGameSettings.SFX_ENABLED);
		setMusicEnabled(DefaultGameSettings.MELODY_ENABLED);
		playAmbSound();
	}


	public void setMusicEnabled(final boolean musicEnabled) {
		this.musicEnabled = musicEnabled;
		if (musicEnabled) {
			playMusic(Assets.Melody.TEST);
		} else {
			stopMusic(Assets.Melody.TEST);
		}
	}

	public void playMusic(final Assets.Melody melody) {
		if (!isMusicEnabled()) return;
		Music music = assetManager.getMelody(melody);
		music.setVolume(MELODY_VOLUME);
		music.setLooping(true);
		music.play();
	}

	public void stopMusic(final Assets.Melody melody) {
		Music music = assetManager.getMelody(melody);
		music.stop();
	}

	public void playSound(final Assets.Sounds soundDef) {
		playSound(soundDef, 1F);
	}

	public void playSound(final Assets.Sounds def, final float volume) {
		if (!isSfxEnabled()) return;
		float pitch = 1 + (def.isRandomPitch() ? (randomBoolean() ? 1 : -1) : 0) * random(-PITCH_OFFSET, PITCH_OFFSET);
		if (!def.isLoop()) {
			assetManager.getSound(getRandomSound(def)).play(volume, pitch, 0);
		} else {
			Sound sound = assetManager.getSound(getRandomSound(def));
			sound.loop(volume, 1, 0);
			loopingSounds.add(sound);
		}
	}

	private String getRandomSound(final Assets.Sounds soundDef) {
		String filePath = soundDef.getFilePath();
		if (soundDef.getFiles().length > 0) {
			filePath = GeneralUtils.getRandomRoadSound(soundDef);
		}
		return filePath;
	}

	public void stopLoopingSounds( ) {
		loopingSounds.forEach(Sound::stop);
	}

	@Override
	public boolean onCommandRun(ConsoleCommands command, ConsoleCommandResult consoleCommandResult) {
		if (command == ConsoleCommandsList.SFX) {
			applySfxCommand(consoleCommandResult);
			return true;
		} else if (command == ConsoleCommandsList.MELODY) {
			applyMusicCommand(consoleCommandResult);
			return true;
		}
		return false;
	}

	private void logAudioMessage(final ConsoleCommandResult consoleCommandResult,
								 final String label,
								 final boolean sfxEnabled) {
		String msg = sfxEnabled ? String.format("%s enabled.", label) : String.format("%s disabled.", label);
		consoleCommandResult.setMessage(msg);
	}

	private void applyMusicCommand(final ConsoleCommandResult consoleCommandResult) {
		setMusicEnabled(!isMusicEnabled());
		logAudioMessage(consoleCommandResult, "Melodies", isMusicEnabled());
	}

	private void applySfxCommand(final ConsoleCommandResult consoleCommandResult) {
		boolean originalValue = isSfxEnabled();
		setSfxEnabled(!isSfxEnabled());
		if (!isSfxEnabled()) {
			stopLoopingSounds();
		} else if (!originalValue) {
			playAmbSound();
		}
		logAudioMessage(consoleCommandResult, "Sound effects", isSfxEnabled());
	}

	private void playAmbSound( ) {
		playSound(Assets.Sounds.AMB_WIND, 0.5F);
	}

	@Override
	public boolean onCommandRun(ConsoleCommands command, ConsoleCommandResult consoleCommandResult, ConsoleCommandParameter parameter) {
		return ConsoleEventsSubscriber.super.onCommandRun(command, consoleCommandResult, parameter);
	}

	@Override
	public void onConsoleDeactivated( ) {
		ConsoleEventsSubscriber.super.onConsoleDeactivated();
	}
}
