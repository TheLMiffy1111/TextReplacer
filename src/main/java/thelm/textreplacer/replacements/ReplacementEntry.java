package thelm.textreplacer.replacements;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonSyntaxException;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class ReplacementEntry {

	public static final Pattern PATTERN = Pattern.compile("\u2e28.*?\u2e29");
	public static final Pattern STARTS_WITH = Pattern.compile("\u2e28.*?\u2e29.*");
	public static final Pattern ENDS_WITH = Pattern.compile(".*\u2e28.*?\u2e29");

	private EnumSet<EnumReplacementType> replacementTypes = EnumSet.allOf(EnumReplacementType.class);
	private EnumMatchType matchType = EnumMatchType.ALL;
	private EnumMatchStrategy matchStrategy;
	private String matchString;
	private Pattern matchPattern;
	private Pattern[] patterns = {};
	private EnumMap<EnumReplacementType, Predicate<?>> extraMatchers = new EnumMap<>(EnumReplacementType.class);
	private TreeSet<String> ignoredLanguages = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	private String translationKey;
	private ITextComponent textComponent;

	public ImmutableSet<EnumReplacementType> getReplacementTypes() {
		return ImmutableSet.copyOf(this.replacementTypes);
	}

	public void setReplacementTypes(Collection<EnumReplacementType> replacementTypes) {
		this.replacementTypes.clear();
		this.replacementTypes.addAll(replacementTypes);
	}

	public EnumMatchType getMatchType() {
		return this.matchType;
	}

	public void setMatchType(EnumMatchType matchType) {
		this.matchType = matchType;
	}

	public EnumMatchStrategy getMatchStrategy() {
		return this.matchStrategy;
	}

	public String getMatchString() {
		return this.matchString;
	}

	public void setMatchString(String matchString) {
		this.matchString = matchString;
		parseMatchString();
	}

	public String getTranslationKey() {
		return this.translationKey;
	}

	public void setTranslationKey(String translationKey) {
		this.translationKey = translationKey;
	}

	public ITextComponent getTextComponent() {
		return this.textComponent;
	}

	public void setTextComponent(ITextComponent textComponent) {
		this.textComponent = textComponent;
	}

	public Pattern[] getPatterns() {
		return this.patterns.clone();
	}
	
	public ImmutableMap<EnumReplacementType, Predicate<?>> getExtraMatchers() {
		return ImmutableMap.copyOf(this.extraMatchers);
	}

	public void setExtraMatcher(EnumReplacementType replacementType, Predicate<?> matcher) {
		if(matcher == null) {
			this.extraMatchers.remove(replacementType);
		}
		else {
			this.extraMatchers.put(replacementType, matcher);
		}
	}
	
	public ImmutableSet<String> getIgnoredLanguages() {
		return ImmutableSet.copyOf(this.ignoredLanguages);
	}
	
	public void setIgnoredLanguages(Collection<String> ignoredLanguages) {
		this.ignoredLanguages.clear();
		this.ignoredLanguages.addAll(ignoredLanguages);
	}

	private void parseMatchString() {
		String[] strings = PATTERN.split(this.matchString, Integer.MAX_VALUE);
		Pattern[] patterns = new Pattern[strings.length*2 - 1];
		for(int i = 0; i < strings.length; i++) {
			patterns[i*2] = Pattern.compile(Pattern.quote(strings[i]));
		}
		if(strings.length > 1) {
			Matcher matcher = PATTERN.matcher(this.matchString);
			int i = 1;
			while(matcher.find()) {
				String pattern = matcher.group();
				patterns[i] = Pattern.compile(pattern.substring(1, pattern.length()-1));
				i += 2;
			}
			boolean startsWith = STARTS_WITH.matcher(this.matchString).matches();
			boolean endsWith = ENDS_WITH.matcher(this.matchString).matches();
			if(!startsWith) {
				this.matchStrategy = EnumMatchStrategy.START;
			}
			else if(!endsWith) {
				this.matchStrategy = EnumMatchStrategy.END;
			}
			else {
				this.matchStrategy = EnumMatchStrategy.DIRECT;
			}
		}
		else {
			this.matchStrategy = EnumMatchStrategy.FULL;
		}
		this.patterns = patterns;
		StringBuilder matchPatternBuilder = new StringBuilder();
		for(int i = 0; i < patterns.length; i++) {
			matchPatternBuilder.append(patterns[i].pattern());
		}
		this.matchPattern = Pattern.compile(matchPatternBuilder.toString());
	}

	public <T> boolean accepts(EnumReplacementType type, ITextComponent component, T extra) {
		if(!this.ignoredLanguages.contains(ReplacementManager.currentLanguage) &&
				this.matchType != EnumMatchType.RAW_TEXT_ONLY &&
				((Predicate<T>)extraMatchers.getOrDefault(type, Predicates.<T>alwaysTrue())).test(extra)) {
			String str = ITextComponent.Serializer.componentToJson(component);
			return this.matchPattern.matcher(str).matches();
		}
		return false;
	}

	public <T> boolean accepts(EnumReplacementType type, String str, boolean isComponent, T extra) {
		if(!this.ignoredLanguages.contains(ReplacementManager.currentLanguage) &&
				(isComponent && this.matchType != EnumMatchType.RAW_TEXT_ONLY ||
				!isComponent && this.matchType != EnumMatchType.COMPONENT_TEXT && this.matchType != EnumMatchType.COMPONENT_JSON) &&
				((Predicate<T>)extraMatchers.getOrDefault(type, Predicates.<T>alwaysTrue())).test(extra)) {
			return this.matchPattern.matcher(str).matches();
		}
		return false;
	}

	public <T> boolean accepts(EnumReplacementType type, boolean isComponent, T extra) {
		return !this.ignoredLanguages.contains(ReplacementManager.currentLanguage) &&
				(isComponent && this.matchType != EnumMatchType.RAW_TEXT_ONLY ||
				!isComponent && this.matchType != EnumMatchType.COMPONENT_TEXT && this.matchType != EnumMatchType.COMPONENT_JSON) &&
				((Predicate<T>)extraMatchers.getOrDefault(type, Predicates.<T>alwaysTrue())).test(extra);
	}

	public ITextComponent processComponent(ITextComponent component) {
		if(this.matchStrategy == EnumMatchStrategy.FULL) {
			if(this.textComponent != null) {
				return this.textComponent;
			}
			return new TextComponentString(I18n.format(this.translationKey));
		}
		String str = ITextComponent.Serializer.componentToJson(component);
		Object[] params = new String[this.patterns.length/2];
		int offset = 0;
		for(int i = 0; i < this.patterns.length; i++) {
			Matcher matcher = this.patterns[i].matcher(str);
			matcher.find(offset);
			offset = matcher.end();
			if(i%2 == 1) {
				params[i/2] = matcher.group();
			}
		}
		if(this.textComponent != null) {
			try {
				return ITextComponent.Serializer.fromJsonLenient(
						String.format(ITextComponent.Serializer.componentToJson(this.textComponent), params));
			}
			catch(JsonSyntaxException e) {
				return this.textComponent;
			}
		}
		try {
			return ITextComponent.Serializer.fromJsonLenient(I18n.format(this.translationKey, params));
		}
		catch(JsonSyntaxException e) {
			return new TextComponentString(I18n.format(this.translationKey, params));
		}
	}

	public ITextComponent processComponent(String str) {
		if(this.matchStrategy == EnumMatchStrategy.FULL) {
			if(this.textComponent != null) {
				return this.textComponent;
			}
			return new TextComponentString(I18n.format(this.translationKey));
		}
		Object[] params = new String[this.patterns.length/2];
		int offset = 0;
		for(int i = 0; i < this.patterns.length; i++) {
			Matcher matcher = this.patterns[i].matcher(str);
			matcher.find(offset);
			offset = matcher.end();
			if(i%2 == 1) {
				params[i/2] = matcher.group();
			}
		}
		if(this.textComponent != null) {
			try {
				return ITextComponent.Serializer.fromJsonLenient(
						String.format(ITextComponent.Serializer.componentToJson(this.textComponent), params));
			}
			catch(JsonSyntaxException e) {
				return this.textComponent;
			}
		}
		return new TextComponentString(I18n.format(this.translationKey, params));
	}

	public String processString(String str) {
		if(this.matchStrategy == EnumMatchStrategy.FULL) {
			if(this.textComponent != null) {
				return this.textComponent.getFormattedText();
			}
			return I18n.format(this.translationKey);
		}
		Object[] params = new String[this.patterns.length/2];
		int offset = 0;
		for(int i = 0; i < this.patterns.length; i++) {
			Matcher matcher = this.patterns[i].matcher(str);
			matcher.find(offset);
			offset = matcher.end();
			if(i%2 == 1) {
				params[i/2] = matcher.group();
			}
		}
		if(this.textComponent != null) {
			try {
				return ITextComponent.Serializer.fromJsonLenient(
						String.format(ITextComponent.Serializer.componentToJson(this.textComponent), params)).getFormattedText();
			}
			catch(JsonSyntaxException e) {
				return this.textComponent.getFormattedText();
			}
		}
		return I18n.format(this.translationKey, params);
	}
}
