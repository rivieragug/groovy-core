/*
 * Copyright 2003-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.groovy.control.customizers;

import groovy.lang.Closure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TODO Add JavaDocs
 * TODO Revisit location in the correct package
 *
 * @author Corinne Krych
 * @author Fabrice Matrat
 *
 */


public class GroovyAccessControl {
    // methods for a given receiver, syntax like MyReceiver.myMethod
    private final List<String> methodsOnReceiverWhitelist;
    private final List<String> methodsOnReceiverBlacklist;
    private final List<String> methodPointersOnReceiverWhitelist;
    private final List<String> methodPointersOnReceiverBlacklist;

    public GroovyAccessControl(ArrayList methodWhitelist, ArrayList methodBlacklist, ArrayList methodPointerWhitelist, ArrayList methodPointerBlacklist) {
        if(methodWhitelist != null) {
            this.methodsOnReceiverWhitelist = Collections.unmodifiableList(methodWhitelist);
        } else {
            this.methodsOnReceiverWhitelist = null;
        }
        if(methodBlacklist != null) {
            this.methodsOnReceiverBlacklist = Collections.unmodifiableList(methodBlacklist);
        } else {
            this.methodsOnReceiverBlacklist = null;
        }

        if(methodPointerWhitelist != null) {
            this.methodPointersOnReceiverWhitelist = Collections.unmodifiableList(methodPointerWhitelist);
        } else {
            this.methodPointersOnReceiverWhitelist = null;
        }
        if(methodPointerBlacklist != null) {
            this.methodPointersOnReceiverBlacklist = Collections.unmodifiableList(methodPointerBlacklist);
        } else {
            this.methodPointersOnReceiverBlacklist = null;
        }
    }

    public Object checkCall(String clazz, String methodCall, Closure closure) {
        if (methodsOnReceiverBlacklist != null) {
            if(methodsOnReceiverBlacklist.contains(clazz + "." + methodCall)) {
                throw new SecurityException(clazz + "." + methodCall + " is not allowed ...........");
            }
        }
        if (methodsOnReceiverWhitelist != null) {
            if(!methodsOnReceiverWhitelist.contains(clazz + "." + methodCall)) {
                throw new SecurityException(clazz + "." + methodCall + " is not allowed ...........");
            }
        }
        return closure.call();
    }

    public Object checkCall(Object object, String methodCall, Closure closure) {
        return checkCall(object.getClass().getName(), methodCall, closure);
    }

    public Object checkMethodPointerDeclaration(String clazz, String methodCall, Closure closure) {
        if (methodPointersOnReceiverBlacklist != null) {
            if(methodPointersOnReceiverBlacklist.contains(clazz + "." + methodCall)) {
                throw new SecurityException(clazz + "." + methodCall + " is not allowed ...........");
            }
        }
        if (methodPointersOnReceiverWhitelist != null) {
            if(!methodPointersOnReceiverWhitelist.contains(clazz + "." + methodCall)) {
                throw new SecurityException(clazz + "." + methodCall + " is not allowed ...........");
            }
        }

        return closure.call();
    }
}
