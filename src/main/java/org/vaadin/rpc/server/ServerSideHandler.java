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