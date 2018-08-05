package thelm.textreplacer;

import java.util.List;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import thelm.textevents.TextEvents;
import thelm.textevents.events.ClientReceivedBossNameEvent;
import thelm.textevents.events.ClientReceivedPlayerNameEvent;
import thelm.textevents.events.ClientReceivedTitleEvent;
import thelm.textevents.events.GetItemDisplayNameEvent;
import thelm.textevents.events.RenderEntityLabelEvent;
import thelm.textevents.events.RenderPageTextInBookEvent;
import thelm.textevents.events.RenderSignTextEvent;
import thelm.textreplacer.replacements.EnumReplacementType;
import thelm.textreplacer.replacements.ReplacementManager;

public class EventHandler implements IResourceManagerReloadListener {

	public static final EventHandler INSTANCE = new EventHandler();

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {
		ReplacementManager.load(resourceManager);
	}

	@SubscribeEvent
	public void onInput(InputEvent event) {
		LoggingManager.handleInput();
	}

	@SubscribeEvent
	public void onGuiScreenKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
		LoggingManager.handleScreenInput(event.getGui());
	}

	@SubscribeEvent
	public void onGuiScreenMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
		LoggingManager.handleScreenInput(event.getGui());
	}

	@SubscribeEvent
	public void onClientTick(ClientTickEvent event) {
		if(event.phase == Phase.END) {
			LoggingManager.handleTick();
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onClientChatReceivedLowest(ClientChatReceivedEvent event) {
		if(LoggingManager.logChat) {
			LoggingManager.handleChat(event);
		}
		ITextComponent component = ReplacementManager.getReplacement(EnumReplacementType.CHAT, event.getMessage(), event.getType());
		event.setMessage(component);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onItemTooltipLowest(ItemTooltipEvent event) {
		List<String> list = event.getToolTip();
		ItemStack stack = event.getItemStack();
		for(int i = 1; i < list.size(); i++) {
			String str = ReplacementManager.getReplacement(EnumReplacementType.TOOLTIP, list.get(i), stack);
			list.set(i, str);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onRenderPageTextInBookLowest(RenderPageTextInBookEvent event) {
		String str = ReplacementManager.getReplacement(EnumReplacementType.BOOK, event.getPage(), event.getStack());
		event.setPage(str);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onRenderEntityLabelLowest(RenderEntityLabelEvent event) {
		String str = ReplacementManager.getReplacement(EnumReplacementType.ENTITY, event.getName(), event.getEntity());
		event.setName(str);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onSignTextLowest(RenderSignTextEvent event) {
		ITextComponent component = ReplacementManager.getReplacement(EnumReplacementType.SIGN, event.getText(), event.getSign());
		event.setText(component);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onClientReceivedTitleLowest(ClientReceivedTitleEvent event) {
		if(LoggingManager.logTitle) {
			LoggingManager.handleTitle(event);
		}
		String str = ReplacementManager.getReplacement(EnumReplacementType.TITLE, event.getText(), event.getType());
		event.setText(str);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onClientReceivedBossNameLowest(ClientReceivedBossNameEvent event) {
		ITextComponent component = ReplacementManager.getReplacement(EnumReplacementType.BOSS, event.getName(), null);
		event.setName(component);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onGetItemDisplayNameLowest(GetItemDisplayNameEvent event) {
		NBTTagCompound nbt = event.getNBT();
		if(nbt.hasKey("Name")) {
			String str = ReplacementManager.getReplacement(EnumReplacementType.ITEM, nbt.getString("Name"), event.getStack());
			nbt.setString("Name", str);
			event.setNBT(nbt);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onClientReceivedPlayerNameLowest(ClientReceivedPlayerNameEvent event) {
		ITextComponent component = ReplacementManager.getReplacement(EnumReplacementType.PLAYER_LIST, event.getName(), event.getData());
		event.setName(component);
	}
}
