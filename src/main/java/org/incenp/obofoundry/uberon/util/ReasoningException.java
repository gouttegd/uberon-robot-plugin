/*
 * Uberon ROBOT plugin
 * Copyright © 2023,2024 Damien Goutte-Gattat
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of copyright holder nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.incenp.obofoundry.uberon.util;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

/**
 * This exception may be thrown by other classes in this package if an error
 * occurs when reasoning over an ontology (for example, if the ontology is
 * unexpectedly inconsistent).
 */
public class ReasoningException extends Exception {

    private static final long serialVersionUID = -4867170036764099132L;

    /**
     * Creates a new instance.
     * 
     * @param msg The error message.
     */
    public ReasoningException(String msg) {
        super(msg);
    }

    /**
     * Creates a new instance for cases where classes were found to be
     * unsatisfiable.
     * 
     * @param msg     The error message.
     * @param classes The list of unsatisfiable classes.
     */
    public ReasoningException(String msg, Set<OWLClass> classes) {
        super(formatMessage(msg, classes));
    }

    private static String formatMessage(String msg, Set<OWLClass> classes) {
        StringBuilder sb = new StringBuilder();
        sb.append(msg);
        sb.append(':');
        for ( OWLClass c : classes ) {
            sb.append(' ');
            sb.append(c.getIRI().toString());
        }
        return sb.toString();
    }
}
