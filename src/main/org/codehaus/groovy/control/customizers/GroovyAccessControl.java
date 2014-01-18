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
import org.codehaus.groovy.syntax.Token;

import java.util.*;

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
    private final Map<String,List<List<String>>> binaryOperatorWhiteList;
    private final Map<String,List<List<String>>> binaryOperatorBlackList;
    private final List<String> methodPointersOnReceiverWhitelist;
    private final List<String> methodPointersOnReceiverBlacklist;

    public GroovyAccessControl(ArrayList methodWhitelist, ArrayList methodBlacklist, ArrayList methodPointerWhitelist, ArrayList methodPointerBlacklist,Map binaryWhiteList, Map binaryBlackList) {
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

    }

    public Object checkCall(String clazz, String methodCall, Closure closure) {
        System.out.println ("I am here" + clazz + "." + methodCall);
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
        if(object != null) {
            return checkCall(object.getClass().getName(), methodCall, closure);
        } else {
            return checkCall(org.codehaus.groovy.runtime.NullObject.class.getName(), methodCall, closure);
        }
        //return true;
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

    public Object checkBinaryExpression(String token, Object left, Object right, Closure closure){
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
            if(binaryOperatorWhiteList.containsKey(token)){
                List<List<String>> list = binaryOperatorWhiteList.get(token);
                boolean found =  false;
                for(List<String> tuple : list){
                    //TODO add uni test for list size==2
                    if(tuple.get(0).equals(clazzLeft) && tuple.get(1).equals(clazzRight)){
                        found = true;
                        break;
                    }
                }
                if (! found) {
                    throw new SecurityException(clazzLeft + " " + token + " "  + clazzRight+ " is not allowed ..........." );
                }
            }
        }


        return closure.call();
    }


}
