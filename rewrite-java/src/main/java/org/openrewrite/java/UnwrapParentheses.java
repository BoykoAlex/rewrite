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
package org.openrewrite.java;

import org.openrewrite.Cursor;
import org.openrewrite.java.tree.J;

public class UnwrapParentheses {
    public static class Scoped extends JavaRefactorVisitor {
        private final J.Parentheses<?> scope;

        public Scoped(J.Parentheses<?> scope) {
            this.scope = scope;
            setCursoringOn();
        }

        @Override
        public <T extends J> J visitParentheses(J.Parentheses<T> parens) {
            return scope.isScope(parens) && isUnwrappable(getCursor()) ?
                    parens.getTree().withFormatting(parens.getFormatting()) :
                    super.visitParentheses(parens);
        }

        public static boolean isUnwrappable(Cursor parensScope) {
            if (!(parensScope.getTree() instanceof J.Parentheses)) {
                return false;
            }
            J parent = parensScope.getParentOrThrow().getTree();
            return !(parent instanceof J.DoWhileLoop.While ||
                    parent instanceof J.If ||
                    parent instanceof J.Switch ||
                    parent instanceof J.Synchronized ||
                    parent instanceof J.Try.Catch ||
                    parent instanceof J.TypeCast ||
                    parent instanceof J.WhileLoop);
        }
    }
}
