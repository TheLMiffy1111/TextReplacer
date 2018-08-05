package thelm.textreplacer.replacements;

import java.util.Locale;

public enum EnumMatchStrategy {

	FULL,
	START,
	END,
	DIRECT,
	;

	public static EnumMatchStrategy fromString(String str) {
		return EnumMatchStrategy.valueOf(str.toUpperCase(Locale.US));
	}
}
