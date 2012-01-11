/*
 *   Copyright 2011 Sami Ekblad sami@vaadin.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *   
 *   NOTICE: modified by Eric Belanger ebelanger@360-innovations.com
 */
package org.vaadin.rpc.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vaadin.rpc.server.ServerSideProxy;
import org.vaadin.rpc.shared.Method;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.terminal.gwt.client.ApplicationConfiguration;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.VConsole;

public class ClientSideProxy implements Serializable
{

	private static final long serialVersionUID = -1678258405670731431L;

	public static final int PARAM_STRING = 0;
	public static final int PARAM_BOOLEAN = 1;
	public static final int PARAM_INT = 2;
	public static final int PARAM_FLOAT = 3;
	public static final int PARAM_MAP = 4;
	public static final int PARAM_RESOURCE = 5;

	private static final String DEFAULT_DEBUG_ID = "ClientSideProxy";
	public static final String CLIENT_INIT = "_init";
	private static final String SERVER_CALL_PREFIX = "c_";
	private static final String SERVER_CALL_PARAM_PREFIX = "p_";
	private static final String SERVER_CALL_SEPARATOR = "_";
	private static final String SERVER_HAS_SENT_THE_INIT = "_si";

	private String debugId;
	private ApplicationConnection appConn;
	private String id;
	private boolean immediate;
	private final HashMap<String, Method> callHandlers = new HashMap<String, Method>();
	private Transcation tx;
	private final ClientSideHandler receiver;
	private boolean postpone;
	private int callCounter;
	// TODO: private boolean clientWasInitialized;
	private final Method dh = new Method()
		{

			private static final long serialVersionUID = 2903268028208023250L;

			public void invoke(String methodName, Object[] params)
			{
				receiver.handleCallFromServer(methodName, params);
			}
		};
	private boolean clientInitializationOk;
	private List<MethodCall> pendingCalls;
	private Transcation updateTx;
	private boolean widgetInitializing;

	public ClientSideProxy(ClientSideHandler clientWidget)
	{
		this(DEFAULT_DEBUG_ID, clientWidget);
	}

	public ClientSideProxy(String debugId, ClientSideHandler clientWidget)
	{
		this.debugId = debugId;
		receiver = clientWidget;
	}

	public static boolean isDebug()
	{
		return ApplicationConfiguration.isDebugMode();
	}

	public static void log(String msg)
	{
		VConsole.log(msg);
	}

	public boolean debug()
	{
		return isDebug();
	}

	public void d(String msg)
	{
		if (debug())
		{
			debug(msg);
		}
	}

	public void debug(String msg)
	{
		debug(debugId, id, msg);
	}

	private void debug(String debugId, String instanceId, String msg)
	{
		log("[" + debugId + ":" + instanceId + "] " + msg);
	}

	public void setAppConn(ApplicationConnection client)
	{
		appConn = client;
	}

