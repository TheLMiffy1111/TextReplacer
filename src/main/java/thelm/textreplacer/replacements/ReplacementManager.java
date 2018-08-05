package thelm.textreplacer.replacements;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeBasedTable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import thelm.textreplacer.TextReplacer;
import thelm.textreplacer.json.ReplacementEntryDeserializer;

public class ReplacementManager {

	private static final HashBasedTable<EnumReplacementType, String, ReplacementEntry> FULL_COMPONENT_TABLE = HashBasedTable.create();
	private static final TreeBasedTable<EnumReplacementType, String, ReplacementEntry> START_COMPONENT_TABLE = TreeBasedTable.create();
	private static final TreeBasedTable<EnumReplacementType, String, ReplacementEntry> END_COMPONENT_TABLE = TreeBasedTable.create(Ordering.natural(), (s1, s2)->StringUtils.compare(StringUtils.reverse(s1), StringUtils.reverse(s2)));
	private static final ArrayListMultimap<EnumReplacementType, ReplacementEntry> DIRECT_COMPONENT_MULTIMAP = ArrayListMultimap.create();

	private static final HashBasedTable<EnumReplacementType, String, ReplacementEntry> FULL_STRING_TABLE = HashBasedTable.create();
	private static final TreeBasedTable<EnumReplacementType, String, ReplacementEntry> START_STRING_TABLE = TreeBasedTable.create();
	private static final TreeBasedTable<EnumReplacementType, String, ReplacementEntry> END_STRING_TABLE = TreeBasedTable.create(Ordering.natural(), (s1, s2)->StringUtils.compare(StringUtils.reverse(s1), StringUtils.reverse(s2)));
	private static final ArrayListMultimap<EnumReplacementType, ReplacementEntry> DIRECT_STRING_MULTIMAP = ArrayListMultimap.create();

	public static final String BASE_LOCATION = "textreplacer:replacements/";
	public static final String INDEX_LOCATION = "textreplacer:replacements/index.properties";

	public static final Type ENTRY_LIST_TYPE = new TypeToken<List<ReplacementEntry>>() {}.getType();
	public static final Gson GSON = new GsonBuilder().registerTypeAdapter(ReplacementEntry.class, ReplacementEntryDeserializer.INSTANCE).create();

	public static boolean loaded = false;
	public static String currentLanguage = "en_us";
	
