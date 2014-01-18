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
 * Tests for the {@link SecureRuntimeASTCustomizer} class.
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
        } catch (ExceptionInInitializerError e) {
            if (e.exception instanceof SecurityException) {
                if(errorMessage) assertTrue("Should have throw a Security Exception with " + errorMessage + " instead of " + e.exception.getMessage(), e.exception.getMessage().contains(errorMessage))
                return true
            } else {
                errorMessage ? fail("Should have throw a Security Exception with " + errorMessage + " instead of " + e.exception.getMessage()) : fail(e.exception.getMessage().contains(errorMessage))
            }
        }

    result
    }

    void testMethodInScript() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            def a = (Object)new ArrayList()
            a.add(new ArrayList())
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.util.ArrayList.new", "java.lang.Object"]
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
            public void b() {
                def a = (Object)new ArrayList()
                a.clear()
            }
            b();
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.add", "java.util.ArrayList.new", "java.lang.Object"]
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
            def a = ((Object)new ArrayList())
            a.add(new ArrayList().clear())
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.new", "java.util.ArrayList.add", "java.lang.Object"]
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

    void testMethodAsARightBinaryExpression() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            def a = ((Object)new ArrayList())
            a.add("" + ((Object)new ArrayList()).clear())
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.new", "java.util.ArrayList.add", "java.lang.Object"]
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
            def a = ((Object)new ArrayList())
            a.add(((Object)new ArrayList()).clear() + "")
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.new", "java.util.ArrayList.add", "java.lang.Object"]
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
        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.new", "java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert !hasSecurityException ({
            shell.evaluate(script)
        }, "Script2.b")
    }

    //TODO
    void testClosureDefinedInScript() {
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

        // 2. not defined in WL, but still no exception should be thrown
        def methodWhiteList = ["java.util.ArrayList", "java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }
        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.add")
    }

    void testStaticMethodDefinedInScript() {
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

        // 2. not defined in WL, but still no exception should be thrown
        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.new", "java.lang.Object", "Script2"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.add")
    }

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

        // Test we're going into StaticMethodCallExpression as Math.random goes through MethodCallExpression
        // This BL doesn't make any sense
        // TODO any better way?
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

        // 2. not defined in WL, but still no exception should be thrown
        def methodWhiteList = ["java.util.ArrayList","java.util.ArrayList.new", "java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }
        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.add")

        // 3. defined in BL
        def methodList = ["java.util.ArrayList.add"]
        customizer.with {
            setMethodsWhiteList(null)
            setMethodsBlackList(methodList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.add")
    }

    void testMethodCallInClassMethod() {
        def shell = new GroovyShell(configuration)
        String script = """
            class A {
                public void b() {
                    def c = ((Object)new ArrayList())
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
    //TODO
    void testConstructorWithClassForName() {
        def shell = new GroovyShell(configuration)
        String script = """
            ((Object)Class).forName('java.util.ArrayList').newInstance()
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
    //TODO
    void testConstructorWithClassForNameComingFromAnotherClass() {
        def shell = new GroovyShell(configuration)
        String script = """
            Math.class.forName('java.util.ArrayList').newInstance()
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)
        customizer.methodDefinitionAllowed = true
        def methodList = ["java.util.ArrayList", "java.util.ArrayList.new", "java.lang.Class.forName"]
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
        def methodList = ["java.util.ArrayList.new"]
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
                ((Object)java.util.ArrayList.new());
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

    void testMethodCallAsString() {
        def shell = new GroovyShell(configuration)
        String script = """
            @Newify
            def create() {
                java.util.ArrayList.new();
            }
            def a = ((Object)create())
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
            def a = [((Object)new ArrayList()).clear()]
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.new", "java.lang.Object"]
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

    void testAlias() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            def alias = (new ArrayList()).&clear
            alias()
        """
        shell.evaluate(script)
        // no error means success
        configuration.addCompilationCustomizers(customizer)

        // 2. not defined in WL
        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.ctor", "java.lang.Object"]
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
            setMethodPointersWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.clear")

        // 3. defined in BL
        def methodBlackList = ["java.util.ArrayList.clear"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodPointersWhiteList(null);
            setMethodsBlackList(methodBlackList);
            setMethodPointersBlackList(methodBlackList);
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
    void testPropertyDirectGet() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
           import java.awt.Point
           Point point  = new Point(1, 2)
           point.@x
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.awt.Point","java.awt.Point.new","java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "TODO")

        // 3. defined in BL
        def methodBlackList = ["TODO"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "TODO")
    }


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
        configuration.addCompilationCustomizers(customizer)

        // 2. not defined in WL
        def methodWhiteList = ["java.awt.Point","java.awt.Point.new","java.lang.Object", ]
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point.setX")

        // 3. defined in BL
        def methodBlackList = ["java.awt.Point.x"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point.set")
    }

    // TODO point.@x=4 (Direct attribute set)
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
        def methodWhiteList = ["java.awt.Point","java.awt.Point.new","java.lang.Object", ]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point.set")

        // 3. defined in BL
        def methodBlackList = ["TODO"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "TODO")
    }

    // TODO points*.x=3 (spread operator)
    void testSpreadOperator() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
           import java.awt.Point
           def points  =  [new Point(1, 2)]
           points*.x = 3
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.awt.Point","java.awt.Point.new","java.lang.Object" ]
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

    void testSpreadWithMethodCallOperator() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
           def list = ['first', 'second']
           list*.toString()
        """
        shell.evaluate(script)
        // no error means success
        configuration.addCompilationCustomizers(customizer)

        // 2. not defined in WL
        def methodWhiteList = ["java.lang.String","java.util.ArrayList", "java.lang.Object" ]
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.toString")

        // 3. defined in BL
        def methodBlackList = ["java.util.ArrayList.toString"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.toString")
    }

    // TODO Array get
    void testArrayGet() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            def x = new int[3]
            x[0]
        """
        shell.evaluate(script)
        // no error means success

       /* // 2. not defined in WL
        def methodWhiteList = ["java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "White list - Should throw Security Exception")
*/
        // 3. defined in BL
        def operatorBlackList = ["[": [["[I", "java.lang.Integer"]]]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(null);
            setBinaryOperatorBlackList(operatorBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "[I [ java.lang.Integer is not allowed")
    }

    // TODO Array set
    void testArraySet() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            def x = new int[3]
            x[0]=1
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.lang.Object", "TODO"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "TODO")

        // 3. defined in BL
        def methodBlackList = ["TODO"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "TODO")
    }

    // TODO Inner Class
    void testInnerClass() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            class foo {
              class bar {
                static void juu() { 5.class.forName('java.lang.String') }
              }
              static void main(String[] args) { bar.juu() }
            }
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.lang.Object", "java.lang.Class", "java.lang.Class.juu"] //Should be able to remove juu
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class.forName")

        // 3. defined in BL
        def methodBlackList = ["java.lang.Class.forName"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class.forName")
    }

    // TODO Format the test otherwise working
    void testStaticInitializationBlock() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            class foo {
              static { 5.class.forName('java.lang.String') }
              static void main(String[] args) { }
           }
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.lang.Object", "java.lang.Class"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class.forName")

        // 3. defined in BL
        def methodBlackList = ["java.lang.Class.forName"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class.forName")
    }

    // TODO Initialization Block
    void testInitializationBlock() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            class foo {
                { 5.class.forName('java.lang.String') }
            }
            new foo()
            return null
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.lang.Object", "java.lang.Class", "foo.new"] // Should be able to remove foo.new
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class.forName")

        // 3. defined in BL
        def methodBlackList = ["java.lang.Class.forName"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class.forName")
    }

    // TODO Field Initialization
    void testFieldInitialization() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            class foo {
                def obj = 5.class.forName('java.lang.String')
            }
            new foo()
            return null
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.lang.Object", "java.lang.Class", "foo.new"] // Should be able to remove foo.new
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class.forName")

        // 3. defined in BL
        def methodBlackList = ["java.lang.Class.forName"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class.forName")
    }

    // TODO Static Field Initialization
    void testStaticFieldInitialization() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            class foo {
                static  obj = 5.class.forName('java.lang.String')
            }
            new foo()
            return null
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.lang.Object", "java.lang.Class", "foo.new"] // Should be able to remove foo.new
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class.forName")

        // 3. defined in BL
        def methodBlackList = ["java.lang.Class.forName"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class.forName")
    }

    // TODO Nested class
    void testNestedClass() {

        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            x = new Object() {
                def plusOne(rhs) {
                    return rhs+1;
                }
            }
            x.plusOne(5)
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "TODO")

        // 3. defined in BL
        def methodBlackList = ["TODO"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point.set")
    }

    // TODO intArray[1] <<= 3 (compound assignment)
    void testCompoundAssignmentOnArray() {

        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            def intArray = [0, 1]
            intArray[1] <<= 3
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "TODO")

        // 3. defined in BL
        def methodBlackList = ["TODO"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "TODO")
    }

    // TODO
    void testComparisonPrimitives() {

        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            5==5
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "TODO")

        // 3. defined in BL
        def methodBlackList = ["TODO"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "TODO")
    }

    // TODO
    void testComparisonObject() {

        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            import java.awt.Point
            def p = new Point(1, 2)
            p == p
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "TODO")

        // 3. defined in BL
        def methodBlackList = ["TODO"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "TODO")
    }

    // TODO Probably 3 tests for null manipulation
    void testNullBehavior() {

        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            null
            null.getClass()
            x = null
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        def methodWhiteList = ["java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setMethodsWhiteList(methodWhiteList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "TODO")

        // 3. defined in BL
        def methodBlackList = ["TODO"]
        customizer.with {
            setMethodsWhiteList(null);
            setMethodsBlackList(methodBlackList);
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "TODO")
    }
}

