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

import groovy.lang.Closure;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.classgen.VariableScopeVisitor;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.transform.sc.ListOfExpressionsExpression;

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
    private Map<String,List<List<String>>> binaryOperatorWhiteList;
    private Map<String,List<List<String>>> binaryOperatorBlackList;

    private List<String> methodPointersWhiteList;
    private List<String> methodPointersBlackList;

    private List<String> propertiesWhiteList;
    private List<String> propertiesBlackList;

    public static final ClassNode GAC_CLASS = ClassHelper.make(GroovyAccessControl.class);


    public SecureRuntimeASTCustomizer() {
        super();
    }

    public void setMethodsBlackList(final List<String> methodsBlackList) {
        if (methodsWhiteList != null) {
            throw new IllegalArgumentException("You are not allowed to set both whitelist and blacklist");
        }
        this.methodsBlackList = methodsBlackList;
    }

    public Map<String, List<List<String>>> getBinaryOperatorWhiteList() {
        return binaryOperatorWhiteList;
    }

    public void setBinaryOperatorWhiteList(Map<String, List<List<String>>> binaryOperatorWhiteList) {
        if (binaryOperatorBlackList != null && binaryOperatorWhiteList != null) {
            throw new IllegalArgumentException("You are not allowed to set both whiteBinarylist and blackBinarylist");
        }
        this.binaryOperatorWhiteList = binaryOperatorWhiteList;
    }

    public Map<String, List<List<String>>> getBinaryOperatorBlackList() {
        return binaryOperatorBlackList;
    }

    public void setBinaryOperatorBlackList(Map<String, List<List<String>>> binaryOperatorBlackList) {
        if (binaryOperatorWhiteList != null && binaryOperatorBlackList != null) {
            throw new IllegalArgumentException("You are not allowed to set both whiteBinarylist and blackBinarylist");
        }
        this.binaryOperatorBlackList = binaryOperatorBlackList;
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

    public void setMethodPointersBlackList(final List<String> methodsBlackList) {
        if (methodPointersWhiteList != null) {
            throw new IllegalArgumentException("You are not allowed to set both methodPointerWhitelist and methodPointerBlacklist");
        }
        this.methodPointersBlackList = methodsBlackList;
    }

    public List<String> getMethodPointersBlackList() {
        return methodPointersBlackList;
    }

    public void setMethodPointersWhiteList(final List<String> methodPointersWhiteList) {
        if (methodPointersBlackList != null) {
            throw new IllegalArgumentException("You are not allowed to set both methodPointerWhitelist and methodPointerBlacklist");
        }
        this.methodPointersWhiteList = methodPointersWhiteList;
    }

    public List<String> getMethodPointersWhiteList() {
        return methodPointersWhiteList;
    }

    public List<String> getPropertiesWhiteList() {
        return propertiesWhiteList;
    }

    public void setPropertiesWhiteList(List<String> propertiesWhiteList) {
        if (propertiesBlackList != null) {
            throw new IllegalArgumentException("You are not allowed to set both propertiesWhitelist and propertiesBlacklist");
        }
        this.propertiesWhiteList = propertiesWhiteList;
    }

    public List<String> getPropertiesBlackList() {
        return propertiesBlackList;
    }

    public void setPropertiesBlackList(List<String> propertiesBlackList) {
        if (propertiesWhiteList != null) {
            throw new IllegalArgumentException("You are not allowed to set both propertiesWhitelist and propertiesBlacklist");
        }
        this.propertiesBlackList = propertiesBlackList;
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

        // TODO: To revisit
        if(getMethodPointersWhiteList() != null) {
            ListExpression array = new ListExpression();
            for(String whiteListElement : getMethodPointersWhiteList()) {
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
        if(getMethodPointersBlackList() != null) {
            ListExpression array = new ListExpression();
            for(String blackListElement : getMethodPointersBlackList()) {
                array.addExpression(new ConstantExpression(blackListElement));
            }
            expression.addExpression(array);
        } else {
            expression.addExpression(ConstantExpression.NULL);
        }

        if(getBinaryOperatorWhiteList() != null) {
            MapExpression map = new MapExpression();
            for(Map.Entry entry : getBinaryOperatorWhiteList().entrySet()) {
                ListExpression le = new ListExpression();
                for (List<String> pair: (List<List<String>>)entry.getValue()){
                    ListExpression pairExpression = new ListExpression();
                    pairExpression.addExpression(new ConstantExpression(pair.get(0)));
                    pairExpression.addExpression(new ConstantExpression(pair.get(1)));
                    le.addExpression(pairExpression);
                }
                map.addMapEntryExpression(new MapEntryExpression(new ConstantExpression(entry.getKey()),le ) );
            }
            expression.addExpression(map);
        } else {
            expression.addExpression(ConstantExpression.NULL);
        }

        if(getBinaryOperatorBlackList() != null) {
            MapExpression map = new MapExpression();
            for(Map.Entry entry : getBinaryOperatorBlackList().entrySet()) {
                ListExpression le = new ListExpression();
                for (List<String> pair: (List<List<String>>)entry.getValue()){
                    ListExpression pairExpression = new ListExpression();
                    pairExpression.addExpression(new ConstantExpression(pair.get(0)));
                    pairExpression.addExpression(new ConstantExpression(pair.get(1)));
                    le.addExpression(pairExpression);
                }
                map.addMapEntryExpression(new MapEntryExpression(new ConstantExpression(entry.getKey()),le ) );
            }

            expression.addExpression(map);
        } else {
            expression.addExpression(ConstantExpression.NULL);
        }

        // Properties WL / BL
        if(getPropertiesWhiteList() != null) {
            ListExpression array = new ListExpression();
            for(String whiteListElement : getPropertiesWhiteList()) {
                array.addExpression(new ConstantExpression(whiteListElement));
            }
            expression.addExpression(array);
        } else {
            expression.addExpression(ConstantExpression.NULL);
        }
        if(getPropertiesBlackList() != null) {
            ListExpression array = new ListExpression();
            for(String blackListElement : getPropertiesBlackList()) {
                array.addExpression(new ConstantExpression(blackListElement));
            }
            expression.addExpression(array);
        } else {
            expression.addExpression(ConstantExpression.NULL);
        }

        classNode.addFieldFirst("groovyAccessControl", MethodNode.ACC_PROTECTED | MethodNode.ACC_FINAL | MethodNode.ACC_STATIC, GAC_CLASS, new ConstructorCallExpression(GAC_CLASS, expression));

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
            System.out.println("Here is the " + exp);

            if (exp instanceof MethodCallExpression) {
                return makeSafeMethodCall((MethodCallExpression) exp);
            }

            if(exp instanceof BinaryExpression) {
                BinaryExpression expression = (BinaryExpression)exp;
                //CompareIdentityExpression
                //CompareToNullExpression'
                //DeclarationExpression
                
                expression.setRightExpression(transform(expression.getRightExpression()));

                if(!(expression instanceof DeclarationExpression)){
                	boolean setProperty = expression.getLeftExpression() instanceof AttributeExpression;
                    expression.setLeftExpression(transform(expression.getLeftExpression()));
                    ArgumentListExpression argumentListExpression = getArgumentsExpressionForCheckBinaryCall(expression);
                    MethodCallExpression methodCallExpression = new MethodCallExpression(
                            new VariableExpression("groovyAccessControl", new ClassNode(GroovyAccessControl.class)),
                            setProperty ? "checkSetPropertyExpression" : "checkBinaryExpression", argumentListExpression);
                    methodCallExpression.setSourcePosition(exp);
                    return methodCallExpression;
                }
                return expression;
            }

            if (exp instanceof StaticMethodCallExpression) {
                StaticMethodCallExpression expression = (StaticMethodCallExpression) exp;

                ArgumentListExpression newMethodCallArguments = getArgumentsExpressionsForClosureCall(
                        new ClassExpression(expression.getOwnerType()),
                        new ConstantExpression(expression.getMethod()),
                        expression.getArguments());

                MethodCallExpression methodCallExpression = new MethodCallExpression(new VariableExpression("groovyAccessControl", GAC_CLASS), "checkCall", newMethodCallArguments);
                methodCallExpression.setSourcePosition(exp);
                return methodCallExpression;
            }

            if(exp instanceof ClosureExpression) {
                ClosureExpression expression = (ClosureExpression)exp;
                expression.getCode().visit(this);
                return expression;
            }

            if (exp instanceof ConstructorCallExpression) {
                ConstructorCallExpression expression = (ConstructorCallExpression) exp;
                ArgumentListExpression newMethodCallArguments = getArgumentsExpressionsForClosureCall(
                        new ClassExpression(expression.getType()),
                        new ConstantExpression("new"),
                        expression.getArguments());

                MethodCallExpression methodCallExpression = new MethodCallExpression(new VariableExpression("groovyAccessControl", GAC_CLASS), "checkCall", newMethodCallArguments);
                methodCallExpression.setSourcePosition(exp);
                return methodCallExpression;
            }

            if(exp instanceof ClassExpression) {
                ClassExpression expression = (ClassExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if(exp instanceof TernaryExpression) {
                if(exp instanceof ElvisOperatorExpression) {
                    ElvisOperatorExpression expression = (ElvisOperatorExpression)exp;
                    System.out.println("TO BE FILLED IF NECESSARY" + expression);
                    return super.transform(expression);
                } else {
	                TernaryExpression expression = (TernaryExpression)exp;
	                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                    return super.transform(expression);
                }
            }
            
            if(exp instanceof SpreadExpression) {
            	SpreadExpression expression = (SpreadExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if(exp instanceof PrefixExpression) {
                PrefixExpression expression = (PrefixExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if(exp instanceof PostfixExpression) {
                PostfixExpression expression = (PostfixExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if(exp instanceof BooleanExpression) {
                BooleanExpression expression = (BooleanExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if(exp instanceof MapExpression) {
                if(exp instanceof NamedArgumentListExpression) {
                	NamedArgumentListExpression expression = (NamedArgumentListExpression)exp;
                    System.out.println("TO BE FILLED IF NECESSARY" + expression);
                    return super.transform(expression);
                }
                MapExpression expression = (MapExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if(exp instanceof MapEntryExpression) {
                MapEntryExpression expression = (MapEntryExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if(exp instanceof ListExpression) {
                if(exp instanceof ClosureListExpression) {
                	ClosureListExpression expression = (ClosureListExpression)exp;
                    System.out.println("TO BE FILLED IF NECESSARY" + expression);
                    return expression;
                }
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
                return super.transform(expression);
            }

            if(exp instanceof VariableExpression) {
                VariableExpression expression = (VariableExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if(exp instanceof PropertyExpression) {
            	// AttributeExpression
                if(exp instanceof AttributeExpression) {
                    AttributeExpression expression = (AttributeExpression)exp;
                    System.out.println("TO BE FILLED IF NECESSARY" + expression);
                    return super.transform(expression);
                }

                return makeSafeProperty((PropertyExpression) exp);
            }

            if(exp instanceof FieldExpression) {
                FieldExpression expression = (FieldExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if (exp instanceof MethodPointerExpression) {
                MethodPointerExpression expression = (MethodPointerExpression) exp;
                ArgumentListExpression newMethodCallArguments =
                        new ArgumentListExpression(
                                transform(expression.getExpression()),
                                expression.getMethodName()
                        );
                MethodCallExpression methodCallExpression = new MethodCallExpression(new VariableExpression("groovyAccessControl", new ClassNode(GroovyAccessControl.class)), "checkMethodPointerDeclaration", newMethodCallArguments);
                methodCallExpression.setSourcePosition(exp);
                return methodCallExpression;
            }

            if(exp instanceof ConstantExpression) {
                if(exp instanceof AnnotationConstantExpression) {
                	AnnotationConstantExpression expression = (AnnotationConstantExpression)exp;
                    System.out.println("TO BE FILLED IF NECESSARY" + expression);
                    return super.transform(expression);
                }

                ConstantExpression expression = (ConstantExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if(exp instanceof ClassExpression) {
                ClassExpression expression = (ClassExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if(exp instanceof ArrayExpression) {
                ArrayExpression expression = (ArrayExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }
            if(exp instanceof BitwiseNegationExpression) {
            	BitwiseNegationExpression expression = (BitwiseNegationExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if(exp instanceof CastExpression) {
                CastExpression expression = (CastExpression)exp;
                CastExpression transformed = new CastExpression(expression.getType(), transform(expression.getExpression()));
                transformed.setSourcePosition(exp);
                return transformed;
            }

            if(exp instanceof EmptyExpression) {
            	EmptyExpression expression = (EmptyExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if(exp instanceof GStringExpression) {
            	GStringExpression expression = (GStringExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if(exp instanceof ListOfExpressionsExpression) {
            	ListOfExpressionsExpression expression = (ListOfExpressionsExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if(exp instanceof SpreadMapExpression) {
            	SpreadMapExpression expression = (SpreadMapExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if(exp instanceof UnaryMinusExpression) {
            	UnaryMinusExpression expression = (UnaryMinusExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            if(exp instanceof UnaryPlusExpression) {
            	UnaryPlusExpression expression = (UnaryPlusExpression)exp;
                System.out.println("TO BE FILLED IF NECESSARY" + expression);
                return super.transform(expression);
            }

            // ....

            return super.transform(exp);
        }

        private Expression makeSafeProperty(final PropertyExpression pexp) {
            if (pexp.isSpreadSafe()) {
                // foo.*x --> foo.collect { it.x }
                // foo*.bar() --> foo.collect { it.bar() }
                Statement code = new ReturnStatement(
                        new PropertyExpression(
                                new VariableExpression("it"),
                                pexp.getProperty()
                        )
                );
                MethodCallExpression result = new MethodCallExpression(
                        pexp.getObjectExpression(),
                        "collect",
                        new ArgumentListExpression(new ClosureExpression(Parameter.EMPTY_ARRAY, code))
                );
                result.setSafe(false);
                result.setSpreadSafe(false);
                result.setImplicitThis(false);
                result.setSourcePosition(pexp);
                return makeSafeMethodCall(result);

            }
            Expression closureExpression = new ConstructorCallExpression(
                    ClassHelper.make(PropertyCallClosure.class),
                    ArgumentListExpression.EMPTY_ARGUMENTS
            );

            ArgumentListExpression groovyAccessControlArguments = new ArgumentListExpression(
                    transform(pexp.getObjectExpression()),
                    transform(pexp.getProperty()),
                    closureExpression
            );

            MethodCallExpression methodCallExpression = new MethodCallExpression(new VariableExpression("groovyAccessControl", GAC_CLASS), "checkPropertyNode", groovyAccessControlArguments);
            methodCallExpression.setSourcePosition(pexp);
            return methodCallExpression;
        }

        private Expression makeSafeMethodCall(final MethodCallExpression call) {
            if (call.isSpreadSafe()) {
                // foo*.bar() --> foo.collect { it.bar() }
                Statement code = new ReturnStatement(
                        new MethodCallExpression(
                                new VariableExpression("it"),
                                call.getMethod(),
                                call.getArguments()
                        )
                );
                MethodCallExpression result = new MethodCallExpression(
                        call.getObjectExpression(),
                        "collect",
                        new ArgumentListExpression(new ClosureExpression(Parameter.EMPTY_ARRAY, code))
                );
                result.setSafe(false);
                result.setSpreadSafe(false);
                result.setImplicitThis(false);
                result.setSourcePosition(call);
                return makeSafeMethodCall(result);
            }
            ArgumentListExpression groovyAccessControlArguments = getArgumentsExpressionsForClosureCall(
                    call.getObjectExpression(),
                    call.getMethod(),
                    call.getArguments());

            MethodCallExpression methodCallExpression = new MethodCallExpression(new VariableExpression("groovyAccessControl", GAC_CLASS), "checkCall", groovyAccessControlArguments);
            methodCallExpression.setSourcePosition(call);
            return methodCallExpression;
        }

        private ArgumentListExpression getArgumentsExpressionsForClosureCall(Expression objectExpression, Expression methodExpression, Expression argsExpression) {
            Expression closureExpression = new ConstructorCallExpression(
                    ClassHelper.make(SecureMethodCallClosure.class),
                    ArgumentListExpression.EMPTY_ARGUMENTS
            );

            List<Expression> argList = new LinkedList<Expression>();
            if (argsExpression instanceof ArgumentListExpression) {
                for (Expression arg : ((ArgumentListExpression) argsExpression).getExpressions()) {
                    argList.add(transform(arg));
                }
            } else {
                argList.add(transform(argsExpression));
            }

            ArgumentListExpression arguments = new ArgumentListExpression();
            arguments.addExpression(transform(objectExpression));
            arguments.addExpression(transform(methodExpression));

            CastExpression cast = new CastExpression(ClassHelper.OBJECT_TYPE.makeArray(), new ListExpression(argList));
            cast.setCoerce(true);
            arguments.addExpression(cast);

            arguments.addExpression(closureExpression);
            return arguments;
        }

        private ArgumentListExpression getArgumentsExpressionForCheckBinaryCall(BinaryExpression binaryExpression){
            BlockStatement blockStatement = new BlockStatement();

            Parameter left = new Parameter(ClassHelper.OBJECT_TYPE, "left");
            Parameter right = new Parameter(ClassHelper.OBJECT_TYPE, "right");

            // Does not work for AttributeExpression
            BinaryExpression be = new BinaryExpression(new VariableExpression(left),binaryExpression.getOperation(), new VariableExpression(right));

            ExpressionStatement expressionStatement = new ExpressionStatement(be);
            blockStatement.addStatement(expressionStatement);
            ClosureExpression closureExpression = new ClosureExpression(new Parameter[]{left,right}, blockStatement);
            ArgumentListExpression arguments = new ArgumentListExpression();

            arguments.addExpression(new ConstantExpression(binaryExpression.getOperation().getText()));
            arguments.addExpression(binaryExpression.getLeftExpression());
            arguments.addExpression(binaryExpression.getRightExpression());
            arguments.addExpression(closureExpression);
            return arguments;

        }
    }

    public static class SecureMethodCallClosure extends Closure {
        public SecureMethodCallClosure() {
            super(null, null);
        }

        public Object doCall(Object receiver, String message, Object[] args) {
            if ("new".equals(message) && receiver instanceof Class) {
                return InvokerHelper.invokeConstructorOf((Class) receiver, args);
            } else {
                return InvokerHelper.invokeMethod(receiver, message, args);
            }
        }

    }

    public static class PropertyCallClosure extends Closure {
        public PropertyCallClosure() {
            super(null, null);
        }

        public Object doCall(Object receiver, String message) {
            return InvokerHelper.getProperty(receiver, message);
        }

    }
}