	public ApplicationConnection getAppConn()
	{
		return appConn;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getId()
	{
		return id;
	}

	public void setDebugId(String debugId)
	{
		this.debugId = debugId;
	}

	public String getDebugId()
	{
		return debugId;
	}

	public void setImmediate(boolean immediate)
	{
		this.immediate = immediate;
	}

	public boolean isImmediate()
	{
		return immediate && updateTx == null;
	}

	private void send(String name, String value, boolean now)
	{
		d("Send " + name + " = " + value);
		getAppConn().updateVariable(getId(), name, value, now);
	}

	private void send(String name, int value, boolean now)
	{
		d("Send " + name + " = " + value);
		getAppConn().updateVariable(getId(), name, value, now);
	}

	private void send(String name, float value, boolean now)
	{
		d("Send " + name + " = " + value);
		getAppConn().updateVariable(getId(), name, value, now);
	}

	private void send(String name, boolean value, boolean now)
	{
		d("Send " + name + " = " + value);
		getAppConn().updateVariable(getId(), name, value, now);
	}

	private void send(String name, String[] value, boolean now)
	{
		d("Send " + name + " = " + value);
		getAppConn().updateVariable(getId(), name, value, now);
	}

	private void send(String name, Map<String, Object> value, boolean now)
	{
		d("Send " + name + " = " + value);
		getAppConn().updateVariable(getId(), name, value, now);
	}

	public native String debug(JavaScriptObject obj) /*-{
														var s = "";
														for (var i in obj) {
														if (typeof obj[i] != "function") {
														s += i + " = " + obj[i]+ "\n";
														} else {
														s += i + " = [function]\n";
														}
														}
														return s;
														}-*/;

	/**
	 * Copy UIDL attribute values to JavaScriptObject as properties.
	 * 
	 * @param toObject
	 *            Javascript object to assign to
	 * @param uidl
	 *            Parent node.
	 * @param nodeTag
	 *            Tag name of the data.
	 * @return true if something was copied. False otherwise.
	 */
	public static boolean updateJavaScriptObject(JavaScriptObject toObject, UIDL uidl, String nodeTag)
	{
		UIDL node = uidl.getChildByTagName(nodeTag);
		if (node != null)
		{
			Set<String> keys = node.getAttributeNames();
			JavaScriptObject fromObject = getUIDLAttributes(node);
			for (String key : keys)
			{
				copyJavaScriptAttribute(key, fromObject, toObject);
			}
			return !keys.isEmpty();
		}
		return false;
	}

	private static native void copyJavaScriptAttribute(String key, JavaScriptObject from, JavaScriptObject to)/*-{
																												to[key] = from[key];
																												}-*/;

	private static native JavaScriptObject getUIDLAttributes(UIDL uidl) /*-{
																		return uidl[1];
																		}-*/;

	/**
	 * Register a method handler for server-driven calls.
	 * 
	 * @param methodName
	 * @param method
	 */
	public void register(String methodName, Method method)
	{
		callHandlers.put(methodName, method);
		d("Registered '" + methodName + "'");
	}

	/**
	 * Invokes the methods as received from server.
	 * 
	 * @param uidl
	 * @return
	 */
	private boolean invokeMethods(UIDL uidl)
	{
		return invokeMethods(uidl, false);
	}

	/**
	 * Invokes the methods as received from server.
	 * 
	 * @param uidl
	 * @return
	 */
	private boolean queueMethods(UIDL uidl)
	{
		return invokeMethods(uidl, true);
	}

	/**
	 * Invokes the methods as received from server.
	 * 
	 * @param uidl
	 * @return
	 */
	private boolean invokeMethods(UIDL uidl, boolean queueOnly)
	{

		d("Processing calls from the server " + (queueOnly ? "(pending)" : ""));
		if (uidl == null)
		{
			d("No calls to process");
			return false;
		}

		// Iterate the UIDL childs nodes and add to process queue
		Iterator<Object> i = uidl.getChildIterator();
		List<MethodCall> callsFromServer = null;
		callsFromServer = new ArrayList<MethodCall>();
		while (i.hasNext())
		{
			UIDL callData = (UIDL) i.next();
			String methodName = callData.getStringAttribute("n");

			// Client init is skipped (handled in invokeInit)
			if (CLIENT_INIT.equals(methodName))
			{
				continue;
			}

			Method ch = callHandlers.get(methodName);
			if (ch != null)
			{
				callsFromServer.add(new MethodCall(methodName, ch, getParams(callData)));
			}
			else
			{
				d("Using default handler for '" + methodName + "'");
				callsFromServer.add(new MethodCall(methodName, dh, getParams(callData)));
			}
		}
		d("Received " + callsFromServer.size() + " calls from the server to be processed " + (queueOnly ? "(pending)" : ""));

		if (queueOnly)
		{
			// Put into queue
			if (pendingCalls != null)
			{
				pendingCalls.addAll(callsFromServer);
			}
			else
			{
				pendingCalls = callsFromServer;
			}
			return false;
		}
		else
		{

			// Invoke
			boolean ret = callMethods(callsFromServer);
			return ret;
		}
	}

	/**
	 * Invokes the methods as received from server.
	 * 
	 * @param uidl
	 * @return
	 */
	private boolean invokeInit(UIDL uidl)
	{
		d("Processing initialization calls from the server");
		if (uidl == null)
		{
			d("No init received");
			return false;
		}

		// Iterate the UIDL childs nodes and find the init
		Iterator<Object> i = uidl.getChildIterator();
		while (i.hasNext())
		{
			UIDL callData = (UIDL) i.next();
			String methodName = callData.getStringAttribute("n");
			if (CLIENT_INIT.equals(methodName))
			{
				d("Init received from server");
				return initWidget(getParams(callData).toArray());
			}
		}
		d("No init received");
		return false;
	}

	/**
	 * Initialize the client-side widget.
	 * 
	 * By default this calls the the external receiver/handler.
	 * 
	 * @param params
	 * @return
	 */
	protected boolean initWidget(Object[] params)
	{
		widgetInitializing = true;
		if (receiver.initWidget(params))
		{
			// Asynchronous init. initComplete should be called later.
			return true;
		}
		else
		{
			// Init complete already
			clientInitComplete();
			return false;
		}

	}

	private boolean callMethods(List<MethodCall> calls)
	{
		if (calls != null && !calls.isEmpty())
		{
			d("Processing " + calls.size() + " calls");
			for (MethodCall c : calls)
			{
				c.exec();
			}
			return true;
		}
		return false;
	}

	public class VariableChange
	{

		private final String vn;
		private Integer vi;
		private Float vf;
		private String vs;
		private Boolean vb;
		private boolean immediate;
		private String[] vsa;
		private Map<String, Object> vm;

		public VariableChange(String variableName, int value)
		{
			vn = variableName;
			vi = value;
		}

		public VariableChange(String variableName, Map<String, Object> value)
		{
			vn = variableName;
			vm = value;
		}

		public void sendToServer(boolean now)
		{
			if (vs != null)
			{
				ClientSideProxy.this.send(vn, vs, now && immediate);
			}
			if (vi != null)
			{
				ClientSideProxy.this.send(vn, vi, now && immediate);
			}
			if (vb != null)
			{
				ClientSideProxy.this.send(vn, vb, now && immediate);
			}
			if (vf != null)
			{
				ClientSideProxy.this.send(vn, vf, now && immediate);
			}
			if (vsa != null)
			{
				ClientSideProxy.this.send(vn, vsa, now && immediate);
			}
			if (vm != null)
			{
				ClientSideProxy.this.send(vn, vm, now && immediate);
			}
		}

		public VariableChange(String variableName, String value)
		{
			vn = variableName;
			vs = value;
		}

		public VariableChange(String variableName, boolean value)
		{
			vn = variableName;
			vb = value;
		}

		public VariableChange(String variableName, float value)
		{
			vn = variableName;
			vf = value;
		}

		public VariableChange(String name, String[] s)
		{
			vn = name;
			vsa = s;
		}

		public Object getValue()
		{
			Object v = vi;
			if (v == null)
			{
				v = vs;
			}
			if (v == null)
			{
				v = vb;
			}
			if (v == null)
			{
				v = vf;
			}
			if (v == null)
			{
				v = vsa;
			}
			return v;
		}

		public void setImmediate(boolean immediate)
		{
			this.immediate = immediate;
		}
	}

	public class MethodCall
	{

		private final String method;
		private final Method cb;
		private final List<Object> params;

		public MethodCall(String method, Method cb, List<Object> params)
		{
			this.method = method;
			this.cb = cb;
			this.params = params;
		}

		public void exec()
		{
			if (debug())
			{
				d("Calling " + method + "(" + debugArray(params) + ")");
			}
			cb.invoke(method, params.toArray());
		}
	}

	private List<Object> getParams(UIDL callNode)
	{
		int pc = callNode.hasAttribute("pc") ? callNode.getIntAttribute("pc") : 0;
		List<Object> params = new ArrayList<Object>();
		for (int i = 0; i < pc; i++)
		{
			String pn = "p" + i;
			if (callNode.hasAttribute(pn) && callNode.hasAttribute("pt" + i))
			{
				int pt = callNode.getIntAttribute("pt" + i);
				if (pt == ClientSideProxy.PARAM_STRING)
				{
					params.add(callNode.getStringAttribute(pn));
				}
				else if (pt == ClientSideProxy.PARAM_RESOURCE)
				{
					params.add(appConn.translateVaadinUri(callNode.getStringAttribute(pn)));
				}
				else if (pt == ClientSideProxy.PARAM_BOOLEAN)
				{
					params.add(callNode.getBooleanAttribute(pn));
				}
				else if (pt == ClientSideProxy.PARAM_INT)
				{
					params.add(callNode.getIntAttribute(pn));
				}
				else if (pt == ClientSideProxy.PARAM_FLOAT)
				{
					params.add(callNode.getFloatAttribute(pn));
				}
				else if (pt == ClientSideProxy.PARAM_MAP)
				{
					params.add(callNode.getMapAttribute(pn));
				}
				else
				{
					d("Invalid parameter type '" + pt + "' for '" + callNode.getStringAttribute("name") + "' parameter " + i);
				}
			}
			else
			{
				d("Missing call parameter " + i + " for " + callNode.getStringAttribute("name"));
			}
		}
		return params;
	}

	protected String debugArray(List<Object> params)
	{
		String v = "";
		boolean first = true;
		for (Object p : params)
		{
			if (!first)
			{
				v += ",";
			}
			first = false;
			v += p;
		}
		return v;
	}

	/**
	 * Perform client-side communication init.
	 * 
	 * @param appConn
	 * @param uidl
	 */
	private void initCommunication(ApplicationConnection appConn, UIDL uidl)
	{
		setAppConn(appConn);
		setId(uidl.getId());
		setImmediate(uidl.hasAttribute("immediate") && uidl.getBooleanAttribute("immediate"));
	}

	public Transcation startTx()
	{
		d("Start transaction");
		if (tx == null)
		{
			d("New transaction created");
			tx = new Transcation();
		}
		else
		{
			tx.nestedLevel++;
		}
		return tx;
	}

	public Transcation currentTx()
	{
		return tx;
	}

	/**
	 * Transaction for sending data to the server.
	 * 
	 * With transactions the calls to the server side (or "variable changes") can be grouped to be sent as one request. This is useful, if you know that your
	 * component needs to call several methods from the server component at once.
	 * 
	 * Transactions can be nested, so that every new child transaction is committed to its parent transaction instead.
	 * 
	 * Use method {@link ClientSideProxy#startTx()} to create a new transaction.
	 * 
	 * @author Sami Ekblad / Vaadin
	 * 
	 */
	public class Transcation
	{

		List<VariableChange> data = new ArrayList<VariableChange>();
		private int nestedLevel;

		/**
		 * Instantiate only using {@link ClientSideProxy#startTx()}
		 * 
		 */
		private Transcation()
		{}

		/**
		 * Send a variable to the server.
		 * 
		 */
		private void send(VariableChange vc)
		{
			data.add(vc);
			vc.immediate = false;
		}

		/**
		 * Send a variable to the server.
		 * 
		 * @see #call(String, Object...)
		 * 
		 * @param name
		 * @param value
		 */
		public void send(String name, String value)
		{
			send(new VariableChange(name, value));
		}

		/**
		 * Send a variable to the server.
		 * 
		 * @see #call(String, Object...)
		 * 
		 * @param name
		 * @param value
		 */
		public void send(String name, boolean value)
		{
			send(new VariableChange(name, value));
		}

		/**
		 * Send a variable to the server.
		 * 
		 * @see #call(String, Object...)
		 * 
		 * @param name
		 * @param value
		 */
		public void send(String name, int value)
		{
			send(new VariableChange(name, value));
		}

		/**
		 * Send a variable to the server.
		 * 
		 * @see #call(String, Object...)
		 * 
		 * @param name
		 * @param value
		 */
		public void send(String name, float value)
		{
			send(new VariableChange(name, value));
		}

		/**
		 * Send a variable to the server.
		 * 
		 * @see #call(String, Object...)
		 * 
		 * @param name
		 * @param value
		 */
		public void send(String name, Map<String, Object> value)
		{
			send(new VariableChange(name, value));
		}

		/**
		 * Send a variable to the server.
		 * 
		 * @see #call(String, Object...)
		 * 
		 * @param name
		 * @param value
		 */
		public void send(String name, JsArrayString value)
		{
			String[] s = new String[value.length()];
			for (int i = 0; i < s.length; i++)
			{
				s[i] = value.get(i);
			}
			send(new VariableChange(name, s));
		}

		/**
		 * Send a variable to the server.
		 * 
		 * @see #call(String, Object...)
		 * 
		 * @param name
		 * @param value
		 */
		public void send(String name, String[] value)
		{
			send(new VariableChange(name, value));
		}

		/**
		 * Commit the transaction.
		 * 
		 * If this is the top-level transaction it will be sent to the server otherwise it is committed to the parent transaction.
		 * 
		 */
		public void commit()
		{
			d("Commit transaction size=" + data.size());
			if (nestedLevel > 0)
			{
				d("Nested transaction commit only to parent. level=" + nestedLevel);
				nestedLevel--;
				return;
			}
			else
			{
				tx = null;
				if (data.size() > 0)
				{
					for (VariableChange v : data)
					{
						v.sendToServer(false);
					}
					sync();
				}
				else
				{
					d("Nothing to sync");
				}
			}
		}

		/**
		 * Is this a nested transaction.
		 * 
		 * Nested transactions are committed to the parent.
		 * 
		 * @return
		 */
		public boolean isNested()
		{
			return nestedLevel > 0;
		}

		/**
		 * Cancel the transaction and discard all data.
		 */
		public void cancel()
		{
			d("Cancel transaction size=" + data.size());
			tx = null;
		}

		/**
		 * Invoke a server-side method. This will call a {@link Method} registered in the {@link ServerSideProxy} by its name.
		 * 
		 * @param method
		 * @param params
		 */
		@SuppressWarnings("unchecked")
		public void call(String method, Object... params)
		{
			int cid = callCounter++;
			send(SERVER_CALL_PREFIX + cid + SERVER_CALL_SEPARATOR + method, true);
			int i = 0;
			for (Object p : params)
			{
				String vn = SERVER_CALL_PARAM_PREFIX + cid + SERVER_CALL_SEPARATOR + (i++);
				if (p instanceof Boolean)
				{
					send(vn, (Boolean) p);
				}
				else if (p instanceof String)
				{
					send(vn, (String) p);
				}
				else if (p instanceof Integer)
				{
					send(vn, (Integer) p);
				}
				else if (p instanceof Float)
				{
					send(vn, (Float) p);
				}
				else if (p instanceof Map<?, ?>)
				{
					send(vn, (Map<String, Object>) p);
				}
			}
		}

		public int size()
		{
			return data.size();
		}
	}

	/**
	 * Force pending variable changes to be sent to the server.
	 * 
	 */
	public void forceSync()
	{
		d("Force sync");
		appConn.sendPendingVariableChanges();
	}

	/**
	 * Sync to the server.
	 * 
	 * Only syncs, if the {@link #isImmediate()} is false.
	 */
	private void sync()
	{
		if (isImmediate())
		{
			d("Server sync");
			appConn.sendPendingVariableChanges();
		}
	}

	/**
	 * Request server to send the client-side initialization data.
	 * 
	 */
	public void requestInit()
	{
		if (id == null)
		{
			// Too early to request. Just check what the init brings up.
			clientInitializationOk = false;
		}
		else
		{
			d("Requesting client init.");
			send(CLIENT_INIT, true, true);
		}
	}

	/**
	 * Invoke a named method at the server-side component.
	 * 
	 * This is a single transaction.
	 * 
	 * @param method
	 * @param params
	 */
	public void call(String method, Object... params)
	{
		// Calls always in a single transaction
		Transcation t = startTx();
		tx.call(method, params);
		t.commit();
	}

	/**
	 * Update the state from the server.
	 * 
	 * This is the counterpart for {@link ServerSideProxy#paintContent(com.vaadin.terminal.PaintTarget)}
	 * 
	 * @param uidl
	 * @param client
	 */
	public void update(Widget widget, UIDL uidl, ApplicationConnection client)
	{

		// Default Vaadin client-side caching handler
		if (client.updateComponent(widget, uidl, true))
		{
			return;
		}

		// This is the page (re-)load
		boolean pageLoad = getId() == null;

		// Ensure the communication module itself
		initCommunication(client, uidl);

		// Server sends a special attribute if it thinks the client should
		// have
		// already been initialized.
		boolean shouldBeInitialized = uidl.hasAttribute(SERVER_HAS_SENT_THE_INIT);

		// Handle special case of page load
		if (pageLoad && shouldBeInitialized)
		{

			// Server thinks we are initialized: Request re-initialization
			setClientInitialized(false);
		}

		// Create a transaction to bundle/postpone all variable changes during
		// init/method calls
		updateTx = startTx();
		try
		{

			// Call init. This might be re-init too.
			invokeInit(uidl.getChildByTagName("cl"));

			// Call all methods, if we are initialized. Otherwise put the calls
			// to queue.
			if (!isInitializing() && isClientInitialized())
			{
				processCallQueue();
				invokeMethods(uidl.getChildByTagName("cl"));
			}
			else
			{
				queueMethods(uidl.getChildByTagName("cl"));
			}

			// If the client is still missing the init ask for it.
			if (!isInitializing() && !isClientInitialized())
			{
				updateTx.send(CLIENT_INIT, true);
			}

		}
		finally
		{
			// Copy before commit. Commit fails if updateTx != null.
			Transcation tmptx = updateTx;
			updateTx = null;
			tmptx.commit();
		}
	}

	private boolean isInitializing()
	{
		return widgetInitializing;
	}

	/**
	 * Notify that client-side widget initialization is ready.
	 * 
	 * This method should be called if asynchronous initialization is made in {@link ClientSideHandler#initWidget(Object[])}.
	 * 
	 * This method is responsible to to process all pending requests.
	 * 
	 */
	public void clientInitComplete()
	{
		setClientInitialized(true);
		processCallQueue();
	}

	/**
	 * Process all methods left pending because of missing init.
	 * 
	 */
	private void processCallQueue()
	{
		if (pendingCalls != null)
		{
			callMethods(pendingCalls);
		}
		pendingCalls = null;

	}

	/**
	 * Is the client-side widget initialized.
	 * 
	 * @return
	 */
	public boolean isClientInitialized()
	{
		return clientInitializationOk;
	}

	/**
	 * Mark the client-side widget as initialized.
	 * 
	 * @param initOk
	 */
	private void setClientInitialized(boolean initOk)
	{
		clientInitializationOk = initOk;
		widgetInitializing = false;
	}
}
