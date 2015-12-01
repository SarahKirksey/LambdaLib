/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.lambdalib.cgui.gui.component;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.vecmath.Vector2d;

import cn.lambdalib.util.client.font.IFont;
import cn.lambdalib.util.client.font.IFont.FontOption;
import cn.lambdalib.util.client.font.TrueTypeFont;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.annotations.CopyIgnore;
import cn.lambdalib.cgui.gui.component.Transform.HeightAlign;
import cn.lambdalib.cgui.gui.component.Transform.WidthAlign;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.gui.event.GuiEvent;
import cn.lambdalib.cgui.gui.event.KeyEvent;
import cn.lambdalib.cgui.gui.event.LeftClickEvent;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.util.helper.GameTimer;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.StatCollector;

/**
 * Textbox displays text on the widget area, it might also be edited. TextBox is designed to handle ONE-LINE texts.
 * @author WeAthFolD
 */
public class TextBox extends Component {
	
	/**
	 * Fired each time the TextBox's content is being edited.
	 */
	public static class ChangeContentEvent implements GuiEvent {}
	
	/**
	 * Fired each time the TextBox's input is confirmed. (a.k.a. User presses enter)
	 */
	public static class ConfirmInputEvent implements GuiEvent {}

	private static TrueTypeFont defaultFont = new TrueTypeFont(new Font("Consolas", Font.PLAIN, 32));
	static {
		defaultFont.setIdeographicFont(new Font("微软雅黑", Font.PLAIN, 32));
	}
	
	public String content = "";

	public IFont font = defaultFont;

	public FontOption option;
	
	/**
	 * Only activated when doesn't allow edit. If activated, The display string will be StatCollector.translateToLocal(content).
	 */
	public boolean localized = false;
	
	public boolean allowEdit = false;
	
	public boolean doesEcho = false;
	public char echoChar = '*';
	
	public Color color = new Color(0xffffffff);
	
	/**
	 * Whether this textBox doesn't draw chars that are out of bounds.
	 */
	public boolean emit = false;
	
	public double zLevel = 0;

	public HeightAlign heightAlign = HeightAlign.CENTER;

	// DEPRECATED
	public double size = 5;
	public WidthAlign widthAlign = WidthAlign.LEFT;
	// DEPRECATED END
	
	@CopyIgnore
	public int caretPos = 0;
	
	public TextBox setSize(double s) {
		size = s;
		return this;
	}
	
	public TextBox allowEdit() {
		allowEdit = true;
		return this;
	}
	
	public TextBox setContent(String str) {
		content = str;
		return this;
	}
	
	private String getProcessedContent() {
		String str = content;
		if(!allowEdit && localized) {
			str = StatCollector.translateToLocal(str);
		}
		
		if(doesEcho) {
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < str.length(); ++i) {
				sb.append(echoChar);
			}
			str = sb.toString();
		}
		
		return str;
	}
	
	private String getClipboardContent() {
		Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		if(cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
			try {
				return (String) cb.getData(DataFlavor.stringFlavor);
			} catch (UnsupportedFlavorException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return "";
	}
	
	private void saveClipboardContent() {
		Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection ss = new StringSelection(content);
		cb.setContents(ss, ss);
	}
	
	private double[] getOffset(Widget w) {
		double x = 0, y = 0;
		
		switch(option.align) {
		case LEFT:
			x = 2;
			break;
		case CENTER:
			x = w.transform.width/ 2;
			break;
		case RIGHT:
			x = w.transform.width;
			break;
		default:
			break;
		}
		
		switch(heightAlign) {
		case TOP:
			y = 0;
			break;
		case CENTER:
			y = (w.transform.height - option.fontSize * 0.8) / 2;
			break;
		case BOTTOM:
			y = (w.transform.height - option.fontSize * 0.8);
			break;
		default:
			break;
		}

		return new double[] { x, y };
	}

	public TextBox() {
		this(new FontOption());
	}
	
	public TextBox(FontOption option) {
		super("TextBox");
		this.option = option;

		listen(KeyEvent.class, (w, event) -> {
			if(!allowEdit)
				return;
			checkCaret();
			
			int par2 = event.keyCode;
			
			if(par2 == Keyboard.KEY_RIGHT) {
				caretPos++;
			} else if(par2 == Keyboard.KEY_LEFT) {
				caretPos--;
			}
			
			if(caretPos < 0) caretPos = 0;
			if(caretPos > content.length()) caretPos = content.length();
			
			if(event.keyCode == Keyboard.KEY_V && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
				String str1 = content.substring(0, caretPos), str2 = getClipboardContent(), str3 = content.substring(caretPos);
				content = str1 + str2 + str3;
				w.post(new ChangeContentEvent());
				return;
			}
			
			if(event.keyCode == Keyboard.KEY_C && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
				saveClipboardContent();
				return;
			}
			
			if (par2 == Keyboard.KEY_BACK && content.length() > 0) {
				if(caretPos > 0) {
					content = content.substring(0, caretPos - 1) + 
						(caretPos == content.length() ? "" : content.substring(caretPos, content.length()));
					--caretPos;
				}
				w.post(new ChangeContentEvent());
			} else if(par2 == Keyboard.KEY_RETURN || par2 == Keyboard.KEY_NUMPADENTER) {
				w.post(new ConfirmInputEvent());
			} else if(par2 == Keyboard.KEY_DELETE) {
				content = "";
				w.post(new ChangeContentEvent());
			}
			if (ChatAllowedCharacters.isAllowedCharacter(event.inputChar)) {
				content = content.substring(0, caretPos) + event.inputChar +
						(caretPos == content.length() ? "" : content.substring(caretPos, content.length()));
				caretPos += 1;
				w.post(new ChangeContentEvent());
			}
			
			checkCaret();
		});
		
		listen(LeftClickEvent.class, (w, e) -> {
			double len = 3;
			double[] offset = getOffset(w);
			double eventX = -offset[0] + e.x;
			
			for(int i = 0; i < content.length(); ++i) {
				double cw = font.getTextWidth(String.valueOf(content.charAt(i)), option);
				len += cw;
				
				if(len > eventX) {
					caretPos = (eventX - len + cw > cw / 2) ? i + 1 : i;
					return;
				}
			}
			caretPos = content.length();
		});
		
		listen(FrameEvent.class, (w, event) -> {
			double[] offset = getOffset(w);
			
			checkCaret();
			
			String str = getProcessedContent();
			
			GL11.glPushMatrix();
			GL11.glTranslated(0, 0, zLevel);

			font.draw(str, offset[0], offset[1], option);

			GL11.glPopMatrix();
			
			if(allowEdit && w.isFocused() && GameTimer.getAbsTime() % 1000 < 500) {
				double len = font.getTextWidth(str.substring(0, caretPos), option);
				font.draw("|", len + offset[0], offset[1], option);
			}
		});
	}
	
	private void checkCaret() {
		if(caretPos > content.length())
			caretPos = content.length() - 1;
		if(caretPos < 0) caretPos = 0;
	}
	
	public static TextBox get(Widget w) {
		return w.getComponent("TextBox");
	}

}
