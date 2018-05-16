package com.feed_the_beast.ftbutilities.data;

import com.feed_the_beast.ftblib.events.player.ForgePlayerConfigEvent;
import com.feed_the_beast.ftblib.lib.config.ConfigBoolean;
import com.feed_the_beast.ftblib.lib.config.ConfigString;
import com.feed_the_beast.ftblib.lib.config.RankConfigAPI;
import com.feed_the_beast.ftblib.lib.data.ForgePlayer;
import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.data.IHasCache;
import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.lib.math.BlockDimPos;
import com.feed_the_beast.ftblib.lib.util.ServerUtils;
import com.feed_the_beast.ftblib.lib.util.StringUtils;
import com.feed_the_beast.ftblib.lib.util.misc.IScheduledTask;
import com.feed_the_beast.ftblib.lib.util.misc.Node;
import com.feed_the_beast.ftbutilities.FTBUtilities;
import com.feed_the_beast.ftbutilities.FTBUtilitiesConfig;
import com.feed_the_beast.ftbutilities.FTBUtilitiesPermissions;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.server.command.TextComponentHelper;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author LatvianModder
 */
public class FTBUtilitiesPlayerData implements INBTSerializable<NBTTagCompound>, IHasCache
{
	public enum Timer
	{
		HOME(FTBUtilitiesPermissions.HOMES_COOLDOWN, FTBUtilitiesPermissions.HOMES_WARMUP),
		WARP(FTBUtilitiesPermissions.WARPS_COOLDOWN, FTBUtilitiesPermissions.WARPS_WARMUP),
		BACK(FTBUtilitiesPermissions.BACK_COOLDOWN, FTBUtilitiesPermissions.BACK_WARMUP),
		SPAWN(FTBUtilitiesPermissions.SPAWN_COOLDOWN, FTBUtilitiesPermissions.SPAWN_WARMUP),
		TPA(FTBUtilitiesPermissions.TPA_COOLDOWN, FTBUtilitiesPermissions.TPA_WARMUP);

		public static final Timer[] VALUES = values();

		private final Node cooldown;
		private final Node warmup;

		Timer(Node c, Node w)
		{
			cooldown = c;
			warmup = w;
		}

		public void teleport(EntityPlayerMP player, double x, double y, double z, int dim, @Nullable IScheduledTask extraTask)
		{
			Universe universe = Universe.get();
			int seconds = RankConfigAPI.get(player, warmup).getInt();

			if (seconds > 0)
			{
				player.sendStatusMessage(StringUtils.color(TextComponentHelper.createComponentTranslation(player, "stand_still", seconds).appendText(" [" + seconds + "]"), TextFormatting.GOLD), true);
				universe.scheduleTask(universe.world.getTotalWorldTime() + 20L, new TeleportTask(player, this, seconds, seconds, x, y, z, dim, extraTask));
			}
			else
			{
				new TeleportTask(player, this, 0, 0, x, y, z, dim, extraTask).execute(universe);
			}
		}

		public void teleport(EntityPlayerMP player, BlockDimPos pos, @Nullable IScheduledTask extraTask)
		{
			teleport(player, pos.posX + 0.5D, pos.posY + 0.1D, pos.posZ + 0.5D, pos.dim, extraTask);
		}
	}

	private static class TeleportTask implements IScheduledTask
	{
		private final EntityPlayerMP player;
		private final Timer timer;
		private final BlockDimPos startPos;
		private final double toX, toY, toZ;
		private final float startHP;
		private final int toDim, startSeconds, secondsLeft;
		private final IScheduledTask extraTask;

		private TeleportTask(EntityPlayerMP p, Timer t, int ss, int s, double x, double y, double z, int dim, @Nullable IScheduledTask e)
		{
			player = p;
			timer = t;
			startPos = new BlockDimPos(player);
			startHP = player.getHealth();
			toX = x;
			toY = y;
			toZ = z;
			toDim = dim;
			startSeconds = ss;
			secondsLeft = s;
			extraTask = e;
		}

		@Override
		public void execute(Universe universe)
		{
			if (!startPos.equalsPos(new BlockDimPos(player)) || startHP != player.getHealth())
			{
				player.sendStatusMessage(StringUtils.color(TextComponentHelper.createComponentTranslation(player, "stand_still_failed"), TextFormatting.RED), true);
			}
			else if (secondsLeft <= 1)
			{
				ServerUtils.teleportEntity(player.mcServer, player, toX, toY, toZ, toDim);
				FTBUtilitiesPlayerData data = FTBUtilitiesPlayerData.get(universe.getPlayer(player));
				data.lastTeleport[timer.ordinal()] = universe.world.getTotalWorldTime();

				if (secondsLeft != 0)
				{
					player.sendStatusMessage(TextComponentHelper.createComponentTranslation(player, "teleporting"), true);
				}

				if (extraTask != null)
				{
					extraTask.execute(universe);
				}
			}
			else
			{
				universe.scheduleTask(universe.world.getTotalWorldTime() + 20L, new TeleportTask(player, timer, startSeconds, secondsLeft - 1, toX, toY, toZ, toDim, extraTask));
				player.sendStatusMessage(new TextComponentString(Integer.toString(secondsLeft - 1)), true);
				player.sendStatusMessage(StringUtils.color(TextComponentHelper.createComponentTranslation(player, "stand_still", startSeconds).appendText(" [" + (secondsLeft - 1) + "]"), TextFormatting.GOLD), true);
			}
		}
	}