	public static void load(IResourceManager resourceManager) {
		TextReplacer.LOGGER.info("Updating language.");
		currentLanguage = Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode();
		TextReplacer.LOGGER.info("Clearing replacements.");
		FULL_COMPONENT_TABLE.clear();
		START_COMPONENT_TABLE.clear();
		END_COMPONENT_TABLE.clear();
		DIRECT_COMPONENT_MULTIMAP.clear();
		FULL_STRING_TABLE.clear();
		START_STRING_TABLE.clear();
		END_STRING_TABLE.clear();
		DIRECT_STRING_MULTIMAP.clear();
		TextReplacer.LOGGER.info("Begin loading replacements.");
		TreeSet<String> locations = new TreeSet<>();
		try {
			for(IResource resource : resourceManager.getAllResources(new ResourceLocation(INDEX_LOCATION))) {
				try {
					InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(IOUtils.toByteArray(resource.getInputStream())), StandardCharsets.UTF_8);
					Properties props = new Properties();
					props.load(reader);
					for(Object obj : props.keySet()) {
						String s = (String)obj;
						if(s.endsWith(".json")) {
							if(s.startsWith("/")) {
								s = s.substring(1);
							}
							locations.add(s);
						}
					}
				}
				catch(Exception e) {
					TextReplacer.LOGGER.warn(INDEX_LOCATION+" in "+resource.getResourcePackName()+" could not be read.", e);
				}
			}
		}
		catch(Exception e) {
			TextReplacer.LOGGER.info(INDEX_LOCATION+" could not be read.", e);
			return;
		}
		if(locations.isEmpty()) {
			TextReplacer.LOGGER.info(INDEX_LOCATION+" did not specify any replacement locations.");
			return;
		}
		int count = 0;
		for(String location : locations) {
			try {
				IResource resource = resourceManager.getResource(new ResourceLocation(BASE_LOCATION+location));
				try {
					InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(IOUtils.toByteArray(resource.getInputStream())), StandardCharsets.UTF_8);
					List<ReplacementEntry> entries = GSON.fromJson(reader, ENTRY_LIST_TYPE);
					for(ReplacementEntry entry : entries) {
						for(EnumReplacementType replacementType : entry.getReplacementTypes()) {
							if(entry.getMatchType() == EnumMatchType.COMPONENT_JSON) {
								switch(entry.getMatchStrategy()) {
								case FULL:
									FULL_COMPONENT_TABLE.put(replacementType, entry.getMatchString(), entry);
									break;
								case START:
									START_COMPONENT_TABLE.put(replacementType, entry.getMatchString(), entry);
									break;
								case END:
									END_COMPONENT_TABLE.put(replacementType, entry.getMatchString(), entry);
									break;
								case DIRECT:
									DIRECT_COMPONENT_MULTIMAP.put(replacementType, entry);
									break;
								default:
									break;
								}
							}
							else {
								switch(entry.getMatchStrategy()) {
								case FULL:
									FULL_STRING_TABLE.put(replacementType, entry.getMatchString(), entry);
									break;
								case START:
									START_STRING_TABLE.put(replacementType, entry.getMatchString(), entry);
									break;
								case END:
									END_STRING_TABLE.put(replacementType, entry.getMatchString(), entry);
									break;
								case DIRECT:
									DIRECT_STRING_MULTIMAP.put(replacementType, entry);
									break;
								default:
									break;
								}
							}
						}
						count++;
					}
				}
				catch(Exception e) {
					TextReplacer.LOGGER.warn(BASE_LOCATION+location+" in "+resource.getResourcePackName()+" could not be read.", e);
				}
			}
			catch(Exception e) {
				TextReplacer.LOGGER.warn(BASE_LOCATION+location+" could not be read.", e);
			}
		}
		TextReplacer.LOGGER.info("Loaded "+count+" entries.");
		loaded = true;
	}

	public static <T> ITextComponent getReplacement(EnumReplacementType replacementType, ITextComponent component, T extra) {
		if(!loaded || component == null) {
			return component;
		}
		String s, s1, s2;
		char c;
		s = ITextComponent.Serializer.componentToJson(component);
		if(s.isEmpty()) {
			return component;
		}
		if(FULL_COMPONENT_TABLE.contains(replacementType, s)) {
			ReplacementEntry entry = FULL_COMPONENT_TABLE.get(replacementType, s);
			if(entry.accepts(replacementType, true, extra)) {
				return entry.processComponent(component);
			}
		}
		c = s.charAt(0);
		s1 = Character.toString(c);
		s2 = Character.toString((char)(c+1));
		for(ReplacementEntry entry : START_COMPONENT_TABLE.row(replacementType).subMap(s1, s2).values()) {
			if(entry.accepts(replacementType, component, extra)) {
				return entry.processComponent(component);
			}
		}
		c = s.charAt(s.length()-1);
		s1 = Character.toString(c);
		s2 = Character.toString((char)(c+1));
		for(ReplacementEntry entry : END_COMPONENT_TABLE.row(replacementType).subMap(s1, s2).values()) {
			if(entry.accepts(replacementType, component, extra)) {
				return entry.processComponent(component);
			}
		}
		for(ReplacementEntry entry : DIRECT_COMPONENT_MULTIMAP.get(replacementType)) {
			if(entry.accepts(replacementType, component, extra)) {
				return entry.processComponent(component);
			}
		}
		s = component.getFormattedText();
		if(s.isEmpty()) {
			return component;
		}
		if(FULL_STRING_TABLE.contains(replacementType, s)) {
			ReplacementEntry entry = FULL_STRING_TABLE.get(replacementType, s);
			if(entry.accepts(replacementType, true, extra)) {
				return entry.processComponent(s);
			}
		}
		c = s.charAt(0);
		s1 = Character.toString(c);
		s2 = Character.toString((char)(c+1));
		for(ReplacementEntry entry : START_STRING_TABLE.row(replacementType).subMap(s1, s2).values()) {
			if(entry.accepts(replacementType, s, true, extra)) {
				return entry.processComponent(s);
			}
		}
		c = s.charAt(s.length()-1);
		s1 = Character.toString(c);
		s2 = Character.toString((char)(c+1));
		for(ReplacementEntry entry : END_STRING_TABLE.row(replacementType).subMap(s1, s2).values()) {
			if(entry.accepts(replacementType, s, true, extra)) {
				return entry.processComponent(s);
			}
		}
		for(ReplacementEntry entry : DIRECT_STRING_MULTIMAP.get(replacementType)) {
			if(entry.accepts(replacementType, s, true, extra)) {
				return entry.processComponent(s);
			}
		}
		return component;
	}

	public static <T> String getReplacement(EnumReplacementType replacementType, String str, T extra) {
		if(!loaded || StringUtils.isEmpty(str)) {
			return str;
		}
		String s1, s2;
		char c;
		if(FULL_STRING_TABLE.contains(replacementType, str)) {
			ReplacementEntry entry = FULL_STRING_TABLE.get(replacementType, str);
			if(entry.accepts(replacementType, false, extra)) {
				return entry.processString(str);
			}
		}
		c = str.charAt(0);
		s1 = Character.toString(c);
		s2 = Character.toString((char)(c+1));
		for(ReplacementEntry entry : START_STRING_TABLE.row(replacementType).subMap(s1, s2).values()) {
			if(entry.accepts(replacementType, str, false, extra)) {
				return entry.processString(str);
			}
		}
		c = str.charAt(str.length()-1);
		s1 = Character.toString(c);
		s2 = Character.toString((char)(c+1));
		for(ReplacementEntry entry : END_STRING_TABLE.row(replacementType).subMap(s1, s2).values()) {
			if(entry.accepts(replacementType, str, false, extra)) {
				return entry.processString(str);
			}
		}
		for(ReplacementEntry entry : DIRECT_STRING_MULTIMAP.get(replacementType)) {
			if(entry.accepts(replacementType, str, false, extra)) {
				return entry.processString(str);
			}
		}
		return str;
	}
}
