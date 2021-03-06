package me.anthuony.birbs;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class BirbLogic extends Thread
{
	private static final boolean avoidOthers = true;
	private static final boolean doAlignment = true;
	private static final boolean doCohesion = true;
	private static final boolean deleteClose = false;
	private final ArrayList<Birb> logicBirbsList;
	private final BirbsContainer bc;
	private Birb birb;
	
	//Multi-threading
	public BirbLogic(ArrayList<Birb> birbsList, String name, ThreadGroup tg, BirbsContainer bc)
	{
		super(tg, name);
		this.logicBirbsList = birbsList;
		this.bc = bc;
	}
	
	public static double getPointDistance(Point2D.Double p1, Point2D.Double p2)
	{
		double p1x = p1.getX();
		double p1y = p1.getY();
		double p2x = p2.getX();
		double p2y = p2.getY();
		return Point2D.distance(p1x, p1y, p2x, p2y);
	}
	
	public static double getBirbDistance(Birb b1, Birb b2)
	{
		return getPointDistance(b1.getWorldPoint(), b2.getWorldPoint());
	}
	
	public void run()
	{
		for (Birb birb : logicBirbsList)
		{
			this.birb = birb;
			if (!bc.isPaused())
			{
				updateBirbColor();
				if (deleteClose)
				{
					doDeleteClose();
				}
				if (avoidOthers)
				{
					if (doAvoidOthers() && doAlignment)
					{
						doAlignment();
					}
				} else
				{
					if (birb.getFormationPoint() != null)
					{
//						adjustFormationSpeed(birb.getFormationPoint());
						seekPoint(birb.getFormationPoint(), true);
					} else
					{
						seekPoint(bc.getInput().getScaledMousePoint(), true);
					}
				}
			}
			updateBirbLocation();
		}
	}
	
	private void doDeleteClose()
	{
		int deleteRange = 10;
		ArrayList<Birb> birbsInRadius = getBirbsInRadius(deleteRange);
		for (Birb otherBirb : birbsInRadius)
		{
			bc.removeBirb(otherBirb);
		}
	}
	
	private void doAlignment()
	{
		ArrayList<Birb> birbsInRadius = getBirbsInRadius(25 * birb.getVel().getMagnitude());
		if (birbsInRadius.size() > 0)
		{
			double avgMag = 0;
			double avgDir = 0;
			double avgX = 0;
			double avgY = 0;
			for (Birb otherBirb : birbsInRadius)
			{
				avgMag += otherBirb.getVel().getMagnitude();
				avgDir += otherBirb.getVel().getDirection();
				avgX += otherBirb.getWorldPoint().getX();
				avgY += otherBirb.getWorldPoint().getY();
			}
			avgMag /= birbsInRadius.size();
			avgDir /= birbsInRadius.size();
			avgX /= birbsInRadius.size();
			avgY /= birbsInRadius.size();
			Point2D.Double avgPoint = new Point2D.Double(avgX, avgY);
			
			double currentDirection = birb.getVel().getDirection();
			double adjustment = getDirectionAdjustment(currentDirection, avgDir);
			double newDirection = (adjustment + currentDirection) % (2 * Math.PI);
			birb.setVel(new Vector(avgMag, newDirection));
			seekPoint(avgPoint, true);
		}
	}
	
	public double getDirectionAdjustment(double currentDirection, double desiredDirection)
	{
		double adjustment = desiredDirection - currentDirection;
//		System.out.println(desiredDirection+" "+currentDirection+" "+adjustment);
		double change = adjustment % (2 * Math.PI);
		
		if (change > Math.PI && currentDirection < Math.PI)
		{
			change -= 2 * Math.PI;
		}
		
		if (change < -Math.PI && currentDirection > Math.PI)
		{
			change += 2 * Math.PI;
		}
		
		if (Math.abs(change) > Birb.getMaxTurnSpeed())
		{
			if (change > 0)
			{
				change = Birb.getMaxTurnSpeed();
			} else
			{
				change = -Birb.getMaxTurnSpeed();
			}
		}
		return change;
	}
	
	public void seekPoint(Point2D.Double desiredPoint, boolean seek)
	{
		double desiredX = desiredPoint.getX();
		double desiredY = desiredPoint.getY();
		double currentX = birb.getWorldPoint().getX();
		double currentY = birb.getWorldPoint().getY();
		
		double currentDirection = birb.getVel().getDirection();
		double desiredDirection = Math.atan2(desiredY - currentY, desiredX - currentX);
		if (!seek)
		{
			desiredDirection += Math.PI;
		}
		if (desiredDirection < 0)
		{
			desiredDirection += 2 * Math.PI;
		}
		
		double adjustment = getDirectionAdjustment(currentDirection, desiredDirection);
		
		double newDirection = (adjustment + currentDirection) % (2 * Math.PI);
		birb.setVel(new Vector(birb.getVel().getMagnitude(), newDirection));
	}
	
	public void adjustFormationSpeed(Point2D.Double formationPoint)
	{
		double distance = getPointDistance(formationPoint, birb.getWorldPoint());
		double slowDownRange = 300;
		if (distance < slowDownRange)
		{
			birb.setSpeedMultiplier(Math.min(distance / slowDownRange, 20));
		} else
		{
			birb.setSpeedMultiplier(1);
		}
	}
	
	public double getBirbDistance(Birb otherBirb)
	{
		return getPointDistance(birb.getWorldPoint(), otherBirb.getWorldPoint());
	}
	
	public ArrayList<Birb> getBirbsInRadius(double radius)
	{
		ArrayList<Birb> birbsInRadius = new ArrayList<Birb>();
		for (Birb otherBirb : bc.getBirbsList())
		{
			if (otherBirb != birb)
			{
				double distance = getBirbDistance(otherBirb);
				if (distance <= radius)
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
		int avoidRange = (int) birb.getVel().getMagnitude() * 15 + 25;
		ArrayList<Birb> birbsInRadius = getBirbsInRadius(avoidRange);
		Birb closestOther = null;
		if (birbsInRadius.size() > 0)
		{
			closestOther = birbsInRadius.get(0);
		}
		for (Birb otherBirb : birbsInRadius)
		{
			if (getBirbDistance(closestOther, otherBirb) < getBirbDistance(closestOther, birb))
			{
				closestOther = otherBirb;
			}
		}
		if (closestOther != null)
		{
			seekPoint(closestOther.getWorldPoint(), false);
			birb.setSpeedMultiplier(getBirbDistance(closestOther) / (avoidRange * 1.5));
			return false;
//			System.out.println(getBirbDistance(closestOther));
		}
		return true;
	}
	
	public void updateBirbLocation()
	{
		double x = birb.getWorldPoint().getX();
		double y = birb.getWorldPoint().getY();
		Vector vel = birb.getVel();
		if (!bc.isPaused())
		{
			x = (x + vel.getMagnitude() * birb.getSpeedMultiplier() * Math.cos(vel.getDirection()));
			y = (y + vel.getMagnitude() * birb.getSpeedMultiplier() * Math.sin(vel.getDirection()));
		}
		
		//Adjust for world boundaries and birb boundaries
//		x = (Math.abs((x + bc.getWorldWidth() + birb.getWidth() / 2.0) % bc.getWorldWidth())) - birb.getWidth() / 2.0;
//		y = (Math.abs((y + bc.getWorldHeight() + birb.getHeight() / 2.0) % bc.getWorldHeight())) - birb.getHeight() / 2.0;
		x = (Math.abs((x + bc.getWorldWidth() + 70 / 2.0) % bc.getWorldWidth())) - 70 / 2.0;
		y = (Math.abs((y + bc.getWorldHeight() + 70 / 2.0) % bc.getWorldHeight())) - 70 / 2.0;
		
		//Update location
		Point2D.Double newWorldPoint = new Point2D.Double(x, y);
		birb.setWorldPoint(newWorldPoint);
		
		double sX = (birb.getWorldPoint().getX() + bc.getCameraOffsetX()) * bc.getScale();
		double sY = (birb.getWorldPoint().getY() + bc.getCameraOffsetY()) * bc.getScale();
		
		Point2D.Double newScreenPoint = new Point2D.Double(sX, sY);
		birb.setScreenPoint(newScreenPoint);
		birb.setOnScreen(sX > -70 * bc.getScale() && sX < bc.getWindowWidth() + 70 * bc.getScale() && sY > -70 * bc.getScale() && sY < bc.getWindowHeight() + 70 * bc.getScale());
	}
	
	public void updateBirbColor()
	{
		double x = birb.getWorldPoint().getX();
		double y = birb.getWorldPoint().getY();
		int b = (int) (x / bc.getWorldWidth() * 255);
		int g = (int) (y / bc.getWorldHeight() * 255);
		int r = 255 - b;
		
		double angle = Math.abs(Math.toDegrees(birb.getVel().getDirection()));
//		int r = (int)(angle * 255 / 360);
//		int g = (int)(angle * 255 / 360);
//		int b = (int)(angle * 255 / 360);
		
		b = Math.min(Math.max(b, 50), 255);
		g = Math.min(Math.max(g, 50), 255);
		r = Math.min(Math.max(r, 50), 255);
		
		Color newColor = new Color(r, g, b, 255);
		birb.setBirbColor(newColor);
	}
}