	public final ForgePlayer player;

	private final ConfigBoolean renderBadge = new ConfigBoolean(true);
	private final ConfigBoolean disableGlobalBadge = new ConfigBoolean(false);
	private final ConfigBoolean enablePVP = new ConfigBoolean(true);
	private final ConfigString nickname = new ConfigString("");

	public ForgeTeam lastChunkTeam;
	public final Collection<ForgePlayer> tpaRequestsFrom;

	private BlockDimPos lastDeath, lastSafePos;
	private long[] lastTeleport;
	public final BlockDimPosStorage homes;
	private boolean fly;

	public FTBUtilitiesPlayerData(ForgePlayer p)
	{
		player = p;
		homes = new BlockDimPosStorage();
		tpaRequestsFrom = new HashSet<>();
		lastTeleport = new long[Timer.VALUES.length];
	}

	public static FTBUtilitiesPlayerData get(ForgePlayer player)
	{
		return player.getData().get(FTBUtilities.MOD_ID);
	}

	@Override
	public NBTTagCompound serializeNBT()
	{
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setBoolean("RenderBadge", renderBadge.getBoolean());
		nbt.setBoolean("DisableGlobalBadges", disableGlobalBadge.getBoolean());
		nbt.setBoolean("EnablePVP", enablePVP.getBoolean());
		nbt.setTag("Homes", homes.serializeNBT());
		nbt.setBoolean("AllowFlying", fly);

		if (lastDeath != null)
		{
			nbt.setIntArray("LastDeath", lastDeath.toIntArray());
		}

		nbt.setString("Nickname", nickname.getString());
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt)
	{
		renderBadge.setBoolean(!nbt.hasKey("RenderBadge") || nbt.getBoolean("RenderBadge"));
		disableGlobalBadge.setBoolean(nbt.getBoolean("DisableGlobalBadges"));
		enablePVP.setBoolean(!nbt.hasKey("EnablePVP") || nbt.getBoolean("EnablePVP"));
		homes.deserializeNBT(nbt.getCompoundTag("Homes"));
		fly = nbt.getBoolean("AllowFlying");
		lastDeath = BlockDimPos.fromIntArray(nbt.getIntArray("LastDeath"));
		nickname.setString(nbt.getString("Nickname"));
	}

	public void addConfig(ForgePlayerConfigEvent event)
	{
		event.getConfig().setGroupName(FTBUtilities.MOD_ID, new TextComponentString(FTBUtilities.MOD_NAME));
		event.getConfig().add(FTBUtilities.MOD_ID, "render_badge", renderBadge);
		event.getConfig().add(FTBUtilities.MOD_ID, "disable_global_badge", disableGlobalBadge);
		event.getConfig().add(FTBUtilities.MOD_ID, "enable_pvp", enablePVP);

		if (FTBUtilitiesConfig.commands.nick && event.getPlayer().hasPermission(FTBUtilitiesPermissions.NICKNAME))
		{
			event.getConfig().add(FTBUtilities.MOD_ID, "nickname", nickname);
		}
	}

	public boolean renderBadge()
	{
		return renderBadge.getBoolean();
	}

	public boolean disableGlobalBadge()
	{
		return disableGlobalBadge.getBoolean();
	}

	public boolean enablePVP()
	{
		return enablePVP.getBoolean();
	}

	public String getNickname()
	{
		return nickname.getString();
	}

	public void setNickname(String name)
	{
		nickname.setString(name.equals(player.getName()) ? "" : name);
		player.markDirty();
		clearCache();
	}

	public void setFly(boolean v)
	{
		fly = v;
		player.markDirty();
	}

	public boolean getFly()
	{
		return fly;
	}

	public void setLastDeath(@Nullable BlockDimPos pos)
	{
		lastDeath = pos;
		player.markDirty();
	}

	@Nullable
	public BlockDimPos getLastDeath()
	{
		return lastDeath;
	}

	public void setLastSafePos(@Nullable BlockDimPos pos)
	{
		lastSafePos = pos;
		player.markDirty();
	}

	@Nullable
	public BlockDimPos getLastSafePos()
	{
		return lastSafePos;
	}

	public long getTeleportCooldown(Timer timer)
	{
		return lastTeleport[timer.ordinal()] + player.getRankConfig(timer.cooldown).getInt() * 20 - player.team.universe.world.getTotalWorldTime();
	}

	@Override
	public void clearCache()
	{
		if (player.isOnline())
		{
			player.getPlayer().refreshDisplayName();
		}
	}
}