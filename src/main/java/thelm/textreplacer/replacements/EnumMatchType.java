package thelm.textreplacer.replacements;

import java.util.Locale;

public enum EnumMatchType {

	ALL(true, true),
	RAW_TEXT_ONLY(true, false),
	COMPONENT_TEXT(false, true),
	COMPONENT_JSON(false, true),
	;

	public final boolean acceptsString;
	public final boolean acceptsComponent;

	EnumMatchType(boolean acceptsString, boolean acceptsComponent) {
		this.acceptsString = acceptsString;
		this.acceptsComponent = acceptsComponent;
	}

	public boolean acceptsString() {
		return this.acceptsString;
	}

	public boolean acceptsComponent() {
		return this.acceptsComponent;
	}

	public static EnumMatchType fromString(String str) {
		return EnumMatchType.valueOf(str.toUpperCase(Locale.US));
	}
}
