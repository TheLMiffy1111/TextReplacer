package thelm.textreplacer;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.lwjgl.input.Keyboard;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.BossInfoClient;
import net.minecraft.client.gui.GuiBossOverlay;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import thelm.textevents.TextEvents;
import thelm.textevents.events.ClientReceivedTitleEvent;

public class LoggingManager {

	public static KeyBinding keyBindLogNBT = new KeyBinding("key.textreplacer.nbt", Keyboard.KEY_NONE, "key.categories.textreplacer");
	public static KeyBinding keyBindLogChat = new KeyBinding("key.textreplacer.chat", Keyboard.KEY_NONE, "key.categories.textreplacer");
	public static KeyBinding keyBindLogBook = new KeyBinding("key.textreplacer.book", Keyboard.KEY_NONE, "key.categories.textreplacer");
	public static KeyBinding keyBindLogEntity = new KeyBinding("key.textreplacer.entity", Keyboard.KEY_NONE, "key.categories.textreplacer");
	public static KeyBinding keyBindLogSign = new KeyBinding("key.textreplacer.sign", Keyboard.KEY_NONE, "key.categories.textreplacer");
	public static KeyBinding keyBindLogDisplayedSign = new KeyBinding("key.textreplacer.displayedsign", Keyboard.KEY_NONE, "key.categories.textreplacer");
	public static KeyBinding keyBindLogTitle = new KeyBinding("key.textreplacer.title", Keyboard.KEY_NONE, "key.categories.textreplacer");
	public static KeyBinding keyBindLogBoss = new KeyBinding("key.textreplacer.boss", Keyboard.KEY_NONE, "key.categories.textreplacer");
	public static KeyBinding keyBindLogItem = new KeyBinding("key.textreplacer.item", Keyboard.KEY_NONE, "key.categories.textreplacer");
	public static KeyBinding keyBindLogPlayerList = new KeyBinding("key.textreplacer.playerlist", Keyboard.KEY_NONE, "key.categories.textreplacer");

	public static boolean logNBT = false;
	public static boolean logChat = false;
	public static boolean logBook = false;
	public static boolean logSign = false;
	public static boolean logTitle = false;

	public static void register() {
		ClientRegistry.registerKeyBinding(keyBindLogNBT);
		ClientRegistry.registerKeyBinding(keyBindLogChat);
		ClientRegistry.registerKeyBinding(keyBindLogBook);
		ClientRegistry.registerKeyBinding(keyBindLogEntity);
		ClientRegistry.registerKeyBinding(keyBindLogSign);
		ClientRegistry.registerKeyBinding(keyBindLogDisplayedSign);
		ClientRegistry.registerKeyBinding(keyBindLogTitle);
		ClientRegistry.registerKeyBinding(keyBindLogBoss);
		ClientRegistry.registerKeyBinding(keyBindLogItem);
		ClientRegistry.registerKeyBinding(keyBindLogPlayerList);
	}

