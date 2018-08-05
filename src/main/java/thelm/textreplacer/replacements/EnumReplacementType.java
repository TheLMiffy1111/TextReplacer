package thelm.textreplacer.replacements;

import java.util.Locale;

public enum EnumReplacementType {

	CHAT,
	TOOLTIP,
	BOOK,
	ENTITY,
	SIGN,
	TITLE,
	BOSS,
	ITEM,
	PLAYER_LIST,
	//SCOREBOARD,
	;

	public static EnumReplacementType fromString(String str) {
		return EnumReplacementType.valueOf(str.toUpperCase(Locale.US));
	}
}
