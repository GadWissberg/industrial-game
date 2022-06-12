package com.gadarts.industrial;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DesktopLauncher {
	public static void main(String[] arg) {
		Lwjgl3ApplicationConfiguration config = createGameConfig();
		config.setForegroundFPS(60);
		String versionName = "0.0";
		int versionNumber = 0;
		try {
			InputStream res = DesktopLauncher.class.getClassLoader().getResourceAsStream("version.txt");
			BufferedReader versionFile = new BufferedReader(new InputStreamReader(Objects.requireNonNull(res)));
			String line;
			List<String> lines = new ArrayList<>();
			while ((line = versionFile.readLine()) != null) {
				lines.add(line);
			}
			versionName = lines.get(0);
			versionNumber = Integer.parseInt(lines.get(1));
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		new Lwjgl3Application(new Industrial(versionName, versionNumber), config);
	}

	private static Lwjgl3ApplicationConfiguration createGameConfig( ) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
		config.setResizable(false);
		config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 4, 2);
		return config;
	}
}
