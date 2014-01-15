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

package org.codehaus.groovy.control.customizers

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.MultipleCompilationErrorsException

/**
 * Tests for the {@link SecureASTCustomizer} class.
 */
class SecureRuntimeASTCustomizerTest extends GroovyTestCase {
    CompilerConfiguration configuration
    SecureRuntimeASTCustomizer customizer

    void setUp() {
        configuration = new CompilerConfiguration()
        customizer = new SecureRuntimeASTCustomizer()
    }

    private boolean hasSecurityException(Closure closure, String errorMessage) {
        boolean result = false;
        try {
            closure()
        } catch (SecurityException e) {
            if(errorMessage) assertTrue("Should have throw a Security Exception with " + errorMessage + " instead of " + e.getMessage(), e.getMessage().contains(errorMessage))
            result = true
        } catch (MultipleCompilationErrorsException e) {
            result = e.errorCollector.errors.any {
                if (it.cause?.class == SecurityException) {
                    if(errorMessage) assertTrue("Should have throw a Security Exception with " + errorMessage + " instead of " + it.cause?.getMessage(), it.cause?.getMessage().contains(errorMessage))
                    return true
                }
                return false
            }
        }

        result
    }

    void testMethodInScript() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            def a = new ArrayList()
            a.add(new ArrayList())
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.util.ArrayList.ctor", "java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.add")

        // 3. defined in BL
        def methodBlackList = ["java.util.ArrayList.add"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.add")
    }

