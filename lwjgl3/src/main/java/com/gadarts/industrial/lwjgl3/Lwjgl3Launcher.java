package com.gadarts.industrial.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.gadarts.industrial.TerrorEffector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Lwjgl3Launcher {

	public static void main(String[] arg) {
		Lwjgl3ApplicationConfiguration config = createGameConfig();
		config.setForegroundFPS(60);
		String versionName = "0.0";
		int versionNumber = 0;
		try {
			InputStream res = Lwjgl3Launcher.class.getClassLoader().getResourceAsStream("version.txt");
			BufferedReader versionFile = new BufferedReader(new InputStreamReader(Objects.requireNonNull(res)));
			String line;
			List<String> lines = new ArrayList<>();
			while ((line = versionFile.readLine()) != null) {
				lines.add(line);
			}
			versionName = lines.get(0);
			versionNumber = Integer.parseInt(lines.get(1));
			res.close();
			versionFile.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		new Lwjgl3Application(new TerrorEffector(versionName, versionNumber), config);
	}

	private static Lwjgl3ApplicationConfiguration createGameConfig( ) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
		config.setResizable(false);
		config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL20, 4, 2);
		return config;
	}
}
