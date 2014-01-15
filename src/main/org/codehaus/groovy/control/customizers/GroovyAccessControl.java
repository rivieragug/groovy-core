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

import java.util.*;

/**
 * TODO Add JavaDocs
 *
 * @author Corinne Krych
 * @author Fabrice Matrat
 *
 */


public class GroovyAccessControl {
    private final List<Checker> methodCheckers;
    public GroovyAccessControl(List<Checker> methodCheckers) {
        this.methodCheckers = Collections.unmodifiableList(methodCheckers);
    }
    public Object checkMethodCall(Object object, String methodCall, Closure closure) {
        return checkMethodCall(object.getClass().getName(), methodCall, closure);
    }

    public Object checkMethodCall(String clazz, String methodCall, Closure closure) {
        for(Checker methodChecker: methodCheckers) {
            Map map = new HashMap();
            map.put("class", clazz); map.put("method", methodCall);
            if (!methodChecker.isAllowed(map)) {
                throw new SecurityException(clazz + "." + methodCall + " is not allowed ...........");
            }
        }
        return closure.call();
    }
    //TODO public Object checkReceiver()
}
