package org.vaadin.rpc.shared;

import java.io.Serializable;

/**
 * Generic method handler interface. This interface is used both client and
 * server side to receive / handle remote calls.
 *
 * @author Sami Ekblad / Vaadin
 *
 */
public interface Method extends Serializable{

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