    void testMethodInsideMethod() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            //new ArrayList().add(new ArrayList())
            public void b() {
                def a = new ArrayList()
                a.clear()
            }
            b();
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.add", "java.util.ArrayList.ctor", "java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }
        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.clear")

        // 3. defined in BL
        def methodBlackList = ["java.util.ArrayList.clear"]
        customizer.with {
            setMethodsWhiteList(null)
            setMethodsBlackList(methodBlackList)
        }
        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.clear")
    }

    void testMethodAsArguments() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            def a = new ArrayList()
            a.add(new ArrayList().clear())
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.ctor", "java.util.ArrayList.add", "java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.clear")

        // TODO 3. defined in BL
        def methodBlackList = ["java.util.ArrayList.clear"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(null)
            setMethodsBlackList(methodBlackList)
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.clear")
    }

    void testMethodAsARightBinaryExpression() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            def a = new ArrayList()
            a.add("" + new ArrayList().clear())
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.ctor", "java.util.ArrayList.add", "java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.clear")

        // 3. defined in BL
        def methodBlackList = ["java.util.ArrayList.clear"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.clear")
    }

    void testMethodAsLeftBinaryExpression() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            def a = new ArrayList()
            a.add(new ArrayList().clear() + "")
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.ctor", "java.util.ArrayList.add", "java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.clear")

        // 3. defined in BL
        def methodBlackList = ["java.util.ArrayList.clear"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.clear")
    }

    void testMethodDefinedInScript() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            public void b() {
            }
            b()
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL, but still no exception should be thrown
        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.ctor", "java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert !hasSecurityException ({
            shell.evaluate(script)
        }, "Script2.b")
    }

    //TODO
    void testMethodNotInWhiteListButAcceptClosureInScript() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            def b = {}
            b()
            def a = new ArrayList()
            a.add(new ArrayList())
        """
        shell.evaluate(script)
        // no error means success

        def methodWhiteList = ["java.util.ArrayList", "java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }
        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.add")
    }

    void testMethodNotInWhiteListButAcceptStaticMethodInScript() {
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            public static void b() {
            }
            b()
            def a = new ArrayList()
            a.add(new ArrayList())
        """
        shell.evaluate(script)
        // no error means success

        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.ctor", "java.lang.Object", "Script2"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.add")
    }

    // TODO is it useful to have Script2.b in BL ??
    void testStaticMethodInBlackList() {
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            public static void b() {
            }
            b()
            def a = new ArrayList()
            a.add(new ArrayList())
        """
        shell.evaluate(script)
        // no error means success

        def methodBlackList = ["Script2.b"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "Script2.b")
    }

    void testForNameSecurity() {
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            def a = Math.class.forName('java.util.ArrayList').newInstance()
            a.add(new ArrayList())
        """
        shell.evaluate(script)
        // no error means success

        def methodList = ["java.util.ArrayList.add"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsBlackList(methodList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.add")
    }

    void testForNameSecurityFromInt() {
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            def a = 5.class.forName('java.util.ArrayList').newInstance()
            a.add(new ArrayList())
        """
        shell.evaluate(script)
        // no error means success

        def methodList = ["java.util.ArrayList.add"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsBlackList(methodList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.add")
    }

    void testForNameSecurityNewify() {
        def shell = new GroovyShell(configuration)
        String script = """
            @Newify
            def create() {
                java.util.ArrayList.new();
            }
            a = create()
            a.add(new ArrayList())
        """
        shell.evaluate(script)
        // no error means success

        def methodList = ["java.util.ArrayList.add"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsBlackList(methodList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.add")
    }

    void testForMethodClassCodeInsideScript() {
        def shell = new GroovyShell(configuration)
        String script = """
            class A {
                public void b() {
                    def c = new ArrayList()
                    c.clear()
                }
            }
            new A().b()
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)
        customizer.methodDefinitionAllowed = true
        def methodList = ["java.util.ArrayList.clear"]
        customizer.with {
            setMethodsBlackList(methodList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.clear")
    }

    void testConstructorWithClassForName() {
        def shell = new GroovyShell(configuration)
        String script = """
            Class.forName('java.util.ArrayList').newInstance()
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)
        customizer.methodDefinitionAllowed = true
        def methodList = ["java.util.ArrayList", "java.lang.Class"]
        customizer.with {
            setMethodsBlackList(methodList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class")
    }

    void testConstructorWithClassForNameComingFromAnotherClass() {
        def shell = new GroovyShell(configuration)
        String script = """
            Math.class.forName('java.util.ArrayList').newInstance()
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)
        customizer.methodDefinitionAllowed = true
        def methodList = ["java.util.ArrayList", "java.util.ArrayList.ctor", "java.lang.Class.forName"]
        customizer.with {
            setMethodsBlackList(methodList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class")
    }

    void testSimpleConstructor() {
        def shell = new GroovyShell(configuration)
        String script = """
            new java.util.ArrayList()
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)
        customizer.methodDefinitionAllowed = true
        def methodList = ["java.util.ArrayList.ctor"]
        customizer.with {
            setMethodsBlackList(methodList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList")
    }

    //TODO Failing test
    void testConstructorWithNewify() {
        def shell = new GroovyShell(configuration)
        String script = """
            @Newify
            def create() {
                java.util.ArrayList.new();
            }
            a = create()
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)
        customizer.methodDefinitionAllowed = true
        def methodList = ["java.util.ArrayList"]
        customizer.with {
            setMethodsBlackList(methodList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList")
    }

    void testIfStatementForRuntime() {
        def shell = new GroovyShell(configuration)
        String script = """
            def bool() {
                ((Object)new ArrayList()).'add'("");
                return true
            }
            if(bool()) {
                new ArrayList().clear()
            }
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)
        customizer.methodDefinitionAllowed = true
        def methodList = ["java.util.ArrayList.add"]
        customizer.with {
            setMethodsBlackList(methodList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.add")
    }

    void testReturnStatementForRuntime() {
        def shell = new GroovyShell(configuration)
        String script = """
            class A {
                public boolean  bool() {
                    ((Object)new ArrayList()).'add'("");
                    return true
                }
            }
            return new A().'bool'()
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)
        customizer.methodDefinitionAllowed = true
        def methodList = ["java.util.ArrayList.add"]
        customizer.with {
            setMethodsBlackList(methodList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.add")
    }

    void testForNameSecurityWithMethodNameInString() {
        def shell = new GroovyShell(configuration)
        String script = """
            @Newify
            def create() {
                java.util.ArrayList.new();
            }
            a = create()
            a.'add'(new ArrayList())
        """
        shell.evaluate(script)
        // no error means success

        def methodList = ["java.util.ArrayList.add"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsBlackList(methodList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.add")
    }

    // TODO method call in array init
    void testMethodInArrayInititalization() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            def a = [new ArrayList().clear()]
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.ctor", "java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.clear")

        // 3. defined in BL
        def methodBlackList = ["java.util.ArrayList.clear"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.clear")
    }
    // From Koshuke test harness

    // TODO alias
    void testAlias() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            def alias = new ArrayList().&clear
            alias()
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.ctor", "java.lang.Object", "org.codehaus.groovy.runtime.MethodClosure.call"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.clear")

        // 3. defined in BL
        def methodBlackList = ["java.util.ArrayList.clear"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.clear")
    }

    //TODO 'foo'.toString().hashCode()
    void testMethodChain() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            'foo'.toString().hashCode()
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.lang.String", "java.lang.String.toString", "java.lang.Object", ]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.String.hashCode")

        // 3. defined in BL
        def methodBlackList = ["java.lang.String.hashCode"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.String.hashCode")
    }

    //import static java.lang.Math.*; max(1f,2f)
    void testStaticImport() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
           import static java.lang.Math.*
           max(1f,2f)
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.lang.Math", "java.lang.Object", ]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Math.max")

        // 3. defined in BL
        def methodBlackList = ["java.lang.Math.max"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Math.max")
    }

    // TODO 'foo'.class.name
    void testClassGetName() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
           'foo'.class.name
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.lang.String","java.lang.Object", ]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.String.getClass")

        // 3. defined in BL
        def methodBlackList = ["java.lang.String.getClass"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.String.getClass")
    }

    // TODO 'foo'.class.name
    void testClassGetName2() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
           'foo'.class.name
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.lang.String","java.lang.String.getClass","java.lang.Object", ]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class.getName")

        // 3. defined in BL
        def methodBlackList = ["java.lang.Class.getName"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class.getName")
    }

    // TODO new java.awt.Point(1,2).@x

    // TODO point.x=3 (property set)
    void testPropertySet() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
           import java.awt.Point
           Point point  =  new Point(1, 2)
           point.x = 3
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.awt.Point","java.awt.Point.ctor","java.lang.Object", ]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point.set")

        // 3. defined in BL
        def methodBlackList = ["java.awt.Point.set"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point.set")
    }

    // TODO point.@x=4 (attribute set)
    void testAttributeSet() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
           import java.awt.Point
           Point point  =  new Point(1, 2)
           point.@x = 3
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.awt.Point","java.awt.Point.ctor","java.lang.Object", ]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point.set")

        // 3. defined in BL
        def methodBlackList = ["java.awt.Point.set"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point.set")
    }

    // TODO points*.x=3 (spread operator)
    void testSpreadOperator() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
           import java.awt.Point
           Point point  =  new Point(1, 2)
           point.*x = 3
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.awt.Point","java.awt.Point.ctor","java.lang.Object", ]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point.set")

        // 3. defined in BL
        def methodBlackList = ["java.awt.Point.set"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point.set")
    }

    //x=new int[3];x[0]=1;x[0] array set get

    // inner class
