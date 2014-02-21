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
import org.codehaus.groovy.runtime.MethodClosure;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * TODO Add JavaDocs TODO Revisit location in the correct package
 *
 * @author Corinne Krych
 * @author Fabrice Matrat
 */


public class GroovyAccessControl {
    static final String CLASS_SEPARATOR = "#";

    // methods for a given receiver, syntax like MyReceiver.myMethod
    private final List<String> methodsOnReceiverWhitelist;
    private final List<String> methodsOnReceiverBlacklist;
    private final Map<String, List<List<String>>> binaryOperatorWhiteList;
    private final Map<String, List<List<String>>> binaryOperatorBlackList;
    private final List<String> methodPointersOnReceiverWhitelist;
    private final List<String> methodPointersOnReceiverBlacklist;
    private final List<String> propertiesWhiteList;
    private final List<String> propertiesBlackList;

    public GroovyAccessControl(
            List<String> methodWhitelist,
            List<String> methodBlacklist,
            List<String> methodPointerWhitelist,
            List<String> methodPointerBlacklist,
            Map<String, List<List<String>>> binaryWhiteList,
            Map<String, List<List<String>>> binaryBlackList,
            List<String> propertiesWhiteList,
            List<String> propertiesBlackList) {

        if (methodWhitelist != null) {
            this.methodsOnReceiverWhitelist = Collections.unmodifiableList(methodWhitelist);
        } else {
            this.methodsOnReceiverWhitelist = null;
        }
        if (methodBlacklist != null) {
            this.methodsOnReceiverBlacklist = Collections.unmodifiableList(methodBlacklist);
        } else {
            this.methodsOnReceiverBlacklist = null;
        }


        if (methodPointerWhitelist != null) {
            this.methodPointersOnReceiverWhitelist = Collections.unmodifiableList(methodPointerWhitelist);
        } else {
            this.methodPointersOnReceiverWhitelist = null;
        }
        if (methodPointerBlacklist != null) {
            this.methodPointersOnReceiverBlacklist = Collections.unmodifiableList(methodPointerBlacklist);
        } else {
            this.methodPointersOnReceiverBlacklist = null;
        }
        if (binaryWhiteList != null) {
            this.binaryOperatorWhiteList = Collections.unmodifiableMap(binaryWhiteList);
        } else {
            this.binaryOperatorWhiteList = null;
        }
        if (binaryBlackList != null) {
            this.binaryOperatorBlackList = Collections.unmodifiableMap(binaryBlackList);
        } else {
            this.binaryOperatorBlackList = null;
        }

        if (propertiesWhiteList != null) {
            this.propertiesWhiteList = Collections.unmodifiableList(propertiesWhiteList);
        } else {
            this.propertiesWhiteList = null;
        }
        if (propertiesBlackList != null) {
            this.propertiesBlackList = Collections.unmodifiableList(propertiesBlackList);
        } else {
            this.propertiesBlackList = null;
        }


    }

    public Object checkCall(Object receiver, String methodName, Object[] args, Closure closure) {
        if (receiver != null) {
            if (receiver instanceof Class) {
                if (methodName.equals("new")) {
                    String ctor = findConstructorForClass(receiver.getClass(), null);
                    if (methodsOnReceiverBlacklist != null && methodsOnReceiverBlacklist.contains(((Class) receiver).getName() + CLASS_SEPARATOR + ctor)) {
                        throw new SecurityException(((Class) receiver).getName() + CLASS_SEPARATOR + ctor + " is not allowed ...........");
                    }
                } else {
                    //For static method
                    String methodFromReceiver = isCallOnObjectReceiverAllowed(receiver.getClass(), methodName, args, closure);
                    if (methodFromReceiver != null) {
                        if (methodsOnReceiverBlacklist != null && methodsOnReceiverBlacklist.contains(methodFromReceiver)) {
                            throw new SecurityException(methodFromReceiver + " is not allowed ...........");
                        }
                    }
                    String methodFromClass = isCallOnObjectReceiverAllowed((Class) receiver, methodName, args, closure);
                    if (methodFromClass != null) {
                        if (methodsOnReceiverBlacklist != null && methodsOnReceiverBlacklist.contains(methodFromClass)) {
                            throw new SecurityException(methodFromClass + " is not allowed ...........");
                        }
                        if (methodsOnReceiverWhitelist != null && !methodsOnReceiverWhitelist.contains(methodFromClass)) {
                            throw new SecurityException(methodFromClass + " is not allowed ...........");
                        }
                    }
                    if ((methodFromReceiver != null && methodFromClass == null) && methodsOnReceiverWhitelist != null) {
                        throw new SecurityException(methodFromReceiver + " is not allowed ...........");
                    }
                }
            } else {
                String method = isCallOnObjectReceiverAllowed(receiver.getClass(), methodName, args, closure);
                if (method != null) {
                    if (methodsOnReceiverBlacklist != null && methodsOnReceiverBlacklist.contains(method)) {
                        throw new SecurityException(method + " is not allowed ...........");
                    }
                    if (methodsOnReceiverWhitelist != null && !methodsOnReceiverWhitelist.contains(method)) {
                        throw new SecurityException(method + " is not allowed ...........");
                    }
                }
            }
        }
        return closure.call(receiver, methodName, args);
    }

