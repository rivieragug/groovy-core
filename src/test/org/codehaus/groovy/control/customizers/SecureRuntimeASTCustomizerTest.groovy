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
import org.codehaus.groovy.syntax.Types

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
            if(errorMessage) assertTrue("Should have thrown a Security Exception with " + errorMessage + " instead of " + e.getMessage(), e.getMessage().contains(errorMessage))
            result = true
        } catch (MultipleCompilationErrorsException e) {
            result = e.errorCollector.errors.any {
                if (it.cause?.class == SecurityException) {
                    if(errorMessage) assertTrue("Should have thrown a Security Exception with " + errorMessage + " instead of " + it.cause?.getMessage(), it.cause?.getMessage().contains(errorMessage))
                    return true
                }
                return false
            }
        } catch (ExceptionInInitializerError e) {
            if (e.exception instanceof SecurityException) {
                if(errorMessage) assertTrue("Should have thrown a Security Exception with " + errorMessage + " instead of " + e.exception.getMessage(), e.exception.getMessage().contains(errorMessage))
                return true
            } else {
                println e.exception
                errorMessage ? fail("Should have thrown a Security Exception with " + errorMessage + " instead of " + e.exception.getMessage()) : fail(e.exception.getMessage().contains(errorMessage))
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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = ["java.util.ArrayList#new"];
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#add")

        // 3. defined in BL
        customizer.with {
            setMethodsWhiteList(null);
            methodsBlackList = ["java.util.ArrayList#add"];
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#add")
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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = ["java.util.ArrayList#add", "java.util.ArrayList#new"]
        }
        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#clear")

        // 3. defined in BL
        customizer.with {
            methodsWhiteList = null
            methodsBlackList = ["java.util.ArrayList#clear"]
        }
        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#clear")
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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = ["java.util.ArrayList#new", "java.util.ArrayList#add"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#clear")

        // 3. defined in BL
        customizer.with {
            methodsWhiteList = null
            methodsBlackList = ["java.util.ArrayList#clear"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#clear")
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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = ["java.util.ArrayList#new", "java.util.ArrayList#add"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#clear")

        // 3. defined in BL
        customizer.with {
            methodsWhiteList  = null
            methodsBlackList = ["java.util.ArrayList#clear"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#clear")
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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = ["java.util.ArrayList#new", "java.util.ArrayList#add"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#clear")

        // 3. defined in BL
        customizer.with {
            methodsWhiteList = null
            methodsBlackList = ["java.util.ArrayList#clear"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#clear")
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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = ["java.util.ArrayList#new"]
        }

        assert !hasSecurityException ({
            shell.evaluate(script)
        }, "Script2#b")
    }

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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = ["groovy.lang.Closure#call"]
        }
        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#add")
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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = ["java.util.ArrayList#new"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#add")
    }

    void testStaticMethodCallExpressionInBlackList() {
        def shell = new GroovyShell(configuration)
        String script = """
            import static java.lang.Math.random
            import java.util.ArrayList
            def b = random()
            def a = new ArrayList()
            a.add(new ArrayList())
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)

        // Test we're going into StaticMethodCallExpression as Math.random goes through MethodCallExpression
        customizer.with {
            methodsBlackList = ["java.lang.Math#random"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Math#random")
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

        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsBlackList = ["java.util.ArrayList#add"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#add")
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

        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsBlackList = ["java.util.ArrayList#add"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#add")
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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = ["java.util.ArrayList#new"]
        }
        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#add")

        // 3. defined in BL
        customizer.with {
            methodsWhiteList = null
            methodsBlackList = ["java.util.ArrayList#add"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#add")
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
        customizer.with {
            methodsBlackList = ["java.util.ArrayList#clear"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#clear")
    }

    void testConstructorWithClassForName() {
        def shell = new GroovyShell(configuration)
        String script = """
            ((Object)Class).forName('java.util.ArrayList').newInstance()
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)
        customizer.methodDefinitionAllowed = true
        customizer.with {
            methodsBlackList = ["java.lang.Class#forName"]
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
        customizer.with {
            methodsBlackList = ["java.lang.Class#forName"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class#forName")
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
        customizer.with {
            methodsBlackList = ["java.util.ArrayList#new"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#new")
    }

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
        customizer.with {
            methodsBlackList = ["java.util.ArrayList#new"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#new")
    }

    void testConstructorWithNewifyWithCast() {
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
        customizer.with {
            methodsBlackList= ["java.util.ArrayList#new"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#new")
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
        customizer.with {
            methodsBlackList = ["java.util.ArrayList#add"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#add")
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
        customizer.with {
            methodsBlackList = ["java.util.ArrayList#add"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#add")
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

        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsBlackList = ["java.util.ArrayList#add"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#add")
    }

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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = ["java.util.ArrayList#new"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#clear")

        // 3. defined in BL
        customizer.with {
            methodsWhiteList = null
            methodsBlackList = ["java.util.ArrayList#clear"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#clear")
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
        customizer.with {
            methodPointersWhiteList = ["java.util.ArrayList#new"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#clear")

        // 3. defined in BL
        customizer.with {
            methodPointersWhiteList = null
            methodPointersBlackList = ["java.util.ArrayList#clear"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList#clear")

    }

    void testMethodChain() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            'foo'.toString().hashCode()
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = ["java.lang.String#toString"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.String#hashCode")

        // 3. defined in BL
        customizer.with {
            methodsWhiteList = null
            methodsBlackList = ["java.lang.String#hashCode"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.String#hashCode")
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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = []
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Math#max")

        // 3. defined in BL
        customizer.with {
            methodsWhiteList = null
            methodsBlackList = ["java.lang.Math#max"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Math#max")
    }

    void testClass() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
           'foo'.class
        """
        shell.evaluate(script)
        // no error means success

        // 2.1  not defined in WL
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            propertiesWhiteList = []
        }

       assert hasSecurityException ({
            shell.evaluate(script)
       }, "java.lang.Object#class")

        // 2.2  defined in WL
        customizer.with {
            propertiesWhiteList = ["java.lang.Object#class" ]
        }

       shell.evaluate(script)

        // 3. defined in BL
        customizer.with {
            propertiesWhiteList = null
            propertiesBlackList = ["java.lang.Object#class"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Object#class")
    }

    void testClassGetName() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
           'foo'.class.name
        """
        shell.evaluate(script)
        // no error means success

        // 2.1 not defined in WL
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            propertiesWhiteList = ["java.lang.Object#class"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class#name")

        // 2.2  defined in WL
        customizer.with {
            propertiesWhiteList = ["java.lang.Class#name", "java.lang.Object#class"]
        }

        shell.evaluate(script)

        // 3. defined in BL
        customizer.with {
            propertiesWhiteList = null
            propertiesBlackList = ["java.lang.Class#name"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class#name")
    }

    void testPropertyDirectGet() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
           import java.awt.Point
           Point point  = new Point(1, 2)
           point.x
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            propertiesWhiteList = ["java.awt.Point#new"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point#x")

        // 3. defined in BL
        customizer.with {
            propertiesWhiteList = null
            propertiesBlackList = ["java.awt.Point#x"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point#x")
    }


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
        customizer.with {
            propertiesWhiteList = []
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point#x")

        // 3. defined in BL
        customizer.with {
            propertiesWhiteList = null
            propertiesBlackList = ['java.awt.Point#x']
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point#x")
    }

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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            propertiesWhiteList = []
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point#@x")

        // 3. defined in BL
        customizer.with {
            propertiesWhiteList = null
            propertiesBlackList = ['java.awt.Point#@x']
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point#@x")
    }

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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            propertiesWhiteList = []
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point#x")

        // 3. defined in BL
        customizer.with {
            propertiesWhiteList = null
            propertiesBlackList = ['java.awt.Point#x']
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point#x")
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
        customizer.with {
            methodsWhiteList = []
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.String#toString")

        // 3. defined in BL
        customizer.with {
            methodsWhiteList = null
            methodsBlackList = ["java.lang.String#toString"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.String#toString")
    }

    void testArrayGet() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            def x = new int[3]
            x[0]
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)

        // 2. not defined in WL
        customizer.with {
            binaryOperatorWhiteList = ["[": [["[I", "java.lang.Long"]]]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "[I [ java.lang.Integer is not allowed ...........")

        // 3. defined in WL
        customizer.with {
            binaryOperatorWhiteList = ["[": [["[I", "java.lang.Integer"]]]
        }
        shell.evaluate(script)

        // 4. not defined in BL
        customizer.with {
            binaryOperatorWhiteList = null
            binaryOperatorBlackList = ["[": [["[I", "java.lang.Long"]]]
        }
        shell.evaluate(script)

        // 5. defined in BL
        customizer.with {
            binaryOperatorWhiteList = null
            binaryOperatorBlackList = ["[": [["[I", "java.lang.Integer"]]]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "[I [ java.lang.Integer is not allowed")
    }

    void testArraySet() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            def x = new int[3]
            x[0]=1
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)

        // 2. not defined in WL
        customizer.with {
            binaryOperatorWhiteList = ["[": [["[I", "java.lang.Integer"]]]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Integer = java.lang.Integer is not allowed ...........")

        // 3. defined in WL
        customizer.with {
            binaryOperatorWhiteList = ["[": [["[I", "java.lang.Integer"]], "=": [["java.lang.Integer", "java.lang.Integer"]]]
        }
        shell.evaluate(script)


        // 3. not defined in BL
        customizer.with {
            binaryOperatorWhiteList = null
            binaryOperatorBlackList = ["=": [["java.lang.Integer", "java.lang.Integer"]]]
        }
        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Integer = java.lang.Integer is not allowed ...........")
    }

    void testInnerClass() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            class foo {
              class bar {
                static void juu() {
                    5.class.forName('java.lang.String')
                }
              }
            }
            foo.bar.juu()
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = []
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class#forName")

        // 3. defined in BL
        customizer.with {
            methodsWhiteList = null
            methodsBlackList = ["java.lang.Class#forName"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class#forName")
    }

    void testClassDefinedInScript() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            class foo {
              def m() {
              }
            }
            new foo().m()
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = []
        }

        shell.evaluate(script);
        // no error means success
        // Should have added foo#m to whitelist
    }

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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = []
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class#forName")

        // 3. defined in BL
        customizer.with {
            methodsWhiteList = null
            methodsBlackList = ["java.lang.Class#forName"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class#forName")
    }

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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = []
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class#forName")

        // 3. defined in BL
        customizer.with {
            methodsWhiteList = null
            methodsBlackList = ["java.lang.Class#forName"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class#forName")
    }

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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = []
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class#forName")

        // 3. defined in BL
        customizer.with {
            methodsWhiteList = null
            methodsBlackList = ["java.lang.Class#forName"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class#forName")
    }

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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = []
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class#forName")

        // 3. defined in BL
        customizer.with {
            methodsWhiteList = null
            methodsBlackList = ["java.lang.Class#forName"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class#forName")
    }

    void testNestedClass() {

        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            def x = new Object()  {
                def plusOne(rhs) {
                    return rhs+1;
                }
            }
            x.plusOne(5)
        """
        //shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            binaryOperatorWhiteList = [:]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Integer + java.lang.Integer is not allowed")

        // 3. defined in BL
        customizer.with {
            binaryOperatorWhiteList = null
            binaryOperatorBlackList = ["+":  [["java.lang.Integer", "java.lang.Integer"]]]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Integer + java.lang.Integer is not allowed")
    }

    void testCompoundAssignmentOnArray() {
        def shell = new GroovyShell(configuration)
        configuration.addCompilationCustomizers(customizer)
        customizer.tokensBlacklist = [Types.LEFT_SHIFT_EQUAL]
        shell.evaluate('int i = 1;')
        assert hasSecurityException ({
            shell.evaluate("""
                def i = new int[1];i[0]=1;i[0]<<=3
            """)
        }, "Token (\"<<=\" at 2:47:  \"<<=\" ) is not allowed")
    }

    void testComparisonPrimitives() {

        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            5==5
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            binaryOperatorWhiteList = [:]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Integer == java.lang.Integer")

        // 3. defined in BL
        customizer.with {
            binaryOperatorWhiteList = null
			binaryOperatorBlackList = ["==": [["java.lang.Integer", "java.lang.Integer"]]]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Integer == java.lang.Integer")
    }

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
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = ["java.lang.Object, java.awt.Point#new"]
			binaryOperatorWhiteList = [:]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point == java.awt.Point")

        // 3. defined in BL
        customizer.with {
			binaryOperatorWhiteList = null
			binaryOperatorBlackList = ["==": [["java.awt.Point", "java.awt.Point"]]]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point == java.awt.Point")
    }

    void testNullBehavior() {
        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            def toto = null.getClass().forName('java.util.ArrayList')
        """
        shell.evaluate(script)
        // no error means success

        // 2. not defined in WL
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = []
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class#forName")

        // 3. defined in BL
        customizer.with {
            methodsWhiteList = null
            methodsBlackList = ["java.lang.Class#forName"]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.Class#forName")
    }

    void testNullBehaviorAssignment() {

        // 1. no restriction
        def shell = new GroovyShell(configuration)
        String script = """
            x = null
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            binaryOperatorWhiteList = [:]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "null = null")
    }

    void testClosureDelegation() {
        def shell = new GroovyShell(configuration)
        String script = """
            def x = 0;
            c = { ->
                delegate = "foo";
                x = length();
            }
            c();

            x;
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = []
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.String#length")
    }

    void testClosureDelegationOwner() {
        def shell = new GroovyShell(configuration)
        String script = """
            def x = 0;
            c = { ->
                delegate = "foo";
                { -> x = length(); }();
            }
            c();

            x;
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = []
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.String#length")
    }

    void testClosureDelegationProperty() {
        def shell = new GroovyShell(configuration)
        String script = """
            def sum = 0;
            c = { ->
                delegate = new java.awt.Point(1,2);
                sum = x+y;
            }
            c();

            sum;
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = ["java.awt.Point#new"]
            propertiesWhiteList = []
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point#x")
    }

    void testClosureDelegationPropertyOwner() {
        def shell = new GroovyShell(configuration)
        String script = """
            def sum = 0;
            c = { ->
                delegate = new java.awt.Point(1,2);
                { -> sum = x+y; }();
            }
            c();

            sum;
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            methodsWhiteList = ["java.awt.Point#new"]
            propertiesWhiteList = []
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.awt.Point#x")
    }

    void testLogicalNotEquals() {
        def shell = new GroovyShell(configuration)
        String script = """
            def x = 3.toString();
            if (x != '') return true; else return false;
        """
        shell.evaluate(script)
        // no error means success

        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            binaryOperatorWhiteList = [:]
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.lang.String != java.lang.String")
    }
}

