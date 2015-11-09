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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.annotations.CopyIgnore;
import cn.lambdalib.cgui.gui.annotations.EditIgnore;
import cn.lambdalib.cgui.gui.event.GuiEvent;
import cn.lambdalib.cgui.gui.event.IGuiEventHandler;
import cn.lambdalib.core.LambdaLib;
import cn.lambdalib.util.deprecated.TypeHelper;

/**
 * <summary>
 * Component is the concrete material of Widget. It can define a set of EventHandlers and store information itself.
 * </summary>
 * <p>
 * Components supports prototype patteren natively. They can be copied to make duplicates, typically when its 
 * 	container widget is being copied.
 * </p>
 * @author WeAthFolD
 */
public class Component {
	
	public final String name;
	
	public boolean enabled = true;
	
	@EditIgnore
	public boolean canEdit = true;
	
	/**
	 * This SHOULD NOT be edited after creation, represents the widget instance this component is in.
	 */
	@EditIgnore
	@CopyIgnore
	public Widget widget;
	
	public Component(String _name) {
		name = _name;
		checkCopyFields();
	}
	
	protected <T extends GuiEvent> void listen(Class<? extends T> type, IGuiEventHandler<T> handler) {
		listen(type, handler, 0);
	}
	
	protected <T extends GuiEvent> void listen(Class<? extends T> type, IGuiEventHandler<T> handler, int prio) {
		if(widget != null)
			throw new RuntimeException("Can only add event handlers before componenet is added into widget");
		Node n = new Node();
		n.type = type;
		n.handler = new EHWrapper(handler);
		n.prio = prio;
		addedHandlers.add(n);
	}
	
	/**
	 * Called when the component is added into a widget, and the widget field is correctly set.
	 */
	public void onAdded() {
		for(Node n : addedHandlers) {
			widget.listen(n.type, n.handler, n.prio, false);
		}
	}
	
	public void onRemoved() {
		for(Node n : addedHandlers) {
			widget.unlisten(n.type, n.handler);
		}
	}
	
	public Component copy() {
		try {
			Component c = getClass().newInstance();
			for(Field f : copiedFields.get(getClass())) {
				TypeHelper.set(f, c, TypeHelper.copy(f, this));
			}
			return c;
		} catch(Exception e) {
			LambdaLib.log.error("Unexpected error occured copying component of type " + getClass());
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean canStore() {
		return true;
	}
	
	/**
	 * Recover all the data fields within the component with the data map specified.
	 */
	@Deprecated
	public void fromPropertyMap(Map<String, String> map) {
		List<Field> fields = checkCopyFields();
		for(Field f : fields) {
			String val = map.get(f.getName());
			if(val != null) {
				TypeHelper.edit(f, this, val);
			}
		}
	}
	
	@Deprecated
	public Map<String, String> getPropertyMap() {
		Map<String, String> ret = new HashMap();
		for(Field f : checkCopyFields()) {
			String val = TypeHelper.repr(f, this);
			if(val != null) {
				ret.put(f.getName(), val);
			}
		}
		
		return ret;
	}
	
	public Collection<Field> getPropertyList() {
		return copiedFields.get(getClass());
	}
	
	private List<Field> checkCopyFields() {
		if(copiedFields.containsKey(getClass()))
			return copiedFields.get(getClass());
		List<Field> ret = new ArrayList<Field>();
		for(Field f : getClass().getFields()) {
			if(((f.getModifiers() & Modifier.FINAL) == 0)
			&& !f.isAnnotationPresent(CopyIgnore.class) && TypeHelper.isTypeSupported(f.getType())) {
				ret.add(f);
			}
		}
		copiedFields.put(getClass(), ret);
		return ret;
	}
	
	private static Map<Class, List<Field>> copiedFields = new HashMap();
	
	private List<Node> addedHandlers = new ArrayList();
	
	private final class EHWrapper<T extends GuiEvent> implements IGuiEventHandler<T> {
		
		final IGuiEventHandler<T> wrapped;

		public EHWrapper(IGuiEventHandler<T> _wrapped) {
			wrapped = _wrapped;
		}
		
		@Override
		public void handleEvent(Widget w, T event) {
			if(enabled)
				wrapped.handleEvent(w, event);
		}
		
	}
	
	private static class Node {
		Class<? extends GuiEvent> type;
		IGuiEventHandler handler;
		int prio;
	}
	
}

