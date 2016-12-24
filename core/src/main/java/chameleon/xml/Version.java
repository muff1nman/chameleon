/*
 * Copyright (c) 2008, Christophe Delory
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY CHRISTOPHE DELORY ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CHRISTOPHE DELORY BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package chameleon.xml;

import java.io.Serializable;

/**
 * A generic version information, composed of a version number, a revision number and a step number.
 * @version $Revision: 92 $
 * @author Christophe Delory
 * @castor.class xml="version" verify-constructable="false"
 */
public class Version implements Cloneable, Serializable
{
    /**
     * The serialization runtime associates with each serializable class a version number, called a serialVersionUID,
     * which is used during deserialization to verify that the sender and receiver of a serialized object have loaded classes for that object
     * that are compatible with respect to serialization.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The current version of this project.
     * Defaults to "0.0.0".
     */
    public static Version CURRENT = new Version("0.0.0");

    public Version(String version) {
        this._version = version;
    }

    public static Version valueOf(final String name)
    {
        return new Version(name.trim()); // May throw IndexOutOfBoundsException.
    }

    private String _version;

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone(); // Should not throw CloneNotSupportedException.
    }

    /**
     * Returns a string representation of this version information.
     * It takes the following form: "<code>&lt;version&gt;.&lt;revision&gt;.&lt;step&gt;</code>".
     * @return a string representing this version information.
     */
    @Override
    public String toString()
    {
        return _version;
    }
}
