package thelm.textreplacer.json;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.google.common.collect.Streams;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.JsonContext;
import thelm.textreplacer.replacements.EnumMatchType;
import thelm.textreplacer.replacements.EnumReplacementType;
import thelm.textreplacer.replacements.ReplacementEntry;

public class ReplacementEntryDeserializer implements JsonDeserializer<ReplacementEntry> {

	public static final ReplacementEntryDeserializer INSTANCE = new ReplacementEntryDeserializer();

	@Override
	public ReplacementEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		ReplacementEntry entry = new ReplacementEntry();
		if(json.isJsonObject()) {
			JsonObject jsonObject = json.getAsJsonObject();
			entry.setMatchString(JsonUtils.getString(jsonObject, "match_pattern"));
			if(jsonObject.has("text_component")) {
				if(JsonUtils.isString(jsonObject, "text_component")) {
					ITextComponent component = ITextComponent.Serializer.fromJsonLenient(JsonUtils.getString(jsonObject, "match_type"));
					entry.setTextComponent(component);
				}
				else {
					ITextComponent component = ITextComponent.Serializer.fromJsonLenient(jsonObject.get("text_component").toString());
					entry.setTextComponent(component);
				}
			}
			else {
				String translationKey = JsonUtils.getString(jsonObject, "translation_key");
				entry.setTranslationKey(translationKey);
			}
			if(jsonObject.has("replacement_type")) {
				EnumReplacementType replacementType = EnumReplacementType.fromString(JsonUtils.getString(jsonObject, "replacement_type"));
				entry.setReplacementTypes(Collections.singleton(replacementType));
			}
			else if(jsonObject.has("replacement_types")) {
				List<EnumReplacementType> replacementTypes = Streams.stream(JsonUtils.getJsonArray(jsonObject, "replacement_types")).filter(JsonUtils::isString).map(JsonElement::getAsString).map(EnumReplacementType::fromString).distinct().collect(Collectors.toList());
				entry.setReplacementTypes(replacementTypes);
			}
			if(JsonUtils.isString(jsonObject, "match_type")) {
				EnumMatchType matchType = EnumMatchType.fromString(JsonUtils.getString(jsonObject, "match_type"));
				entry.setMatchType(matchType);
			}
			if(jsonObject.has("ignored_languages")) {
				List<String> ignoredLanguages = Streams.stream(JsonUtils.getJsonArray(jsonObject, "ignored_languages")).filter(JsonUtils::isString).map(JsonElement::getAsString).collect(Collectors.toList());
				entry.setIgnoredLanguages(ignoredLanguages);
			}
			if(jsonObject.has("matchers")) {
				JsonObject jsonObject1 = jsonObject.getAsJsonObject("matchers");
				for(EnumReplacementType replacementType : EnumReplacementType.values()) {
					String name = replacementType.name().toLowerCase(Locale.US);
					if(jsonObject1.has(name)) {
						JsonElement jsonElement = jsonObject1.get(name);
						switch(replacementType) {
						case CHAT: {
							if(JsonUtils.isString(jsonElement)) {
								ChatType type = ChatType.valueOf(jsonElement.getAsString().toUpperCase(Locale.US));
								entry.setExtraMatcher(replacementType, type::equals);
							}
							else {
								JsonObject jsonObject2 = jsonElement.getAsJsonObject();
								if(jsonObject2.has("type")) {
									ChatType type = ChatType.valueOf(JsonUtils.getString(jsonObject2, "type").toUpperCase(Locale.US));
									entry.setExtraMatcher(replacementType, type::equals);
								}
							}
							break;
						}
						case ENTITY: {
							break;
						}
						case ITEM: {
							Ingredient ingredient = CraftingHelper.getIngredient(jsonElement, new JsonContext("minecraft"));
							entry.setExtraMatcher(replacementType, ingredient);
							break;
						}
						case BOOK: {
							Ingredient ingredient = CraftingHelper.getIngredient(jsonElement, new JsonContext("minecraft"));
							entry.setExtraMatcher(replacementType, ingredient);
							break;
						}
						case PLAYER_LIST: {
							break;
						}
						case SIGN: {
							break;
						}
						case TITLE: {
							if(JsonUtils.isString(jsonElement)) {
								SPacketTitle.Type type = SPacketTitle.Type.valueOf(jsonElement.getAsString().toUpperCase(Locale.US));
								entry.setExtraMatcher(replacementType, type::equals);
							}
							else {
								JsonObject jsonObject2 = jsonElement.getAsJsonObject();
								if(jsonObject2.has("type")) {
									SPacketTitle.Type type = SPacketTitle.Type.valueOf(JsonUtils.getString(jsonObject2, "type").toUpperCase(Locale.US));
									entry.setExtraMatcher(replacementType, type::equals);
								}
							}
							break;
						}
						case TOOLTIP:{
							Ingredient ingredient = CraftingHelper.getIngredient(jsonElement, new JsonContext("minecraft"));
							entry.setExtraMatcher(replacementType, ingredient);
							break;
						}
						default:
							break;
						}
					}
				}
			}
			return entry;
		}
		throw new JsonParseException("Unable to parse "+json+" into a ReplacementEntry.");
	}
}