    private String isCallOnObjectReceiverAllowed(Class receiver, String methodName, Object[] args, Closure closure) {
        String clazz = findClassForMethod(receiver, methodName, null);
        if (clazz != null) {
            return clazz + CLASS_SEPARATOR + methodName;
        }
        return null;
    }

    private String findConstructorForClass(Class<?> clazz, Class<?>... paramTypes) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        if (constructors.length > 0) {
            return "new";
        }
        return null;
        //&& (paramTypes == null || Arrays.equals(paramTypes, method.getParameterTypes()))) {
    }

    public String findClassForMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        Class<?> searchType = clazz;
        while (searchType != null) {
            Method[] methods = (searchType.isInterface() ? searchType.getMethods() : searchType.getDeclaredMethods());
            for (Method method : methods) {
                if (name.equals(method.getName())) {
                    //&& (paramTypes == null || Arrays.equals(paramTypes, method.getParameterTypes()))) {
                    return searchType.getName();
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }


    private Class extractClassForReceiver(Object receiver) {
        if (receiver != null) {
            if (receiver instanceof Class) {
                return ((Class) receiver);
            } else {
                return receiver.getClass();
            }
        }
        return null;
    }

    public Object checkMethodPointerDeclaration(Object receiver, String methodCall) {
        Class toto = extractClassForReceiver(receiver);
        String clazz = (toto != null) ? toto.getName() : "null";

        if (methodPointersOnReceiverBlacklist != null) {
            if (methodPointersOnReceiverBlacklist.contains(clazz + CLASS_SEPARATOR + methodCall)) {
                throw new SecurityException(clazz + CLASS_SEPARATOR + methodCall + " is not allowed ...........");
            }
        }
        if (methodPointersOnReceiverWhitelist != null) {
            if (!methodPointersOnReceiverWhitelist.contains(clazz + CLASS_SEPARATOR + methodCall)) {
                throw new SecurityException(clazz + CLASS_SEPARATOR + methodCall + " is not allowed ...........");
            }
        }

        return new MethodClosure(receiver, methodCall);
    }

    public Object checkBinaryExpression(String token, Object left, Object right, Closure closure) {
        String clazzLeft = left == null ? "null" : left.getClass().getName();
        String clazzRight = right == null ? "null" : right.getClass().getName();
        if (binaryOperatorBlackList != null) {
            if (binaryOperatorBlackList.containsKey(token)) {
                List<List<String>> list = binaryOperatorBlackList.get(token);
                for (List<String> tuple : list) {
                    //TODO add uni test for list size==2
                    if (tuple.get(0).equals(clazzLeft) && tuple.get(1).equals(clazzRight)) {
                        throw new SecurityException(clazzLeft + " " + token + " " + clazzRight + " is not allowed ...........");
                    }
                }
            }
        }

        if (binaryOperatorWhiteList != null) {
            boolean found = false;
            if (binaryOperatorWhiteList.containsKey(token)) {
                List<List<String>> list = binaryOperatorWhiteList.get(token);

                for (List<String> tuple : list) {
                    //TODO add uni test for list size==2
                    if (tuple.get(0).equals(clazzLeft) && tuple.get(1).equals(clazzRight)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                throw new SecurityException(clazzLeft + " " + token + " " + clazzRight + " is not allowed ...........");
            }
        }


        return closure.call(left, right);
    }

    public Object checkSetPropertyExpression(String token, Object left, Object right, Closure closure) {
        //methodsOnReceiverBlacklist.contains(left.getClass() + ".set");
        return closure.call(left, right);
    }

    public Object checkPropertyNode(Object receiver, String name, boolean attribute, Closure closure) {
        Class toto = extractClassForReceiver(receiver);
        String clazz = (toto != null) ? toto.getName() : "null";
        String id = clazz + CLASS_SEPARATOR + (attribute ? "@" : "") + name;
        if (propertiesBlackList != null && propertiesBlackList.contains(id)) {
            throw new SecurityException(id + " is not allowed ...........");
        }
        if (propertiesWhiteList != null && !propertiesWhiteList.contains(id)) {
            throw new SecurityException(id + " is not allowed ...........");
        }
        return closure.call(receiver, name);
    }

}
