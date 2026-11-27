package app.phase.companion.game;

import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Team name color. One jar serves 26.1–26.3, and the API changed inside that range:
 * 26.1 takes {@code setColor(ChatFormatting)} ("no color" is {@code RESET}), 26.2+ takes
 * {@code setColor(Optional<TeamColor>)}. 26.x ships unobfuscated, so the names below are the
 * ones the game really has at runtime; the method is looked up once by its parameter type.
 */
final class TeamColors {
	private TeamColors() {}

	/** {@code PlayerTeam.setColor}, {@code null} if neither form was found. */
	private static final Method SET_COLOR;
	/** Color by name: {@code TeamColor.byName} or {@code ChatFormatting.getByName}. */
	private static final Method BY_NAME;
	/** 26.2+: the parameter is {@code Optional<TeamColor>}. */
	private static final boolean OPTIONAL;
	/** 26.1: {@code ChatFormatting.RESET}, the color of a new team. */
	private static final Object RESET;

	static {
		Method setColor = null;
		Method byName = null;
		Object reset = null;
		boolean optional = false;
		try {
			for (Method method : PlayerTeam.class.getMethods()) {
				if (!method.getName().equals("setColor") || method.getParameterCount() != 1) {
					continue;
				}
				Class<?> type = method.getParameterTypes()[0];
				if (type == Optional.class) {
					byName = Class.forName("net.minecraft.world.scores.TeamColor").getMethod("byName", String.class);
					optional = true;
				} else if (type.isEnum()) {
					byName = type.getMethod("getByName", String.class);
					reset = type.getField("RESET").get(null);
				} else {
					continue;
				}
				setColor = method;
				break;
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			setColor = null;
		}
		if (setColor == null) {
			Companion.LOG.warn("Phase Companion: this game version has no known team color API, name colors are off");
		}
		SET_COLOR = setColor;
		BY_NAME = byName;
		OPTIONAL = optional;
		RESET = reset;
	}

	/** {@code name} is the color name from {@code room.json} ({@code blue}) or {@code null} for no color. */
	static void apply(PlayerTeam team, String name) {
		if (SET_COLOR == null) {
			return;
		}
		try {
			Object color = name == null ? null : BY_NAME.invoke(null, name);
			SET_COLOR.invoke(team, OPTIONAL ? Optional.ofNullable(color) : color == null ? RESET : color);
		} catch (ReflectiveOperationException | RuntimeException e) {
			Companion.LOG.warn("Phase Companion: could not set a team color", e);
		}
	}
}
