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

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.classgen.VariableScopeVisitor;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;

import java.util.*;

/**
 * TO BE FILLED
 * @author Corinne Krych
 * @author Fabrice Matrat
 * @since  2.2.1
 *
 */
public class SecureRuntimeASTCustomizer extends SecureASTCustomizer {
    // TODO move methods to compile time check
    private List<String> methodsWhiteList;
    private List<String> methodsBlackList;

    public SecureRuntimeASTCustomizer() {
        super();
    }

    public void setMethodsBlackList(final List<String> methodsBlackList) {
        if (methodsWhiteList != null) {
            throw new IllegalArgumentException("You are not allowed to set both whitelist and blacklist");
        }
        this.methodsBlackList = methodsBlackList;
    }

    public List<String> getMethodsBlackList() {
        return methodsBlackList;
    }

    public void setMethodsWhiteList(final List<String> methodsWhiteList) {
        if (methodsBlackList != null) {
            throw new IllegalArgumentException("You are not allowed to set both whitelist and blacklist");
        }
        this.methodsWhiteList = methodsWhiteList;
    }

    public List<String> getMethodsWhiteList() {
        return methodsWhiteList;
    }

    @Override
    public void call(final SourceUnit source, final GeneratorContext context, final ClassNode classNode) throws CompilationFailedException {
        super.call(source, context, classNode);

        SecuringCodeRuntimeVisitor runtimeVisitor = new SecuringCodeRuntimeVisitor(source);

        List<Statement> initializers = classNode.getObjectInitializerStatements();
        for(Statement statement : initializers) {
            statement.visit(runtimeVisitor);
        }
        List<FieldNode> fields = classNode.getFields();
        for (FieldNode field : fields) {
            runtimeVisitor.visitField(field);
        }

        for (MethodNode methodNode : classNode.getMethods()) {
            methodNode.getCode().visit(runtimeVisitor);
        }

//      if (something) {  // Want to add only once the GroovyAccessControl but is it the right place
        ArgumentListExpression expression = new ArgumentListExpression();
        if(getMethodsWhiteList() != null) {
            ListExpression array = new ListExpression();
            for(String whiteListElement : getMethodsWhiteList()) {
                array.addExpression(new ConstantExpression(whiteListElement));
            }
            List<MethodNode> methods = filterMethods(classNode);
            for(MethodNode methodNode : methods) {
                array.addExpression(new ConstantExpression(methodNode.getDeclaringClass() + "." + methodNode.getName()));
            }
            // Need to add all the method of other class
            // even inner class ? or just className.*
            expression.addExpression(array);
        } else {
            expression.addExpression(ConstantExpression.NULL);
        }
        if(getMethodsBlackList() != null) {
            ListExpression array = new ListExpression();
            for(String blackListElement : getMethodsBlackList()) {
                array.addExpression(new ConstantExpression(blackListElement));
            }
            expression.addExpression(array);
        } else {
            expression.addExpression(ConstantExpression.NULL);
        }
//      }
        classNode.addFieldFirst("groovyAccessControl", MethodNode.ACC_PROTECTED | MethodNode.ACC_FINAL | MethodNode.ACC_STATIC, new ClassNode(GroovyAccessControl.class), new ConstructorCallExpression(new ClassNode(GroovyAccessControl.class), expression));

        VariableScopeVisitor scopeVisitor = new VariableScopeVisitor(source);
        scopeVisitor.visitClass(classNode);
    }

    private class SecuringCodeRuntimeVisitor extends ClassCodeExpressionTransformer {

        private SourceUnit sourceUnit;

        public SecuringCodeRuntimeVisitor(SourceUnit source) {
            sourceUnit = source;
        }

        @Override
        public SourceUnit getSourceUnit() {
            return sourceUnit;
        }

