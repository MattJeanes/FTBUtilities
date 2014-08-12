package latmod.core.mod.gui;
import latmod.core.mod.gui.GuiLM;
import cpw.mods.fml.relauncher.*;

@SideOnly(Side.CLIENT)
public abstract class ButtonLM extends WidgetLM
{
	public ButtonLM(GuiLM g, int x, int y, int w, int h)
	{ super(g, x, y, w, h); }
	
	public boolean mousePressed(int mx, int my, int b)
	{
		if(mouseOver(mx, my))
		{
			onButtonPressed(b);
			return true;
		}
		
		return false;
	}
	
	public abstract void onButtonPressed(int b);
}