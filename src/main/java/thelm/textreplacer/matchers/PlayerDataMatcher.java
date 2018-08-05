package thelm.textreplacer.matchers;

import java.util.function.Predicate;

import net.minecraft.network.play.server.SPacketPlayerListItem;

public class PlayerDataMatcher implements Predicate<SPacketPlayerListItem.AddPlayerData> {

	@Override
	public boolean test(SPacketPlayerListItem.AddPlayerData data) {
		return true;
	}
}