        @Override
        public Expression transform(Expression exp) {
            if(exp instanceof ArgumentListExpression) {
                ArgumentListExpression expression = (ArgumentListExpression)exp;
                Iterator<Expression> iterator = expression.iterator();
                ArgumentListExpression arguments = new ArgumentListExpression();
                while (iterator.hasNext()) {
                    Expression exp1 = iterator.next();
                    arguments.addExpression(transform(exp1));
                }

                arguments.setSourcePosition(exp);
                return arguments;
            }

            if(exp instanceof MethodCallExpression) {
                MethodCallExpression expression = (MethodCallExpression)exp;

                ArgumentListExpression groovyAccessControlArguments = getArgumentsExpressionsForClosureCall(expression,
                        expression.getObjectExpression(),
                        expression.getMethod());

                expression.setArguments(transform(expression.getArguments()));

                MethodCallExpression methodCallExpression = new MethodCallExpression(new VariableExpression("groovyAccessControl", new ClassNode(GroovyAccessControl.class)), "checkCall", groovyAccessControlArguments);
                methodCallExpression.setSourcePosition(exp);
                return methodCallExpression;
            }

            if(exp instanceof BinaryExpression) {
                BinaryExpression expression = (BinaryExpression)exp;
                expression.setLeftExpression(transform(expression.getLeftExpression()));
                expression.setRightExpression(transform(expression.getRightExpression()));
                return expression;
            }

            if(exp instanceof StaticMethodCallExpression) {
                StaticMethodCallExpression expression = (StaticMethodCallExpression)exp;

                ArgumentListExpression newMethodCallArguments = getArgumentsExpressionsForClosureCall(expression,
                        new ConstantExpression(expression.getOwnerType().getName()),
                        new ConstantExpression(expression.getMethod()));

                expression.setArguments(transform(expression.getArguments()));

                MethodCallExpression methodCallExpression = new MethodCallExpression(new VariableExpression("groovyAccessControl", new ClassNode(GroovyAccessControl.class)), "checkCall", newMethodCallArguments);
                methodCallExpression.setSourcePosition(exp);
                return methodCallExpression;
            }

            if(exp instanceof ClosureExpression) {
                ClosureExpression expression = (ClosureExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof ConstructorCallExpression) {
                ConstructorCallExpression expression = (ConstructorCallExpression)exp;
                ArgumentListExpression newMethodCallArguments = getArgumentsExpressionsForClosureCall(expression,
                        new ConstantExpression(expression.getType().getName()),
                        new ConstantExpression("new"));

                MethodCallExpression methodCallExpression = new MethodCallExpression(new VariableExpression("groovyAccessControl", new ClassNode(GroovyAccessControl.class)), "checkCall", newMethodCallArguments);
                methodCallExpression.setSourcePosition(exp);
                return methodCallExpression;
            }

            if(exp instanceof TernaryExpression) {
                TernaryExpression expression = (TernaryExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof ElvisOperatorExpression) {
                ElvisOperatorExpression expression = (ElvisOperatorExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof PrefixExpression) {
                PrefixExpression expression = (PrefixExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof PostfixExpression) {
                PostfixExpression expression = (PostfixExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof BooleanExpression) {
                BooleanExpression expression = (BooleanExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof TupleExpression) {
                TupleExpression expression = (TupleExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof MapExpression) {
                MapExpression expression = (MapExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof MapEntryExpression) {
                MapEntryExpression expression = (MapEntryExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof ListExpression) {
                ListExpression expression = (ListExpression)exp;
                List<Expression> list = expression.getExpressions();
                ListExpression transformed = new ListExpression();
                for(Expression ex1 : list) {
                    transformed.addExpression(transform(ex1));
                }
                transformed.setSourcePosition(expression);
                return transformed;
            }

            if(exp instanceof RangeExpression) {
                RangeExpression expression = (RangeExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof PropertyExpression) {
                PropertyExpression expression = (PropertyExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof AttributeExpression) {
                AttributeExpression expression = (AttributeExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof FieldExpression) {
                FieldExpression expression = (FieldExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof MethodPointerExpression) {
                MethodPointerExpression expression = (MethodPointerExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof ConstantExpression) {
                ConstantExpression expression = (ConstantExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof ClassExpression) {
                ClassExpression expression = (ClassExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof VariableExpression) {
                VariableExpression expression = (VariableExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            if(exp instanceof DeclarationExpression) {
                DeclarationExpression expression = (DeclarationExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return expression;
            }

            // ....

            return exp;
        }

        private ArgumentListExpression getArgumentsExpressionsForClosureCall(Expression expression, Expression objectExpression, Expression methodExpression) {
            BlockStatement blockStatement = new BlockStatement();
            ExpressionStatement expressionStatement = new ExpressionStatement(expression);
            blockStatement.addStatement(expressionStatement);
            ClosureExpression closureExpression = new ClosureExpression(null, blockStatement);
            ArgumentListExpression arguments = new ArgumentListExpression();
            arguments.addExpression(objectExpression);
            arguments.addExpression(methodExpression);
            arguments.addExpression(closureExpression);
            return arguments;
        }
    }
}
