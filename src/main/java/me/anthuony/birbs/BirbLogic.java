package me.anthuony.birbs;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class BirbLogic extends Thread
{
	private Birb birb;
	private final ArrayList<Birb> logicBirbsList;
	private static BirbsContainer bc;
	private static boolean avoidOthers = false;
	private static boolean doAlignment = true;
	private static boolean doCohesion = true;
	private static boolean deleteClose = false;
	private static int colorOffset = 0;
	
	//Multi-threading
	public BirbLogic(ArrayList<Birb> birbsList, String name, ThreadGroup tg)
	{
		super(tg, name);
		this.logicBirbsList = birbsList;
	}
	
	public void run()
	{
		for (Birb birb : logicBirbsList)
		{
			this.birb = birb;
			updateBirbColor();
			if(deleteClose)
			{
				doDeleteClose();
			}
			if(avoidOthers)
			{
				if(doAvoidOthers() && doAlignment)
				{
					doAlignment();
				}
			}
//			if(avoidOthers)
//			{
//				doAvoidOthers();
//			}
			else
			{
				if(birb.getFormationPoint() != null)
				{
					seekPoint(birb.getFormationPoint(), true);
				}
				else
				{
					seekPoint(bc.getInput().getScaledMousePoint(), true);
				}
			}
			updateBirbLocation();
		}
	}
	
	private void doDeleteClose()
	{
		int deleteRange = 10;
		ArrayList<Birb> birbsInRadius = getBirbsInRadius(deleteRange);
		for(Birb otherBirb: birbsInRadius)
		{
			bc.removeBirb(otherBirb);
		}
	}
	
	private void doAlignment()
	{
		ArrayList<Birb> birbsInRadius = getBirbsInRadius(300);
		if(birbsInRadius.size() > 0)
		{
			double avgMag = 0;
			double avgDir = 0;
			double avgX = 0;
			double avgY = 0;
			for(Birb otherBirb: birbsInRadius)
			{
				avgMag += otherBirb.getVel().getMagnitude();
				avgDir += otherBirb.getVel().getDirection();
				avgX += otherBirb.getPoint().getX();
				avgY += otherBirb.getPoint().getY();
			}
			avgMag /= birbsInRadius.size();
			avgDir /= birbsInRadius.size();
			avgX /= birbsInRadius.size();
			avgY /= birbsInRadius.size();
			Point2D.Double avgPoint = new Point2D.Double(avgX, avgY);
			
			double currentDirection = birb.getVel().getDirection();
			double adjustment = getDirectionAdjustment(currentDirection, avgDir);
			double newDirection = (adjustment+currentDirection)%(2*Math.PI);
			birb.setVel(new Vector(avgMag, newDirection));
			seekPoint(avgPoint, true);
		}
	}
	
	public double getDirectionAdjustment(double currentDirection, double desiredDirection)
	{
		double adjustment = desiredDirection - currentDirection;
//		System.out.println(desiredDirection+" "+currentDirection+" "+adjustment);
		double change = adjustment%(2*Math.PI);
		
		if(change > Math.PI && currentDirection < Math.PI) {
			change -= 2*Math.PI;
		}
		
		if(change < -Math.PI && currentDirection > Math.PI) {
			change += 2*Math.PI;
		}
		
		if(Math.abs(change) > Birb.getMaxTurnSpeed()) {
			if(change > 0) {
				change = Birb.getMaxTurnSpeed();
			}
			else {
				change = -Birb.getMaxTurnSpeed();
			}
		}
		return change;
	}
	public void seekPoint(Point2D.Double desiredPoint, boolean seek)
	{
		double desiredX = desiredPoint.getX();
		double desiredY = desiredPoint.getY();
		double currentX = birb.getPoint().getX();
		double currentY = birb.getPoint().getY();
		
		double currentDirection = birb.getVel().getDirection();
		double desiredDirection = Math.atan2(desiredY - currentY, desiredX - currentX);
		if(!seek)
		{
			desiredDirection += Math.PI;
		}
		if(desiredDirection<0) {
			desiredDirection += 2*Math.PI;
		}
		
		double adjustment = getDirectionAdjustment(currentDirection, desiredDirection);
		
		double newDirection = (adjustment+currentDirection)%(2*Math.PI);
		birb.setVel(new Vector(birb.getVel().getMagnitude(), newDirection));
	}
	
	public double getBirbDistance(Birb otherBirb)
	{
		double birbX = birb.getPoint().getX();
		double birbY = birb.getPoint().getY();
		double otherX = otherBirb.getPoint().getX();
		double otherY = otherBirb.getPoint().getY();
		double distance = Point2D.distance(birbX, birbY, otherX, otherY);
		return distance;
	}
	
	public double getBirbDistance(Birb b1, Birb b2)
	{
		double b1X = b1.getPoint().getX();
		double b1Y = b1.getPoint().getY();
		double b2X = b2.getPoint().getX();
		double b2Y = b2.getPoint().getY();
		double distance = Point2D.distance(b1X, b1Y, b2X, b2Y);
		return distance;
	}
	
	public ArrayList<Birb> getBirbsInRadius(int radius)
	{
		ArrayList<Birb> birbsInRadius = new ArrayList<Birb>();
		for(Birb otherBirb: bc.getBirbsList())
		{
			if(otherBirb != birb)
			{
				double distance = getBirbDistance(otherBirb);
				if(distance <= radius)
				{
					birbsInRadius.add(otherBirb);
				}
//				System.out.println(distance);
			}
		}
		return birbsInRadius;
	}
	
	public boolean doAvoidOthers()
	{
		int avoidRange = (int)birb.getVel().getMagnitude()*15+25;
		ArrayList<Birb> birbsInRadius = getBirbsInRadius(avoidRange);
		Birb closestOther = null;
		if(birbsInRadius.size() > 0)
		{
			closestOther = birbsInRadius.get(0);
		}
		for(Birb otherBirb: birbsInRadius)
		{
			if(getBirbDistance(closestOther, otherBirb) < getBirbDistance(closestOther, birb))
			{
				closestOther = otherBirb;
			}
		}
		if(closestOther != null)
		{
			seekPoint(closestOther.getPoint(), false);
			birb.setSpeedMultiplier(getBirbDistance(closestOther)/(avoidRange*1.5));
			return false;
//			System.out.println(getBirbDistance(closestOther));
		}
		return true;
	}
	
	public void updateBirbLocation()
	{
		double x = birb.getPoint().getX();
		double y = birb.getPoint().getY();
		Vector vel = birb.getVel();
		x = (x + vel.getMagnitude() * birb.getSpeedMultiplier() * Math.cos(vel.getDirection()));
		y = (y + vel.getMagnitude() * birb.getSpeedMultiplier() * Math.sin(vel.getDirection()));
		
		//Adjust for world boundaries and birb boundaries
		x = (Math.abs((x + bc.getWorldWidth() + birb.getWidth() / 2.0) % bc.getWorldWidth())) - birb.getWidth() / 2.0;
		y = (Math.abs((y + bc.getWorldHeight() + birb.getHeight() / 2.0) % bc.getWorldHeight())) - birb.getHeight() / 2.0;
		
		//Update location
		Point2D.Double newPoint = new Point2D.Double(x, y);
		birb.setWorldPoint(newPoint);
	}
	
	public void updateBirbColor()
	{
//		Color color = birb.getBirbColor();
//		int r = (color.getRed() + 3) % 255;
//		int g = (color.getGreen() + 1) % 255;
//		int bl = (color.getBlue() + 2) % 255;
		int b = (int)((birb.getPoint().getX() + colorOffset) / bc.getWorldWidth() * 255);
		int g = (int)((birb.getPoint().getY() + colorOffset) / bc.getWorldHeight() * 255);
		int r = 255 - b;
		
		b = Math.max(b, 0);
		g = Math.max(g, 0);
		r = Math.max(r, 0);
		b = Math.min(b, 255);
		g = Math.min(g, 255);
		r = Math.min(r, 255);
		
		if(Math.abs(b - colorOffset) < 10)
		{
			r = 255;
			g = 255;
			b = 255;
		}
		if(Math.abs(g - colorOffset) < 10)
		{
			g = 255;
			b = 255;
		}
		
		Vector vel = birb.getVel();
		int velMag = (int) vel.getMagnitude();
//		Color newColor = new Color(r, g, b, 50 + 30 * velMag);
		Color newColor = new Color(r, g, b,255);
		birb.setBirbColor(newColor);
	}
	
	public static void setBc(BirbsContainer bc)
	{
		BirbLogic.bc = bc;
	}
	public static void incrementColorOffset()
	{
		colorOffset++;
		if(colorOffset > 255)
		{
			colorOffset = 0;
		}
	}
}