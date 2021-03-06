package me.anthuony.birbs;

import java.awt.*;
import java.util.ArrayList;

public class Renderer
{
	private static final int d = 7 / 2;
	private static final int dd = (int) (d / 1.5);
	private static final int ddd = d / 2;
	private static final int[] triangleX = new int[]{dd, -dd, -ddd, -dd};
	private static final int[] triangleY = new int[]{0, ddd, 0, -ddd};
	BirbsContainer bc;
	
	public Renderer(BirbsContainer bc)
	{
		this.bc = bc;
	}
	
	public void drawRect(Graphics2D g2d)
	{
		g2d.setColor(Color.ORANGE);
		g2d.fillRect(bc.getWindowWidth() / 2, bc.getWindowHeight() / 2, 1, 1);
	}
	
	public void drawBackground(Graphics2D g2d)
	{
		g2d.setPaint(new Color(50, 50, 50, 255));
		g2d.fillRect(0, 0, bc.getWindowWidth(), bc.getWindowHeight());
		
		g2d.setPaint(new Color(0, 0, 0, 255));
		g2d.fillRect((int) (bc.getCameraOffsetX() * bc.getScale()), (int) (bc.getCameraOffsetY() * bc.getScale()), (int) (bc.getWorldWidth() * bc.getScale()), (int) (bc.getWorldHeight() * bc.getScale()));
	}
	
	public void drawCenteredString(Graphics2D g2d, Font f, String str, double x, double y)
	{
		FontMetrics metrics = g2d.getFontMetrics(f);
		
		x *= bc.getScale();
		y *= bc.getScale();
		x -= metrics.stringWidth(str) / 2.0;
		y += metrics.getAscent() / 2.0;
		x += bc.getCameraOffsetX() * bc.getScale();
		y += bc.getCameraOffsetY() * bc.getScale();
		
		g2d.setFont(f);
		g2d.drawString(str, (int) x, (int) y);
	}
	
	public void drawRightAlignedString(Graphics2D g2d, Font f, String str, double x, double y)
	{
		FontMetrics metrics = g2d.getFontMetrics(f);
		
		x -= metrics.stringWidth(str);
		y += metrics.getAscent();
		
		g2d.setFont(f);
		g2d.drawString(str, (int) x, (int) y);
	}
	
	public void drawLeftAlignedString(Graphics2D g2d, Font f, String str, double x, double y)
	{
		FontMetrics metrics = g2d.getFontMetrics(f);
		
		y += metrics.getAscent();
		
		g2d.setFont(f);
		g2d.drawString(str, (int) x, (int) y);
	}
	
	public void drawRightAlignedList(Graphics2D g2d, Font f, ArrayList<String> list, double x, double y)
	{
		FontMetrics metrics = g2d.getFontMetrics(f);
		
		for (int i = 0; i < list.size(); i++)
		{
			drawRightAlignedString(g2d, f, list.get(i), x, y + i * metrics.getAscent());
		}
	}
	
	public void drawLeftAlignedList(Graphics2D g2d, Font f, ArrayList<String> list, double x, double y)
	{
		FontMetrics metrics = g2d.getFontMetrics(f);
		
		for (int i = 0; i < list.size(); i++)
		{
			drawLeftAlignedString(g2d, f, list.get(i), x, y + i * metrics.getAscent());
		}
	}
	
	public void drawMousePosition(Graphics2D g2d, Font f)
	{
		g2d.setFont(f);
		try
		{
			int x = (int) (bc.getInput().getMousePoint().getX() * bc.getScale());
			int y = (int) (bc.getInput().getMousePoint().getY() * bc.getScale());
			String str = "x: " + x + " y: " + y;
			drawRightAlignedString(g2d, f, str, bc.getWindowWidth() - 10, 0);
		} catch (NullPointerException e)
		{
			//Do nothing since the mouse is not over the window
		}
	}
	
	public void drawFPS(Graphics2D g2d, Font f)
	{
		g2d.setFont(f);
		String str = "FPS: " + bc.getFps();
		drawRightAlignedString(g2d, f, str, bc.getWindowWidth() - 10, 20);
	}
	
	public Polygon getTriangle(BirbsContainer bc, double scale)
	{
		int x = (int) (Birb.getBaseWidth() * bc.getScale() * scale / 2);
		int xx = (int) (x / 1.5);
		int xxx = x / 2;
		int[] triangleX = new int[]{xx, -xx, -xxx, -xx};
		int[] triangleY = new int[]{0, xxx, 0, -xxx};
		return new Polygon(triangleX, triangleY, 4);
	}
	
	public void drawBirb(Graphics2D g2d, Birb b)
	{
		int hitboxWidth = (int) (Birb.getBaseWidth() * bc.getScale() * b.getScale() / 2);
		
		if (bc.isHitboxVisible())
		{
			g2d.setPaint(b.getBirbColor().darker());
			g2d.drawRect(-hitboxWidth, -hitboxWidth, 2 * hitboxWidth, 2 * hitboxWidth);
		}
		if (bc.isDrawName())
		{
			String name = b.getName();
			Font nameTagFont = new Font("Courier New", Font.BOLD, (int) (bc.getScale() * 40));
			g2d.setPaint(b.getBirbColor().brighter());
			FontMetrics metrics = g2d.getFontMetrics(nameTagFont);
			
			double x = -metrics.stringWidth(name) / 2.0;
			double y = +metrics.getAscent() / 2.0;
			
			g2d.setFont(nameTagFont);
			g2d.drawString(name, (int) x, (int) (y + 1.25 * hitboxWidth));
		}
		g2d.setPaint(b.getBirbColor());
		g2d.rotate(b.getVel().getDirection());
		g2d.drawPolygon(getTriangle(bc, b.getScale()));
	}
}