//    "class foo {\n" +
//    "  class bar {\n" +
//    "    static void juu() { 5.class.forName('java.lang.String') }\n" +
//    "  }\n" +
//    "static void main(String[] args) { bar.juu() }\n" +
//    "}"
    // static initialization block
//    "class foo {\n" +
//    "static { 5.class.forName('java.lang.String') }\n" +
//    " static void main(String[] args) { }\n" +
//    "}"
    // initialization block
//    "class foo {\n" +
//    "{ 5.class.forName('java.lang.String') }\n" +
//    "}\n" +
//    "new foo()\n" +
//    "return null"
    //field intitialization
//    "class foo {\n" +
//    "def obj = 5.class.forName('java.lang.String')\n" +
//    "}\n" +
//    "new foo()\n" +
//    "return null"
    //static field initialization
//    "class foo {\n" +
//    "static obj = 5.class.forName('java.lang.String')\n" +
//    "}\n" +
//    "new foo()\n" +
//    "return null"
    //compound assignment
//    point.x += 3
//    intArray[1] <<= 3;
    //comparison (BL compareTo)
//    point==point
//    5==5
    // nested class
//    x = new Object() {
//        def plusOne(rhs) {
//            return rhs+1;
//        }
//    }
//    x.plusOne(5)
    //ArrayArgumentsInvocation() {
//    new TheTest.MethodWithArrayArg().f(new Object[3])")
    // null
//    x=null; null.getClass()
}
