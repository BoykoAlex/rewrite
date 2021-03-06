/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.RefactorVisitorTest

interface UseStaticImportTest : RefactorVisitorTest {
    @Test
    fun replaceWithStaticImports(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(
                """
                    package asserts;
                    
                    public class Assert {
                        public static void assertTrue(boolean b) {}
                        public static void assertFalse(boolean b) {}
                        public static void assertEquals(int m, int n) {}
                    }
                """
            ),
            visitors = listOf(
                UseStaticImport().apply { setMethod("asserts.Assert assert*(..)") }
            ),
            before = """
                package test;
                
                import asserts.Assert;
                
                class Test {
                    void test() {
                        Assert.assertTrue(true);
                        Assert.assertFalse(false);
                        Assert.assertEquals(1, 2);
                    }
                }
            """,
            after = """
                package test;
                
                import static asserts.Assert.*;
                
                class Test {
                    void test() {
                        assertTrue(true);
                        assertFalse(false);
                        assertEquals(1, 2);
                    }
                }
            """
    )

    @Test
    fun junit5Assertions(jp: JavaParser) = assertRefactored(
            parser = JavaParser.fromJavaVersion()
                    .classpath(JavaParser.dependenciesFromClasspath("junit-jupiter-api"))
                    .build(),
            visitors = listOf(UseStaticImport().apply {
                setMethod("org.junit.jupiter.api.Assertions assert*(..)")
            }),
            before = """
                package org.openrewrite;

                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.Assertions;

                class Sample {
                    @Test
                    void sample() {
                        Assertions.assertEquals(42, 21*2);
                    }
                }
            """.trimIndent(),
            after = """
                package org.openrewrite;

                import org.junit.jupiter.api.Test;
                
                import static org.junit.jupiter.api.Assertions.assertEquals;

                class Sample {
                    @Test
                    void sample() {
                        assertEquals(42, 21*2);
                    }
                }
            """.trimIndent()
    )
}
