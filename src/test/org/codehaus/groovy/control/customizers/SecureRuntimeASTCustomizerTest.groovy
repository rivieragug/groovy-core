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

    void testMethodNotInWhiteList() {
        def shell = new GroovyShell(configuration)
        String script = """
            import java.util.ArrayList
            def a = new ArrayList()
            a.add(new ArrayList())
        """
        shell.evaluate(script)
        // no error means success

        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.ctor", "java.lang.Object"]
        configuration.addCompilationCustomizers(customizer)
        customizer.with {
            setReceiversWhiteList(methodWhiteList);
            addMethodChecker(new WhitelistRuntimeChecker(methodWhiteList, null))
        }

        assert hasSecurityException ({
            shell.evaluate(script)
        }, "java.util.ArrayList.add")
    }

//    void testMethodNotInWhiteListInsideMethod() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            import java.util.ArrayList
//            //new ArrayList().add(new ArrayList())
//            public void b() {
//                def a = new ArrayList()
//                a.clear()
//            }
//            b();
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.add", "java.util.ArrayList.ctor", "java.lang.Object"]
//        configuration.addCompilationCustomizers(customizer)
//        customizer.with {
//            setReceiversWhiteList(methodWhiteList);
//        }
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.util.ArrayList.clear")
//    }
//
//    void testMethodNotInWhiteListButAsArguments() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            import java.util.ArrayList
//            def a = new ArrayList()
//            a.add(new ArrayList().clear())
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.ctor", "java.util.ArrayList.add", "java.lang.Object"]
//        configuration.addCompilationCustomizers(customizer)
//        customizer.with {
//            setReceiversWhiteList(methodWhiteList);
//        }
//
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.util.ArrayList.clear")
//    }
//
//
//    void testMethodNotInWhiteListAsABinaryExpression() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            import java.util.ArrayList
//            def a = new ArrayList()
//            a.add("" + new ArrayList().clear())
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.ctor", "java.util.ArrayList.add", "java.lang.Object"]
//        configuration.addCompilationCustomizers(customizer)
//        customizer.with {
//            setReceiversWhiteList(methodWhiteList);
//        }
//
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.util.ArrayList.clear")
//    }
//
//    void testMethodNotInWhiteListButAcceptMethodInScript() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            import java.util.ArrayList
//            public void b() {
//            }
//            b()
//            def a = new ArrayList()
//            a.add(new ArrayList())
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.ctor", "java.lang.Object"]
//        configuration.addCompilationCustomizers(customizer)
//        customizer.with {
//            setReceiversWhiteList(methodWhiteList);
//        }
//
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.util.ArrayList.add")
//    }
//
//    void testMethodNotInWhiteListButAcceptClosureInScript() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            import java.util.ArrayList
//            def b = {}
//            b()
//            def a = new ArrayList()
//            a.add(new ArrayList())
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        def methodWhiteList = ["java.util.ArrayList", "java.lang.Object"]
//        configuration.addCompilationCustomizers(customizer)
//        customizer.with {
//            setReceiversWhiteList(methodWhiteList);
//        }
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.util.ArrayList.add")
//    }
//
//    void testMethodNotInWhiteListButAcceptStaticMethodInScript() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            import java.util.ArrayList
//            public static void b() {
//            }
//            b()
//            def a = new ArrayList()
//            a.add(new ArrayList())
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        def methodWhiteList = ["java.util.ArrayList", "java.util.ArrayList.ctor", "java.lang.Object", "Script2"]
//        configuration.addCompilationCustomizers(customizer)
//        customizer.with {
//            setReceiversWhiteList(methodWhiteList);
//        }
//
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.util.ArrayList.add")
//    }
//
//    void testStaticMethodInBlackList() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            import java.util.ArrayList
//            public static void b() {
//            }
//            b()
//            def a = new ArrayList()
//            a.add(new ArrayList())
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        def methodBlackList = ["Script2.b"]
//        configuration.addCompilationCustomizers(customizer)
//        customizer.with {
//            setReceiversBlackList(methodBlackList);
//        }
//
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "Script2.b")
//    }
//
//    void testForNameSecurity() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            import java.util.ArrayList
//            def a = Math.class.forName('java.util.ArrayList').newInstance()
//            a.add(new ArrayList())
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        def methodList = ["java.util.ArrayList.add"]
//        configuration.addCompilationCustomizers(customizer)
//        customizer.with {
//            setReceiversBlackList(methodList);
//        }
//
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.util.ArrayList.add")
//    }
//
//    void testForNameSecurityFromInt() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            import java.util.ArrayList
//            def a = 5.class.forName('java.util.ArrayList').newInstance()
//            a.add(new ArrayList())
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        def methodList = ["java.util.ArrayList.add"]
//        configuration.addCompilationCustomizers(customizer)
//        customizer.with {
//            setReceiversBlackList(methodList);
//        }
//
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.util.ArrayList.add")
//    }
//
//    void testForNameSecurityNewify() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            @Newify
//            def create() {
//                java.util.ArrayList.new();
//            }
//            a = create()
//            a.add(new ArrayList())
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        def methodList = ["java.util.ArrayList.add"]
//        configuration.addCompilationCustomizers(customizer)
//        customizer.with {
//            setReceiversBlackList(methodList);
//        }
//
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.util.ArrayList.add")
//    }
//
//    void testForNameSecurityWithMethodNameInString() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            @Newify
//            def create() {
//                java.util.ArrayList.new();
//            }
//            a = create()
//            a.'add'(new ArrayList())
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        def methodList = ["java.util.ArrayList.add"]
//        configuration.addCompilationCustomizers(customizer)
//        customizer.with {
//            setReceiversBlackList(methodList);
//        }
//
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.util.ArrayList.add")
//    }
//
//    void testForMethodClassCodeInsideScript() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            class A {
//                public void b() {
//                    def c = new ArrayList()
//                    c.clear()
//                }
//            }
//            new A().b()
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        configuration.addCompilationCustomizers(customizer)
//        customizer.methodDefinitionAllowed = true
//        def methodList = ["java.util.ArrayList.clear"]
//        customizer.with {
//            setReceiversBlackList(methodList);
//        }
//
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.util.ArrayList.clear")
//    }
//
//    void testConstructorWithClassForName() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            Class.forName('java.util.ArrayList').newInstance()
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        configuration.addCompilationCustomizers(customizer)
//        customizer.methodDefinitionAllowed = true
//        def methodList = ["java.util.ArrayList", "java.lang.Class"]
//        customizer.with {
//            setReceiversBlackList(methodList);
//        }
//
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.lang.Class")
//    }
//
//    void testConstructorWithClassForNameComingFromAnotherClass() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            Math.class.forName('java.util.ArrayList').newInstance()
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        configuration.addCompilationCustomizers(customizer)
//        customizer.methodDefinitionAllowed = true
//        def methodList = ["java.util.ArrayList", "java.util.ArrayList.ctor", "java.lang.Class"]
//        customizer.with {
//            setReceiversBlackList(methodList);
//        }
//
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.lang.Class")
//    }
//
//    void testSimpleConstructor() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            new java.util.ArrayList()
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        configuration.addCompilationCustomizers(customizer)
//        customizer.methodDefinitionAllowed = true
//        def methodList = ["java.util.ArrayList.ctor"]
//        customizer.with {
//            setReceiversBlackList(methodList);
//        }
//
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.util.ArrayList")
//    }
//
//    void testConstructorWithNewify() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            @Newify
//            def create() {
//                java.util.ArrayList.new();
//            }
//            a = create()
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        configuration.addCompilationCustomizers(customizer)
//        customizer.methodDefinitionAllowed = true
//        def methodList = ["java.util.ArrayList"]
//        customizer.with {
//            setReceiversBlackList(methodList);
//        }
//
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.util.ArrayList")
//    }
//
//    void testIfStatementForRuntime() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            def bool() {
//                ((Object)new ArrayList()).'add'("");
//                return true
//            }
//            if(bool()) {
//                new ArrayList().clear()
//            }
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        configuration.addCompilationCustomizers(customizer)
//        customizer.methodDefinitionAllowed = true
//        def methodList = ["java.util.ArrayList.add"]
//        customizer.with {
//            setReceiversBlackList(methodList);
//        }
//
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.util.ArrayList.add")
//    }
//
//    void testReturnStatementForRuntime() {
//        def shell = new GroovyShell(configuration)
//        String script = """
//            class A {
//                public boolean  bool() {
//                    ((Object)new ArrayList()).'add'("");
//                    return true
//                }
//            }
//            return new A().'bool'()
//        """
//        shell.evaluate(script)
//        // no error means success
//
//        configuration.addCompilationCustomizers(customizer)
//        customizer.methodDefinitionAllowed = true
//        def methodList = ["java.util.ArrayList.add"]
//        customizer.with {
//            setReceiversBlackList(methodList);
//        }
//
//        assert hasSecurityException ({
//            shell.evaluate(script)
//        }, "java.util.ArrayList.add")
//    }
}
