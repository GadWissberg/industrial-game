package com.gadarts.industrial.console;

import com.badlogic.gdx.utils.TimeUtils;
import lombok.Getter;

import java.text.SimpleDateFormat;
import java.util.Date;

@Getter
public class ConsoleTextTimeData {
	private final SimpleDateFormat date = new SimpleDateFormat("HH:mm:ss");
	private final Date timeStamp = new Date(TimeUtils.millis());

}
