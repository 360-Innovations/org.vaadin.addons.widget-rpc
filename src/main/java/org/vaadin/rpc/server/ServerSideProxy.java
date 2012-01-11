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
package org.vaadin.rpc.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vaadin.rpc.shared.Method;

import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.terminal.Resource;

/**
 * This is a server-side class that implements methods to invoke client-side methods.
 * 
 * This hides all the bookkeeping needed to implement a higly-interactive Vaadin widgets that communicate a lot with the associated server-side Component.
 * 
 * Using this class components can call registered handlers from the client-side using methods {@link #call(String, Object...)} and
 * {@link #callOnce(String, Object...)} during the lifecycle of the widget.
 * 
 * When the client-side widget needs to be intialized the {@link ServerSideHandler} is used.
 * 
 * @author Sami Ekblad / Vaadin
 * 
 */
public class ServerSideProxy implements Serializable
{

	public static final int PARAM_STRING = 0;
	public static final int PARAM_BOOLEAN = 1;
	public static final int PARAM_INT = 2;
	public static final int PARAM_FLOAT = 3;
	public static final int PARAM_MAP = 4;
	public static final int PARAM_RESOURCE = 5;

	private static final String SERVER_CALL_PREFIX = "c_";
	private static final String SERVER_CALL_PARAM_PREFIX = "p_";
	private static final String SERVER_CALL_SEPARATOR = "_";
	private static final String SERVER_HAS_SENT_THE_INIT = "_si";

	private static final long serialVersionUID = 4687944475579171126L;
	private static final String CLIENT_INIT = "_init";

	private final ServerSideHandler handler;
	private final List<Object[]> clientCallQueue = new ArrayList<Object[]>();
	private final Map<String, Method> methods = new HashMap<String, Method>();

	private Object[] clientInitParams;
	private boolean initSent;

	public ServerSideProxy(final ServerSideHandler handler)
	{
		this.handler = handler;
		initSent = false;
	}

	private void receiveFromClient(Map<String, Object> variables)
	{
		// Handle init request first
		if (variables.containsKey(CLIENT_INIT) && clientInitParams == null)
		{
			initClientWidget(handler.initRequestFromClient());
		}

		// Other calls
		for (String n : variables.keySet())
		{
			if (n.startsWith(SERVER_CALL_PREFIX))
			{
				String cidStr = n.substring(SERVER_CALL_PREFIX.length(), n.indexOf(SERVER_CALL_SEPARATOR, SERVER_CALL_PREFIX.length() + 1));
				int cid = Integer.parseInt(cidStr);
				n = n.substring(SERVER_CALL_PREFIX.length() + ("" + cid).length() + 1);
				List<Object> params = new ArrayList<Object>();
				int i = 0;
				String pn = SERVER_CALL_PARAM_PREFIX + cid + SERVER_CALL_SEPARATOR + i;
				while (variables.containsKey(pn))
				{
					params.add(variables.get(pn));
					pn = SERVER_CALL_PARAM_PREFIX + cid + SERVER_CALL_SEPARATOR + (++i);
				}

				Method m = methods.get(n);
				if (m != null)
				{
					m.invoke(n, params.toArray());
				}
				else
				{
					// Default handler
					handler.callFromClient(n, params.toArray());
				}

			}
		}
	}

	/**
	 * Register a method handler for client-driven calls.
	 * 
	 * @param methodName
	 * @param method
	 */
	public void register(String methodName, Method method)
	{
		methods.put(methodName, method);
	}

	/**
	 * Call a name method from the client side widget.
	 * 
	 * @param method
	 * @param params
	 */
	public void call(String method, Object... params)
	{
		Object[] call = new Object[params.length + 1];
		call[0] = method;
		for (int i = 0; i < params.length; i++)
		{
			call[i + 1] = params[i];
		}
		synchronized (clientCallQueue)
		{
			clientCallQueue.add(call);
		}
		handler.requestRepaint();
	}

	/**
	 * Call a name method from the client side widget.
	 * 
	 * This method first check if there is a pending call to the same method and cancels them.
	 * 
	 * @param method
	 * @param params
	 */
	public void callOnce(String method, Object... params)
	{
		cancelCalls(method);
		Object[] call = new Object[params.length + 1];
		call[0] = method;
		for (int i = 0; i < params.length; i++)
		{
			call[i + 1] = params[i];
		}
		synchronized (clientCallQueue)
		{
			clientCallQueue.add(call);
		}
		handler.requestRepaint();
	}

	/**
	 * Cancel all calls to the client-side.
	 * 
	 * @param methodName
	 */
	private void cancelCalls(String methodName)
	{
		synchronized (clientCallQueue)
		{
			ArrayList<Object[]> tmp = new ArrayList<Object[]>(clientCallQueue);
			for (Object[] c : tmp)
			{
				if (c[0].equals(methodName))
				{
					clientCallQueue.remove(c);
				}
			}
		}
	}

