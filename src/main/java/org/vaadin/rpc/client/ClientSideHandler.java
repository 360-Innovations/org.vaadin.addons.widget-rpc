package org.vaadin.rpc.client;

import java.io.Serializable;

/**
 * Interface for receiving / handling server-side calls.
 *
 * @author Sami Ekblad / Vaadin
 *
 */
public interface ClientSideHandler extends Serializable{

    /**
     * Invoked when client-side widget should be initialized.
     *
     * If an asynchronous init is made the handler must call the initComplete
     * method to notify about the it.
     *
     * @param params
     *            Initialization parameters sent from the server.
     * @return true if init will be made asynchronously, false if init is
     *         complete already,
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