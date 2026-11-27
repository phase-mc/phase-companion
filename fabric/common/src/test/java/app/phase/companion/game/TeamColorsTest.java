package app.phase.companion.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The team color reaches the game's team on this game version. The color type differs
 * (ChatFormatting before 26.2, Optional<TeamColor> since), so the result is read by the enum name.
 */
class TeamColorsTest {
	/** A team's text components reach the item registry (hover events), so the game's registries are needed. */
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void namedColorAndNoColor() {
		PlayerTeam team = new PlayerTeam(new Scoreboard(), "ph.a00");
		TeamColors.apply(team, "blue");
		assertEquals("blue", colorName(team));
		TeamColors.apply(team, "dark_purple");
		assertEquals("dark_purple", colorName(team));
		TeamColors.apply(team, null);
		String none = colorName(team);
		assertTrue(none == null || none.equals("reset"), "no color, got " + none);
	}

	private static String colorName(PlayerTeam team) {
		Object color = team.getColor();
		if (color instanceof Optional<?> optional) {
			color = optional.orElse(null);
		}
		return color == null ? null : ((Enum<?>) color).name().toLowerCase();
	}
}
