package latmod.core.mod;
import latmod.core.*;
import latmod.core.base.*;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;

@Mod(modid = LC.MODID, name = LC.MODNAME, version = LC.VERSION)
public class LC
{
	protected static final String MODID = "latcore";
	protected static final String MODNAME = "LatCore";
	protected static final String VERSION = "1.2.0";
	
	@Mod.Instance(LC.MODID)
	public static LC inst;
	
	@SidedProxy(clientSide = "latmod.core.mod.LCClient", serverSide = "latmod.core.mod.LCCommon")
	public static LCCommon proxy;
	
	public static LMMod finals;
	
	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent e)
	{
		finals = new LMMod(MODID);
		
		proxy.preInit();
	}
	
	@Mod.EventHandler
	public void init(FMLInitializationEvent e)
	{
		proxy.init();
	}
	
	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent e)
	{
		proxy.postInit();
		OreHelper.load();
		new LC_TooltipHandler();
	}
}