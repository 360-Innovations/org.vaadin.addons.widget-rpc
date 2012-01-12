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
 */
package org.vaadin.rpc.shared;

import java.io.Serializable;

/**
 * Generic method handler interface. This interface is used both client and server side to receive / handle remote calls.
 * 
 * @author Sami Ekblad / Vaadin
 * 
 */
public interface Method extends Serializable
{

	/**
	 * Invoke a method by name.
	 * 
	 * @param methodName
	 *            name of the methd to invoke.
	 * @param params
	 *            Array of untyped parameters.
	 */
	void invoke(String methodName, Object[] params);
}