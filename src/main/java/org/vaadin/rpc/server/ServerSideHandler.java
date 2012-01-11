package org.vaadin.rpc.server;

import java.io.Serializable;

/**
 * Interface for receiver/handler for method invocations from the client-side.
 * 
 * @author Sami Ekblad / Vaadin
 * 
 */
public interface ServerSideHandler extends Serializable
{

	/**
	 * Invoked when client-side request full initialization.
	 * 
	 * @return Initialization parameters to the client. These should be handled in {@link org.vaadin.rpc.client.ClientSideHandler.#initWidget(Object[])}.
	 */
	public Object[] initRequestFromClient();

	/**
	 * Invoked to handle a method call from the client-side.
	 * 
	 * @param method
	 * @param params
	 */
	public void callFromClient(String method, Object[] params);

	/**
	 * Invoked to notify the underlying component repaint is needed. This is used to pass the information to the right (hosting) Vaadin component to notify the
	 * Vaadin about changes to the client.
	 * 
	 * Typically only calls the component's own {@link com.vaadin.ui.Component#requestRepaint()} .
	 */
	public void requestRepaint();
}