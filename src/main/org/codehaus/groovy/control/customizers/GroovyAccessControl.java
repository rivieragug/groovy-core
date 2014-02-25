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
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.runtime.MethodClosure;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    // contains the set of classes which are wildcarded, that is to say that anything on those
    // classes is accepted
    private final Set<String> classWildcards;

    public GroovyAccessControl(
            List<String> methodWhitelist,
            List<String> methodBlacklist,
            List<String> methodPointerWhitelist,
            List<String> methodPointerBlacklist,
            Map<String, List<List<String>>> binaryWhiteList,
            Map<String, List<List<String>>> binaryBlackList,
            List<String> propertiesWhiteList,
            List<String> propertiesBlackList,
            Set<String> classWildCards) {

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

        this.classWildcards = classWildCards==null?Collections.<String>emptySet():Collections.unmodifiableSet(classWildCards);
    }

    public Object checkCall(Object receiver, String methodName, Object[] args, Closure closure) {
        checkCallOnReceiver(receiver, methodName, args, methodsOnReceiverBlacklist, methodsOnReceiverWhitelist);
        return closure.call(receiver, methodName, args);
    }

    public Object checkMethodPointerDeclaration(Object receiver, String methodCall) {
        checkCallOnReceiver(receiver, methodCall, null, methodPointersOnReceiverBlacklist, methodPointersOnReceiverWhitelist);
        return new MethodClosure(receiver, methodCall);
    }

    public Object checkBinaryExpression(String token, Object left, Object right, Closure closure) {
        String clazzLeft = left == null ? "null" : left.getClass().getName();
        String clazzRight = right == null ? "null" : right.getClass().getName();
        if(binaryOperatorBlackList != null) {
            if(binaryOperatorBlackList.containsKey(token)){
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

    public Object checkPropertyNode(Object receiver, String name, boolean attribute, Closure closure) {
        Class<?> receiverClass = receiver.getClass();
        if (!isWildcarded(receiverClass)) {
            String clazz = findClassForProperty(receiverClass, name);
            if (clazz == null ) {
                clazz = findClassForMethod(receiverClass, "get" +  name.substring(0, 1).toUpperCase() + name.substring(1));
            }
            String id = clazz + CLASS_SEPARATOR + (attribute ? "@" : "") + name;

            if (propertiesBlackList != null && propertiesBlackList.contains(id)) {
                throw new SecurityException(id + " is not allowed ...........");
            }
            if (propertiesWhiteList != null && !propertiesWhiteList.contains(id)) {
                throw new SecurityException(id + " is not allowed ...........");
            }
        }
        return closure.call(receiver, name);
    }

    private boolean isWildcarded(Class clazz) {
        return classWildcards.contains(clazz.getName())
                || clazz.getEnclosingClass() != null && isWildcarded(clazz.getEnclosingClass());
    }

    private void checkCallOnReceiver(Object receiver, String methodName, Object[] args, List blacklist, List whitelist) {
        if(receiver != null) {
            if (isWildcarded(receiver.getClass())) {
                return;
            }
            if (receiver instanceof Class) {
                if (isWildcarded((Class) receiver)) {
                    return;
                }
                if (methodName.equals("new")) {
                    String ctor = findConstructorForClass(receiver.getClass(), null);
                    if (methodsOnReceiverBlacklist != null && methodsOnReceiverBlacklist.contains(((Class) receiver).getName() + CLASS_SEPARATOR + ctor)) {
                        throw new SecurityException(((Class) receiver).getName() + CLASS_SEPARATOR + ctor + " is not allowed ...........");
                    }
                } else {
                    //For static method
                    String methodFromReceiver = findMethodPatternForReceiver(receiver.getClass(), methodName, args);
                    if (methodFromReceiver != null) {
                        if (blacklist != null && blacklist.contains(methodFromReceiver)) {
                            throw new SecurityException(methodFromReceiver + " is not allowed ...........");
                        }
                    }
                    String methodFromClass = findMethodPatternForReceiver((Class) receiver, methodName, args);
                    if (methodFromClass != null) {
                        if (blacklist != null && blacklist.contains(methodFromClass)) {
                            throw new SecurityException(methodFromClass + " is not allowed ...........");
                        }
                        if (whitelist != null && !whitelist.contains(methodFromClass)) {
                            throw new SecurityException(methodFromClass + " is not allowed ...........");
                        }
                    }
                    if ((methodFromReceiver != null && methodFromClass == null) && whitelist != null) {
                        throw new SecurityException(methodFromReceiver + " is not allowed ...........");
                    }
                }
            } else {
                String method = findMethodPatternForReceiver(receiver.getClass(), methodName, args);
                if (method != null) {
                    if (blacklist != null && blacklist.contains(method)) {
                        throw new SecurityException(method + " is not allowed ...........");
                    }
                    if (whitelist != null && !whitelist.contains(method)) {
                        throw new SecurityException(method + " is not allowed ...........");
                    }
                }
            }
        }
    }

    private String findMethodPatternForReceiver(final Class receiver, final String methodName, final Object[] args) {
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

    private String findClassForProperty(final Class<?> clazz, final String property) {
        Class<?> searchType = clazz;
        while (searchType != null) {
            Field[] fields = searchType.getFields();
            for (Field field : fields) {
                if (property.equals(field.getName())) {
                    return searchType.getName();
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }
}
