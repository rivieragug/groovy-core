/*
 * Copyright 2003-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.groovy.control.customizers;


import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.ListExpression;

import java.util.List;

public class WhitelistRuntimeCheckerFactory implements MethodCheckerFactory {
    public ConstructorCallExpression getInstance(SecureRuntimeASTCustomizer customizer, ClassNode classNode) {
        ArgumentListExpression expression = new ArgumentListExpression();
        if(customizer.getReceiversWhiteList() != null) {
            ListExpression array = new ListExpression();
            for(String whiteListElement : customizer.getReceiversWhiteList()) {
                array.addExpression(new ConstantExpression(whiteListElement));
            }
            List<MethodNode> methods = customizer.filterMethods(classNode);
            for(MethodNode methodNode : methods) {
                array.addExpression(new ConstantExpression(methodNode.getDeclaringClass() + "." + methodNode.getName()));
            }
            // Need to add all the method of other class
            // even inner class ? or just className.*
            expression.addExpression(array);
        } else {
            expression.addExpression(ConstantExpression.NULL);
        }
        if(customizer.getReceiversBlackList() != null) {
            ListExpression array = new ListExpression();
            for(String blackListElement : customizer.getReceiversBlackList()) {
                array.addExpression(new ConstantExpression(blackListElement));
            }
            expression.addExpression(array);
        } else {
            expression.addExpression(ConstantExpression.NULL);
        }
        return new ConstructorCallExpression(new ClassNode(WhitelistRuntimeChecker.class), expression);
    }
}
