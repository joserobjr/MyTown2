package mytown.handlers;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import mytown.MyTown;
import mytown.core.ChatUtils;
import mytown.core.utils.Log;
import mytown.datasource.MyTownDatasource;
import mytown.datasource.MyTownUniverse;
import mytown.entities.BlockWhitelist;
import mytown.entities.Plot;
import mytown.entities.Resident;
import mytown.entities.Town;
import mytown.proxies.DatasourceProxy;
import mytown.proxies.LocalizationProxy;
import mytown.util.Constants;
import mytown.util.Formatter;
import mytown.util.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.UseHoeEvent;
import net.minecraftforge.event.world.BlockEvent;

/**
 * @author Joe Goett
 */
public class PlayerTracker {
    private static Log log = MyTown.instance.log.createChild("PlayerTracker");

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent ev) {
        MyTownDatasource ds = DatasourceProxy.getDatasource();
        Resident res = ds.getOrMakeResident(ev.player);
        if (res != null) {
            res.setPlayer(ev.player);
        } else {
            log.warn("Didn't create resident for player %s (%s)", ev.player.getCommandSenderName(), ev.player.getPersistentID());
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent ev) {
        MyTownDatasource ds = DatasourceProxy.getDatasource();
        Resident res = ds.getOrMakeResident(ev.player);
        if (res != null) {
            res.setPlayer(ev.player);
        }

    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent ev) {
        Resident res = DatasourceProxy.getDatasource().getOrMakeResident(ev.player);
        res.checkLocationOnDimensionChanged(ev.player.chunkCoordX, ev.player.chunkCoordZ, ev.toDim);
    }

    @SubscribeEvent
    public void onEnterChunk(EntityEvent.EnteringChunk ev) {
        if (!(ev.entity instanceof EntityPlayer))
            return;
        checkLocationAndSendMap(ev);
    }

    private void checkLocationAndSendMap(EntityEvent.EnteringChunk ev) {
        if (ev.entity instanceof FakePlayer || ev.entity.worldObj.isRemote)
            return;
        Resident res = DatasourceProxy.getDatasource().getOrMakeResident(ev.entity);
        if (res == null) return; // TODO Log?
        // TODO Check Resident location

        res.checkLocation(ev.oldChunkX, ev.oldChunkZ, ev.newChunkX, ev.newChunkZ, ev.entity.dimension);

        if (res.isMapOn()) {
            Formatter.sendMap(res);
        }
    }

    // Because I can
    @SubscribeEvent
    public void onUseHoe(UseHoeEvent ev) {
        if (ev.current.getDisplayName().equals(Constants.EDIT_TOOL_NAME)) {
            ev.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onItemUse(PlayerInteractEvent ev) {
        if (ev.entityPlayer.worldObj.isRemote)
            return;

        ItemStack currentStack = ev.entityPlayer.inventory.getCurrentItem();
        if (currentStack == null)
            return;

        if ((ev.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || ev.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) && ev.entityPlayer.isSneaking()) {
            if (currentStack.getItem().equals(Items.wooden_hoe) && currentStack.getDisplayName().equals(Constants.EDIT_TOOL_NAME)) {
                // On shift right-click change MODE of the selector tool
                NBTTagList lore = currentStack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8);
                String description = lore.getStringTagAt(0);
                if (description.equals(Constants.EDIT_TOOL_DESCRIPTION_BLOCK_WHITELIST)) {
                    String mode = lore.getStringTagAt(1);
                    if (mode.equals(Constants.EDIT_TOOL_DESCRIPTION_BLOCK_MODE_PLOT))
                        mode = Constants.EDIT_TOOL_DESCRIPTION_BLOCK_MODE_TOWN;
                    else {
                        // TODO: check for permission
                        mode = Constants.EDIT_TOOL_DESCRIPTION_BLOCK_MODE_PLOT;
                    }
                    NBTTagList newLore = new NBTTagList();
                    newLore.appendTag(new NBTTagString(Constants.EDIT_TOOL_DESCRIPTION_BLOCK_WHITELIST));
                    newLore.appendTag(new NBTTagString(mode));
                    newLore.appendTag(new NBTTagString(lore.getStringTagAt(2)));
                    newLore.appendTag(new NBTTagString(EnumChatFormatting.DARK_AQUA + "Uses: 1"));
                    currentStack.getTagCompound().getCompoundTag("display").setTag("Lore", newLore);

                }
            }
        }
        if (ev.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK && currentStack.getItem().equals(Items.wooden_hoe) && currentStack.getDisplayName().equals(Constants.EDIT_TOOL_NAME)) {
            Resident res = DatasourceProxy.getDatasource().getOrMakeResident(ev.entityPlayer);
            Town town = res.getSelectedTown();
            //TODO: Verify permission

            NBTTagList lore = currentStack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8);
            String description = lore.getStringTagAt(0);


            if (description.equals(Constants.EDIT_TOOL_DESCRIPTION_PLOT)) {
                if (res.isFirstPlotSelectionActive() && res.isSecondPlotSelectionActive()) {
                    ChatUtils.sendLocalizedChat(ev.entityPlayer, LocalizationProxy.getLocalization(), "mytown.cmd.err.plot.alreadySelected");
                } else {
                    boolean result = res.selectBlockForPlot(ev.entityPlayer.dimension, ev.x, ev.y, ev.z);
                    if (result) {
                        if (!res.isSecondPlotSelectionActive()) {
                            ChatUtils.sendLocalizedChat(ev.entityPlayer, LocalizationProxy.getLocalization(), "mytown.notification.town.plot.selectionStart");
                        } else {
                            ChatUtils.sendLocalizedChat(ev.entityPlayer, LocalizationProxy.getLocalization(), "mytown.notification.town.plot.selectionEnd");
                        }
                    } else
                        ChatUtils.sendLocalizedChat(ev.entityPlayer, LocalizationProxy.getLocalization(), "mytown.cmd.err.plot.selectionFailed");

                }
                System.out.println(String.format("Player has selected: %s;%s;%s", ev.x, ev.y, ev.z));
            } else if (description.equals(Constants.EDIT_TOOL_DESCRIPTION_BLOCK_WHITELIST)) {
                town = MyTownUniverse.getInstance().getTownsMap().get(Utils.getTownNameFromLore(ev.entityPlayer));
                if (lore.getStringTagAt(1).equals(Constants.EDIT_TOOL_DESCRIPTION_BLOCK_MODE_PLOT)) {
                    Plot plot = town.getPlotAtResident(res);
                    if (!plot.isCoordWithin(ev.world.provider.dimensionId, ev.x, ev.y, ev.z) && plot.hasOwner(res)) {
                        res.sendMessage(LocalizationProxy.getLocalization().getLocalization("mytown.cmd.err.blockNotInPlot"));
                        return;
                    } else {
                        // If plot is within the bounds of a plot then create or delete the blockwhitelist

                        String whitelisterFlagName = Utils.getFlagNameFromLore(ev.entityPlayer);
                        ev.entityPlayer.setCurrentItemOrArmor(0, null);
                        BlockWhitelist bw = plot.getTown().getBlockWhitelist(ev.world.provider.dimensionId, ev.x, ev.y, ev.z, whitelisterFlagName, plot.getDb_ID());
                        if (bw == null) {
                            bw = new BlockWhitelist(ev.world.provider.dimensionId, ev.x, ev.y, ev.z, whitelisterFlagName, plot.getDb_ID());
                            res.sendMessage(LocalizationProxy.getLocalization().getLocalization("mytown.notification.perm.plot.whitelist.added"));
                            DatasourceProxy.getDatasource().saveBlockWhitelist(bw, plot.getTown());
                        } else {
                            res.sendMessage(LocalizationProxy.getLocalization().getLocalization("mytown.notification.perm.plot.whitelist.removed"));
                            DatasourceProxy.getDatasource().deleteBlockWhitelist(bw, plot.getTown());
                        }

                        ev.setCanceled(true);
                    }
                } else if (lore.getStringTagAt(1).equals(Constants.EDIT_TOOL_DESCRIPTION_BLOCK_MODE_TOWN)) {
                    Town townAt = Utils.getTownAtPosition(ev.world.provider.dimensionId, ev.x >> 4, ev.z >> 4);
                    if (town == null || town != townAt) {
                        res.sendMessage(LocalizationProxy.getLocalization().getLocalization("mytown.cmd.err.blockNotInTown"));
                        return;
                    } else {
                        // If town is found then create of delete the block whitelist

                        String whitelisterFlagName = Utils.getFlagNameFromLore(ev.entityPlayer);
                        ev.entityPlayer.setCurrentItemOrArmor(0, null);
                        BlockWhitelist bw = town.getBlockWhitelist(ev.world.provider.dimensionId, ev.x, ev.y, ev.z, whitelisterFlagName, 0);
                        if (bw == null) {
                            bw = new BlockWhitelist(ev.world.provider.dimensionId, ev.x, ev.y, ev.z, whitelisterFlagName, 0);
                            res.sendMessage(LocalizationProxy.getLocalization().getLocalization("mytown.notification.perm.town.whitelist.added"));
                            DatasourceProxy.getDatasource().saveBlockWhitelist(bw, town);
                        } else {
                            res.sendMessage(LocalizationProxy.getLocalization().getLocalization("mytown.notification.perm.town.whitelist.removed"));
                            DatasourceProxy.getDatasource().deleteBlockWhitelist(bw, town);
                        }
                        ev.setCanceled(true);
                    }
                }
            }
        }
    }


    @SubscribeEvent
    public void onPlayerBreaksBlock(BlockEvent.BreakEvent ev) {
        if (VisualsTickHandler.instance.isBlockMarked(ev.x, ev.y, ev.z, ev.world.provider.dimensionId)) {
            // Cancel event if it's a border that has been broken
            ev.setCanceled(true);
        }
    }


}