	/**
	 * This is the method for sending the data to the client-side widget.
	 * 
	 * It should be called from the hosting components {@link com.vaadin.ui.AbstractComponent#paintContent(PaintTarget)} method.
	 * 
	 * @param target
	 * @throws PaintException
	 */
	public void paintContent(PaintTarget target) throws PaintException
	{

		// Ask init 1) when explicitly asked 2) when no client calls has been
		// made AND no pending init data is available
		if (!initSent)
		{

			// Ask for init params, if not set yet
			if (clientInitParams == null)
			{
				initClientWidget(handler.initRequestFromClient());
			}
			initSent = true;
		}
		else
		{
			// Notify client that init data should have been there already
			target.addAttribute(SERVER_HAS_SENT_THE_INIT, true);
		}

		target.startTag("cl");

		// Paint init first
		if (clientInitParams != null)
		{
			target.startTag("c");
			target.addAttribute("n", CLIENT_INIT);
			paintCallParameters(target, clientInitParams, 0);
			target.endTag("c");
			clientInitParams = null;
		}

		synchronized (clientCallQueue)
		{
			try
			{
				ArrayList<Object[]> tmpCalls = new ArrayList<Object[]>(clientCallQueue); // copy
				for (Object[] aCall : tmpCalls)
				{
					target.startTag("c");
					target.addAttribute("n", (String) aCall[0]);
					paintCallParameters(target, aCall, 1);
					target.endTag("c");
					clientCallQueue.remove(aCall);
				}
			}
			catch (Throwable e)
			{
				throw new PaintException(e.getMessage());
			}
			finally
			{
				target.endTag("cl");
			}
		}
	}

	/**
	 * Paint method parameters.
	 * 
	 * @param target
	 * @param aCall
	 * @param start
	 * @throws PaintException
	 */
	private void paintCallParameters(PaintTarget target, Object[] aCall, int start) throws PaintException
	{
		target.addAttribute("pc", aCall.length - start);
		for (int i = start; i < aCall.length; i++)
		{
			if (aCall[i] != null)
			{
				int pi = i - start; // index parameters from start
				paintCallParameter(target, aCall[i], pi);
			}
		}
	}

	/**
	 * Paint single call parameter.
	 * 
	 * @param target
	 * @param p
	 * @param pi
	 * @throws PaintException
	 */
	private void paintCallParameter(PaintTarget target, Object p, int pi) throws PaintException
	{
		if (p instanceof String)
		{
			target.addAttribute("p" + pi, (String) p);
			target.addAttribute("pt" + pi, PARAM_STRING);
		}
		else if (p instanceof Float)
		{
			target.addAttribute("p" + pi, (Float) p);
			target.addAttribute("pt" + pi, PARAM_FLOAT);
		}
		else if (p instanceof Boolean)
		{
			target.addAttribute("p" + pi, (Boolean) p);
			target.addAttribute("pt" + pi, PARAM_BOOLEAN);
		}
		else if (p instanceof Integer)
		{
			target.addAttribute("p" + pi, (Integer) p);
			target.addAttribute("pt" + pi, PARAM_INT);
		}
		else if (p instanceof Map)
		{
			target.addAttribute("p" + pi, (Map<?, ?>) p);
			target.addAttribute("pt" + pi, PARAM_MAP);
		}
		else if (p instanceof Resource)
		{
			target.addAttribute("p" + pi, (Resource) p);
			target.addAttribute("pt" + pi, PARAM_RESOURCE);
		}
	}

	/**
	 * The method for receiving data from the client.
	 * 
	 * This should be called from hosting components {@link com.vaadin.ui.Component#changeVariables(Object, Map)} method
	 * 
	 * @param source
	 * @param variables
	 */
	public void changeVariables(Object source, Map<String, Object> variables)
	{
		receiveFromClient(variables);
	}

	/**
	 * Request the client-side widget initialization.
	 * 
	 */
	public void requestClientSideInit()
	{
		initSent = false;
		handler.requestRepaint();
	}

	/**
	 * Initialize the client-side widget with given parameters.
	 * 
	 */
	private void initClientWidget(Object... params)
	{
		clientInitParams = params;
		handler.requestRepaint();
	}

	/**
	 * Get unsent calls.
	 * 
	 * @return
	 */
	// TODO: remove?
	// private List<String> getUnsentCalls() {
	// synchronized (clientCallQueue) {
	// ArrayList<String> res = new ArrayList<String>();
	// for (Object[] c : clientCallQueue) {
	// res.add((String) c[0]);
	// }
	// return res;
	// }
	// }

}
