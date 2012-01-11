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

/**
 * Interface for receiving / handling server-side calls.
 * 
 * @author Sami Ekblad / Vaadin
 * 
 */
public interface ClientSideHandler extends Serializable
{

	/**
	 * Invoked when client-side widget should be initialized.
	 * 
	 * If an asynchronous init is made the handler must call the initComplete method to notify about the it.
	 * 
	 * @param params
	 *            Initialization parameters sent from the server.
	 * @return true if init will be made asynchronously, false if init is complete already,
	 */
	public boolean initWidget(Object[] params);

	/**
	 * Handle an otherwise unhandled call from the server.
	 * 
	 * @param method
	 * @param params
	 */
	public void handleCallFromServer(String method, Object[] params);

}