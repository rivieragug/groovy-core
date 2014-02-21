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
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.MethodClosure;

import java.lang.reflect.Constructor;
import java.util.*;
import java.lang.reflect.Method;

/**
 * TODO Add JavaDocs
 * TODO Revisit location in the correct package
 *
 * @author Corinne Krych
 * @author Fabrice Matrat
 *
 */


public final class GroovyAccessControl {
    // methods for a given receiver, syntax like MyReceiver.myMethod
    private final List<String> methodsOnReceiverWhitelist;
    private final List<String> methodsOnReceiverBlacklist;
    private final Map<String,List<List<String>>> binaryOperatorWhiteList;
    private final Map<String,List<List<String>>> binaryOperatorBlackList;
    private final List<String> methodPointersOnReceiverWhitelist;
    private final List<String> methodPointersOnReceiverBlacklist;
    private final List<String> propertiesWhiteList;
    private final List<String> propertiesBlackList;

    public GroovyAccessControl(ArrayList methodWhitelist, ArrayList methodBlacklist, ArrayList methodPointerWhitelist, ArrayList methodPointerBlacklist,Map binaryWhiteList, Map binaryBlackList, ArrayList propertiesWhiteList, ArrayList propertiesBlackList) {
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
        if(binaryWhiteList != null){
            this.binaryOperatorWhiteList = Collections.unmodifiableMap(binaryWhiteList);
        }
        else {
            this.binaryOperatorWhiteList = null;
        }
        if(binaryBlackList != null){
            this.binaryOperatorBlackList = Collections.unmodifiableMap(binaryBlackList);
        }
        else {
            this.binaryOperatorBlackList = null;
        }

        if(propertiesWhiteList != null){
            this.propertiesWhiteList = Collections.unmodifiableList(propertiesWhiteList);
        }
        else {
            this.propertiesWhiteList = null;
        }
        if(propertiesBlackList != null){
            this.propertiesBlackList = Collections.unmodifiableList(propertiesBlackList);
        }
        else {
            this.propertiesBlackList = null;
        }


    }

    public final Object checkCall(final Object receiver, final String methodName, final Object[] args, final Closure closure) {
        if(receiver != null) {
            if (receiver instanceof Class) {
                if (methodName.equals("new")) {
                    String ctor = findConstructorForClass(receiver.getClass(), null);
                    if (methodsOnReceiverBlacklist != null && methodsOnReceiverBlacklist.contains(((Class) receiver).getName() + "." + ctor)) {
                        throw new SecurityException(((Class) receiver).getName() + "." + ctor + " is not allowed ...........");
                    }
                } else {
                    //For static method
                    String methodFromReceiver = isCallOnObjectReceiverAllowed(receiver.getClass(), methodName, args, closure);
                    if(methodFromReceiver != null) {
                        if (methodsOnReceiverBlacklist != null && methodsOnReceiverBlacklist.contains(methodFromReceiver)) {
                            throw new SecurityException(methodFromReceiver + " is not allowed ...........");
                        }
                    }
                    String methodFromClass = isCallOnObjectReceiverAllowed((Class)receiver, methodName, args, closure);
                    if(methodFromClass != null) {
                        if (methodsOnReceiverBlacklist != null && methodsOnReceiverBlacklist.contains(methodFromClass)) {
                            throw new SecurityException(methodFromClass + " is not allowed ...........");
                        }
                        if (methodsOnReceiverWhitelist != null && !methodsOnReceiverWhitelist.contains(methodFromClass)) {
                            throw new SecurityException(methodFromClass + " is not allowed ...........");
                        }
                    }
                    if((methodFromReceiver != null && methodFromClass == null) && methodsOnReceiverWhitelist != null) {
                        throw new SecurityException(methodFromReceiver + " is not allowed ...........");
                    }
                }
            } else {
                String method = isCallOnObjectReceiverAllowed(receiver.getClass(), methodName, args, closure);
                if(method != null) {
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

    private String isCallOnObjectReceiverAllowed(final Class receiver, final String methodName, final Object[] args, final Closure closure) {
        String clazz = findClassForMethod(receiver, methodName, null);
        if (clazz != null) {
            return clazz + "." + methodName;
        }
        return null;
    }

    private String findConstructorForClass(Class<?> clazz, Class<?>... paramTypes) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        if(constructors.length > 0) {
            return "new";
        }
        return null;
        //&& (paramTypes == null || Arrays.equals(paramTypes, method.getParameterTypes()))) {
    }

    private String findClassForMethod(final Class<?> clazz, final String name, final Class<?>... paramTypes) {
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

    public final Object checkMethodPointerDeclaration(final Object receiver, final String methodCall) {
        Class toto = extractClassForReceiver(receiver);
        String clazz = (toto != null) ? toto.getName() : "null";

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

        return new MethodClosure(receiver ,methodCall);
    }

    public final Object checkBinaryExpression(final String token, final Object left, final Object right, final Closure closure){
        String clazzLeft = left== null ? "null" : left.getClass().getName();
        String clazzRight = right== null ? "null" : right.getClass().getName();
        if(binaryOperatorBlackList != null) {
            if(binaryOperatorBlackList.containsKey(token)){
                List<List<String>> list = binaryOperatorBlackList.get(token);
                for(List<String> tuple : list){
                    //TODO add uni test for list size==2
                    if(tuple.get(0).equals(clazzLeft) && tuple.get(1).equals(clazzRight)){
                        throw new SecurityException(clazzLeft + " " + token + " "  + clazzRight+ " is not allowed ..........." );
                    }
                }
            }
        }

        if(binaryOperatorWhiteList != null) {
            boolean found =  false;
            if(binaryOperatorWhiteList.containsKey(token)){
                List<List<String>> list = binaryOperatorWhiteList.get(token);

                for(List<String> tuple : list){
                    //TODO add uni test for list size==2
                    if(tuple.get(0).equals(clazzLeft) && tuple.get(1).equals(clazzRight)){
                        found = true;
                        break;
                    }
                }
            }
            if (! found) {
                throw new SecurityException(clazzLeft + " " + token + " "  + clazzRight+ " is not allowed ..........." );
            }
        }


        return closure.call(left, right);
    }

    public final Object checkPropertyNode(final Object receiver, final String name, final Closure closure) {
        Class receiverClass = extractClassForReceiver(receiver);
        String clazz = (receiverClass != null) ? receiverClass.getName() : "null";
        if (propertiesBlackList != null) {
            if(propertiesBlackList.contains(clazz + "." + name)) {
                throw new SecurityException(clazz + "." + name + " is not allowed ...........");
            }
        }
        if (propertiesWhiteList != null) {
            if(!propertiesWhiteList.contains(clazz + "." + name)) {
                throw new SecurityException(clazz + "." + name + " is not allowed ...........");
            }
        }
        return closure.call(receiver, name);
    }

}
