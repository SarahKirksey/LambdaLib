/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib.cgui.gui.component;

import java.util.Random;

import org.lwjgl.opengl.GL11;

import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.util.client.HudUtils;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.util.helper.GameTimer;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 *
 */
public class ProgressBar extends Component {
    
    public enum Direction { RIGHT, LEFT, UP, DOWN };
    
    public boolean illustrating = false;
    public ResourceLocation texture;
    public double maxDelta = 0.5; //prog per sec
    public double 
        maxFluctSpeed = .8, //prog per sec
        fluctRegion = 0.15; //fluct in (progress - 0.5*fluctRegion, progress + 0.5 * fluctRegion)
    public Direction dir = Direction.RIGHT;
    public double progress;
    public Color color = Color.white();
    
    double curFluct;
    double curSpeed;
    public double progressDisplay = -1; //cur display progress

    public ProgressBar() {
        super("ProgressBar");
        listen(FrameEvent.class, (wi, e) -> {
            if(illustrating) {
                progress = 0.5 * (1 + Math.sin(GameTimer.getAbsTime() / 1000.0));
            }
            
            {
                long time = GameTimer.getAbsTime();
                if(lastDrawTime == 0) lastDrawTime = time;
                
                double dt = Math.min((time - lastDrawTime) * 0.001, 10); //convert to seconds
                
                if(progressDisplay == -1) {
                    progressDisplay = progress;
                } else {
                    //Buffering
                    double delta = progress - progressDisplay;
                    double sgn = Math.signum(delta);
                    delta = Math.min(Math.abs(delta), dt * maxDelta);
                    progressDisplay += sgn * delta;
                }
                
                { //Fluctuation
                    double accel = (rand.nextDouble() - 0.5) * maxFluctSpeed;
                    curSpeed += accel;
                    curSpeed = Math.max(-maxFluctSpeed, Math.min(curSpeed, maxFluctSpeed));
                    curFluct += curSpeed * dt;
                    curFluct = Math.max(-0.5 * fluctRegion, Math.min(curFluct, 0.5 * fluctRegion));
                }
                
                lastDrawTime = time;
            }
            
            {
                double disp;
                if(progressDisplay == 0) {
                    disp = 0;
                } else if(progressDisplay == 1) {
                    disp = 1;
                } else {
                    disp = Math.max(0, Math.min(progressDisplay + curFluct, 1.0));
                }
                
                //System.out.println(progressDisplay + " " + curFluct + " " + disp);
                double x, y, u = 0, v = 0, w, h, tw, th;
                double width = wi.transform.width, height = wi.transform.height;
                switch(dir) {
                case RIGHT:
                    w = width * disp;
                    h = height;
                    x = y = 0;
                    
                    u = 0;
                    v = 0;
                    tw = disp;
                    th = 1;
                    break;
                case LEFT:
                    w = width * disp;
                    h = height;
                    x = width - w;
                    y = 0;
                    
                    u = (1 - disp);
                    v = 0;
                    tw = disp;
                    th = 1;
                    break;
                case UP:
                    w = width;
                    h = height * disp;
                    x = 0;
                    y = height * (1 - disp);
                    
                    u = 0;
                    v = (1 - disp);
                    tw = 1;
                    th = disp;
                    break;
                case DOWN:
                    w = width;
                    h = height * disp;
                    x = y = 0;
                    u = 0;
                    v = 0;
                    tw = 1;
                    th = disp;
                    break;
                default:
                    throw new RuntimeException("niconiconi, WTF??");
                }
                if(texture != null && !texture.getResourcePath().equals("<null>")) {
                    RenderUtils.loadTexture(texture);
                } else {
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                }
                color.bind();
                HudUtils.rawRect(x, y, u, v, w, h, tw, th);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }
        });
    }
    
    long lastDrawTime;
    
    private static final Random rand = new Random();
    
    public ProgressBar setDirection(Direction dir) {
        this.dir = dir;
        return this;
    }
    
    public ProgressBar setFluctRegion(double r) {
        fluctRegion = r;
        return this;
    }
    
    public static ProgressBar get(Widget w) {
        return w.getComponent("ProgressBar");
    }

    
}