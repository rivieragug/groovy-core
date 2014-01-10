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
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.classgen.VariableScopeVisitor;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This customizer allows securing source code at runtime by controlling what code constructs are allowed. For example,
 * if you only...
 * <p>
 *
 * @author Fabrice Matrat
 * @author Corinne Krych
 * @since 2.x.x
 */
public class SecureRuntimeASTCustomizer extends SecureASTCustomizer {
    @Override
    public void call(SourceUnit sourceUnit, GeneratorContext generatorContext, ClassNode classNode) throws CompilationFailedException {
//        VariableExpression whiteList = new VariableExpression("whitelist", new ClassNode(ArrayList.class));
//        ConstructorCallExpression ctorWhiteList =  new ConstructorCallExpression(new ClassNode(ArrayList.class), new ArgumentListExpression());
//        DeclarationExpression constructionExpression = new DeclarationExpression(whiteList, Token.newSymbol("=", -1, -1), ctorWhiteList);
//        ExpressionStatement ctorStmt = new ExpressionStatement(constructionExpression);
//
//        ArgumentListExpression args = new ArgumentListExpression();
//        args.addExpression(new ConstantExpression("Foo.bar"));
//        MethodCallExpression addExp = new MethodCallExpression(whiteList, "add", args);
//        ExpressionStatement addStmt = new ExpressionStatement(addExp);
//
//
//        //new StaticMethodCallExpression(new ClassNode(Collections.class), "unmodifiableList", whiteList)
//
//        ArgumentListExpression args2 = new ArgumentListExpression();
//        args2.addExpression();
//        args2.addExpression(new ConstantExpression("null"));
//        ConstructorCallExpression right =  new ConstructorCallExpression(new ClassNode(GroovyAccessControl.class), args2);

        // insert at beginning of script
        BlockStatement block = sourceUnit.getAST().getStatementBlock();


//        List<Statement> myStatements = block.getStatements();
//        myStatements.add(0, ctorStmt);
//        myStatements.add(1, addStmt);

        new RuntimeSecureVisitor(sourceUnit).visitBlockStatement(block);

        classNode.addField("groovyAccessControl", MethodNode.ACC_PRIVATE | MethodNode.ACC_FINAL, new ClassNode(GroovyAccessControl.class), new ConstructorCallExpression(new ClassNode(GroovyAccessControl.class), new ArgumentListExpression()));

        VariableScopeVisitor scopeVisitor = new VariableScopeVisitor(sourceUnit);
        scopeVisitor.visitClass(classNode);
    }

    public class GroovyAccessControl {

        // methods for a given receiver, syntax like MyReceiver.myMethod
        private final List<String> methodsOnReceiverWhitelist;
        private final List<String> methodsOnReceiverBlacklist;

        public GroovyAccessControl() {
            System.out.println("in default constructor");
            this.methodsOnReceiverWhitelist = new ArrayList<String>();
            methodsOnReceiverWhitelist.add("Script1.println");
            this.methodsOnReceiverBlacklist = null;
        }

        public GroovyAccessControl(List<String> whitelist, List<String> blacklist) {
            this.methodsOnReceiverWhitelist = Collections.unmodifiableList(whitelist);
            this.methodsOnReceiverBlacklist = Collections.unmodifiableList(blacklist);
        }

        // TODO arguments list
        public Object checkCall(Object object, String methodCall, Closure closure) {
            System.out.println("Inside checkCall" + object.getClass() + "." + methodCall);
            if (methodsOnReceiverBlacklist != null) {
                if(methodsOnReceiverBlacklist.contains(object.getClass().getName() + "." + methodCall)) {
                    throw new SecurityException(object.getClass().getName() + "." + methodCall + " is not allowed");
                }
            }
            if (methodsOnReceiverWhitelist != null) {
                if(!methodsOnReceiverWhitelist.contains(object.getClass().getName() + "." + methodCall)) {
                    throw new SecurityException(object.getClass().getName() + "." + methodCall + " is not allowed");
                }
            }
            if (closure != null) {
                return closure.call();
            } else return null;


        }
        public Object checkCall(Class clazz, String methodCall, Closure closure) {
            System.out.println("Inside checkCallClazz" + clazz + "." + methodCall);
            return closure.call();
        }



    }

    private class RuntimeSecureVisitor extends ClassCodeExpressionTransformer {
        private SourceUnit sourceUnit;

        public RuntimeSecureVisitor(SourceUnit sourceUnit) {
            super();
            this.sourceUnit = sourceUnit;
        }

        @Override
        protected SourceUnit getSourceUnit() {
            return sourceUnit;
        }

        @Override
        public Expression transform(Expression exp) {
            if(exp instanceof MethodCallExpression) {
                MethodCallExpression expression = (MethodCallExpression)exp;

                BlockStatement blockStatement = new BlockStatement();
                ExpressionStatement expressionStatement = new ExpressionStatement(expression);

                blockStatement.addStatement(expressionStatement);
                ClosureExpression closureExpression = new ClosureExpression(null, blockStatement);
                ArgumentListExpression arguments = new ArgumentListExpression();
                arguments.addExpression(expression.getObjectExpression());
                arguments.addExpression(expression.getMethod());
                arguments.addExpression(closureExpression);

                return new MethodCallExpression(new VariableExpression("groovyAccessControl", new ClassNode(GroovyAccessControl.class)), "checkCall", arguments);
            } /*else if(exp instanceof StaticMethodCallExpression) {
            StaticMethodCallExpression expression = (StaticMethodCallExpression)exp;
            ArgumentListExpression arguments = new ArgumentListExpression();
            arguments.addExpression(expression.getReceiver());
            arguments.addExpression(expression.getMethod());
            BlockStatement blockStatement = new BlockStatement();
            ExpressionStatement expressionStatement = new ExpressionStatement(expression);
            blockStatement.addStatement(expressionStatement);
            ClosureExpression closureExpression = new ClosureExpression(null, blockStatement);
            closureExpression.setVariableScope(new VariableScope());
            arguments.addExpression(closureExpression);
            return new StaticMethodCallExpression(new ClassNode(GroovyAccessControl.class), "checkCall", arguments);
        }*/
            return exp;
        }
    }


}