	public static void handleInput() {
		if(keyBindLogNBT.isPressed()) {
			logNBT = !logNBT;
			Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("textreplacer.lognbt."+logNBT));
		}
		if(keyBindLogChat.isPressed()) {
			logChat = !logChat;
			Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("textreplacer.logchat."+logChat));
		}
		if(keyBindLogBook.isPressed()) {
			logBook = !logBook;
			Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("textreplacer.logbook."+logBook));
		}
		if(keyBindLogEntity.isPressed()) {
			Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("textreplacer.logentity"));
			handleEntity();
		}
		if(keyBindLogSign.isPressed()) {
			Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("textreplacer.logsign"));
			handleSign();
		}
		if(keyBindLogDisplayedSign.isPressed()) {
			logSign = !logSign;
			Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("textreplacer.logdisplayedsign."+logSign));
		}
		if(keyBindLogTitle.isPressed()) {
			logTitle = !logTitle;
			Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("textreplacer.logtitle."+logTitle));
		}
		if(keyBindLogBoss.isPressed()) {
			Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("textreplacer.logboss"));
			handleBoss();
		}
		if(keyBindLogPlayerList.isPressed()) {
			Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("textreplacer.logplayerlist"));
			handlePlayerList();
		}
	}

	public static void handleScreenInput(GuiScreen guiScreen) {
		int keyCode = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
		if(keyCode != 0 && !Keyboard.isRepeatEvent()) {
			if(Keyboard.getEventKeyState()) {
				if(keyBindLogItem.isActiveAndMatches(keyCode)) {
					handleItem(guiScreen);
				}
			}
		}
	}

	public static void handleTick() {
		if(logBook) {
			handleBook();
		}
		if(logSign) {
			handleDisplayedSign();
		}
	}

	public static void handleChat(ClientChatReceivedEvent event) {
		TextEvents.LOGGER.info(
				"[Chat] Component: "+ITextComponent.Serializer.componentToJson(event.getMessage())+
				", Text: "+event.getMessage().getFormattedText()+
				", Type: "+event.getType());
	}

	private static Field bookField;
	private static NBTTagCompound prevBookNBT;

	private static void handleBook() {
		GuiScreen guiScreen = Minecraft.getMinecraft().currentScreen;
		if(guiScreen instanceof GuiScreenBook) {
			try {
				if(bookField == null) {
					bookField = ReflectionHelper.findField(GuiScreenBook.class, "book", "field_146474_h", "h");
				}
				ItemStack book = (ItemStack)bookField.get(guiScreen);
				NBTTagCompound nbt = book.writeToNBT(new NBTTagCompound());
				if(nbt.equals(prevBookNBT)) {
					return;
				}
				prevBookNBT = nbt;
				TextEvents.LOGGER.info(
						"[Book] NBT: "+nbt);
			}
			catch(Exception e) {
				TextEvents.LOGGER.warn("Could not get currently displayed book.", e);
			}
		}
	}

	private static void handleEntity() {
		World world = Minecraft.getMinecraft().world;
		for(Entity entity : world.loadedEntityList) {
			if(entity != null) {
				if(logNBT) {
					NBTTagCompound nbt = entity.writeToNBT(new NBTTagCompound());
					TextEvents.LOGGER.info(
							"[Entity] NBT: "+nbt);
					continue;
				}
				String name = entity.getDisplayName().getFormattedText();
				ResourceLocation key = EntityList.getKey(entity);
				Vec3d pos = entity.getPositionVector();
				TextEvents.LOGGER.info(
						"[Entity] Name: "+name+
						", Key: "+key+
						", Pos: "+pos);
			}
		}
	}

	private static void handleSign() {
		World world = Minecraft.getMinecraft().world;
		for(TileEntity tileEntity : world.loadedTileEntityList) {
			if(tileEntity instanceof TileEntitySign) {
				TileEntitySign sign = (TileEntitySign)tileEntity;
				IBlockState state = world.getBlockState(sign.getPos());
				if(logNBT) {
					NBTTagCompound nbt = sign.writeToNBT(new NBTTagCompound());
					TextEvents.LOGGER.info(
							"[Sign] NBT: "+nbt+
							", State: "+state);
					continue;
				}
				ITextComponent[] text = sign.signText;
				BlockPos pos = sign.getPos();
				TextEvents.LOGGER.info(
						"[Sign] Components: "+Arrays.toString(Stream.of(text).map(ITextComponent.Serializer::componentToJson).toArray())+
						", Text: "+Arrays.toString(Stream.of(text).map(ITextComponent::getFormattedText).toArray())+
						", State: "+state+
						", Pos: "+pos);
			}
		}
	}

	private static Field tileSignField;
	private static NBTTagCompound prevSignNBT;

	private static void handleDisplayedSign() {
		GuiScreen guiScreen = Minecraft.getMinecraft().currentScreen;
		if(guiScreen instanceof GuiEditSign) {
			try {
				if(tileSignField == null) {
					tileSignField = ReflectionHelper.findField(GuiEditSign.class, "tileSign", "field_146848_f", "a");
				}
				TileEntitySign sign = (TileEntitySign)tileSignField.get(guiScreen);
				NBTTagCompound nbt = sign.writeToNBT(new NBTTagCompound());
				if(nbt.equals(prevSignNBT)) {
					return;
				}
				prevSignNBT = nbt;
				if(logNBT) {
					TextEvents.LOGGER.info(
							"[DSign] NBT: "+nbt);
					return;
				}
				ITextComponent[] text = sign.signText;
				TextEvents.LOGGER.info(
						"[DSign] Components: "+Arrays.toString(Stream.of(text).map(ITextComponent.Serializer::componentToJson).toArray())+
						", Text: "+Arrays.toString(Stream.of(text).map(ITextComponent::getFormattedText).toArray()));
			}
			catch(Exception e) {
				TextEvents.LOGGER.warn("Could not get currently displayed sign.", e);
			}
		}
	}

	public static void handleTitle(ClientReceivedTitleEvent event) {
		TextEvents.LOGGER.info(
				"[Title] Text: "+event.getText()+
				", Type: "+event.getType());
	}

	private static Field bossInfoMapField;

	private static void handleBoss() {
		try {
			if(bossInfoMapField == null) {
				bossInfoMapField = ReflectionHelper.findField(GuiBossOverlay.class, "mapBossInfos", "field_184060_g", "g");
			}
			Map<UUID, BossInfoClient> bossInfoMap = (Map<UUID, BossInfoClient>)bossInfoMapField.get(Minecraft.getMinecraft().ingameGUI.getBossOverlay());
			for(BossInfoClient info : bossInfoMap.values()) {
				ITextComponent name = info.getName();
				BossInfo.Color color = info.getColor();
				BossInfo.Overlay overlay = info.getOverlay();
				boolean darkenSky = info.shouldDarkenSky();
				boolean createFog = info.shouldCreateFog();
				TextEvents.LOGGER.info(
						"[Boss] Name: "+ITextComponent.Serializer.componentToJson(name)+
						", Text: "+name.getFormattedText()+
						", Color: "+color+
						", Overlay: "+overlay+
						", DarkSky: "+darkenSky+
						", Fog: "+createFog);
			}
		}
		catch(Exception e) {
			TextEvents.LOGGER.warn("Could not get BossInfo map.", e);
		}
	}

	private static Field hoveredSlotField;

	private static void handleItem(GuiScreen guiScreen) {
		if(guiScreen instanceof GuiContainer) {
			Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("textreplacer.logitem"));
			try {
				if(hoveredSlotField == null) {
					hoveredSlotField = ReflectionHelper.findField(GuiContainer.class, "hoveredSlot", "field_147006_u", "v");
				}
				Slot hoveredSlot = (Slot)hoveredSlotField.get(guiScreen);
				ItemStack stack = hoveredSlot == null ? ItemStack.EMPTY : hoveredSlot.getStack();
				NBTTagCompound nbt = stack.writeToNBT(new NBTTagCompound());
				TextEvents.LOGGER.info(
						"[Item] NBT: "+nbt);
			}
			catch(Exception e) {
				TextEvents.LOGGER.warn("Could not get hovered slot.", e);
			}
		}
	}

	private static void handlePlayerList() {
		NetHandlerPlayClient netHandler = Minecraft.getMinecraft().getConnection();
		if(netHandler != null) {
			for(NetworkPlayerInfo info : netHandler.getPlayerInfoMap()) {
				ITextComponent displayName = info.getDisplayName();
				GameType gameType = info.getGameType();
				String name = info.getGameProfile().getName();
				TextEvents.LOGGER.info(
						"[PlayerList] Display: "+ITextComponent.Serializer.componentToJson(displayName)+
						", Text: "+displayName.getFormattedText()+
						", GameType: "+gameType+
						", Name: "+name);
			}
		}
	}
}
