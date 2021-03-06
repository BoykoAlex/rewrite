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
package org.openrewrite.java.tree;

import org.openrewrite.Cursor;
import org.openrewrite.Formatting;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.JavaFormatter.enclosingIndent;

@NonNullApi
public class TreeBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TreeBuilder.class);

    private static final Pattern whitespacePrefixPattern = Pattern.compile("^\\s*");
    private static final Pattern whitespaceSuffixPattern = Pattern.compile("\\s*[^\\s]+(\\s*)");

    private final J.CompilationUnit cu;

    public TreeBuilder(J.CompilationUnit cu) {
        this.cu = cu;
    }

    public static <T extends TypeTree & Expression> T buildName(String fullyQualifiedName) {
        return buildName(fullyQualifiedName, Formatting.EMPTY);
    }

    public static <T extends TypeTree & Expression> T buildName(String fullyQualifiedName, Formatting fmt) {
        return buildName(fullyQualifiedName, fmt, randomId());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static <T extends TypeTree & Expression> T buildName(String fullyQualifiedName, Formatting fmt, UUID id) {
        String[] parts = fullyQualifiedName.split("\\.");

        String fullName = "";
        Expression expr = null;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                fullName = part;
                expr = J.Ident.build(randomId(), part, null, Formatting.EMPTY);
            } else {
                fullName += "." + part;

                Matcher whitespacePrefix = whitespacePrefixPattern.matcher(part);
                Formatting identFmt = whitespacePrefix.matches() ? format(whitespacePrefix.group(0)) : Formatting.EMPTY;

                Matcher whitespaceSuffix = whitespaceSuffixPattern.matcher(part);
                whitespaceSuffix.matches();
                Formatting partFmt = i == parts.length - 1 ? Formatting.EMPTY : format("", whitespaceSuffix.group(1));

                expr = new J.FieldAccess(
                        i == parts.length - 1 ? id : randomId(),
                        expr,
                        J.Ident.build(randomId(), part.trim(), null, identFmt),
                        (Character.isUpperCase(part.charAt(0)) || i == parts.length - 1) ?
                                JavaType.Class.build(fullName) :
                                null,
                        partFmt
                );
            }
        }

        //noinspection ConstantConditions
        return expr.withFormatting(fmt);
    }

    /**
     * Build a class-scoped declaration. A "class-scoped declaration" is anything you can put inside a class declaration.
     * Examples of such statements include method declarations, field declarations, inner class declarations, and static initializers.
     *
     * @param insertionScope The class this declaration is being inserted into.
     * @param snippet        The declaration code to insert
     * @param types          specify any
     */
    public J buildDeclaration(J.ClassDecl insertionScope, String snippet, JavaType... types) {
        JavaParser javaParser = cu.buildParser();

        // Turn this on in IntelliJ: Preferences > Editor > Code Style > Formatter Control
        // @formatter:off
        String scopeVariables = insertionScope.getFields().stream()
                .flatMap(field -> field.getVars().stream().map(v -> variableDefinitionSource(field, v)))
                .collect(joining(";\n  ", "  // variables visible in the insertion scope\n  ", ";\n")) + "\n";
        if (insertionScope.getFields().isEmpty()) {
            scopeVariables = "";
        }

        JavaType.Class[] imports = stream(types)
                .filter(Objects::nonNull)
                .filter(it -> it instanceof JavaType.Class)
                .map(JavaType.Class.class::cast)
                .toArray(JavaType.Class[]::new);

        String source = stream(imports)
                .map(i -> "import " + i.getFullyQualifiedName() + ";").collect(joining("\n", "", "\n\n")) +
                "class CodeSnippet {\n" +
                scopeVariables +
                StringUtils.trimIndent(snippet) + "\n" +
                "}";
        // @formatter:on

        if (logger.isDebugEnabled()) {
            logger.debug("Building code snippet using synthetic class:");
            logger.debug(source);
        }

        J.CompilationUnit cu = javaParser.parse(source).get(0);
        List<J> statements = cu.getClasses().get(0).getBody().getStatements();
        return new FillTypeAttributions(imports).visit(statements.get(statements.size() - 1));
    }

    public J.ClassDecl buildInnerClassDeclaration(J.ClassDecl insertionScope,
                                                  String classDeclarationSnippet,
                                                  JavaType... types) {
        J.ClassDecl cd = (J.ClassDecl) buildDeclaration(insertionScope, classDeclarationSnippet, types);
        JavaType.Class clazz = cd.getType();
        if (insertionScope.getType() != null && clazz != null) {
            JavaType.Class newClazz = JavaType.Class.build(insertionScope.getType().getFullyQualifiedName() +
                    "." + cd.getSimpleName());
            return cd.withType(newClazz);
        }
        return cd;
    }

    public J.VariableDecls buildFieldDeclaration(J.ClassDecl insertionScope,
                                                 String fieldDeclarationSnippet,
                                                 JavaType... types) {
        return (J.VariableDecls) buildDeclaration(insertionScope, fieldDeclarationSnippet, types);
    }

    public J.MethodDecl buildMethodDeclaration(J.ClassDecl insertionScope,
                                               String methodDeclarationSnippet,
                                               JavaType... types) {
        return (J.MethodDecl) buildDeclaration(insertionScope, methodDeclarationSnippet, types);
    }

    @SuppressWarnings("unchecked")
    public <T extends J> List<T> buildSnippet(Cursor insertionScope,
                                              String snippet,
                                              JavaType... imports) {
        JavaParser javaParser = cu.buildParser();

        // Turn this on in IntelliJ: Preferences > Editor > Code Style > Formatter Control
        // @formatter:off
        String source = stream(imports)
                .filter(Objects::nonNull)
                .filter(it -> it instanceof JavaType.FullyQualified)
                .map(JavaType.FullyQualified.class::cast)
                .map(i -> "import " + i.getFullyQualifiedName() + ";")
                .collect(joining("\n", "", "\n\n")) +
                "class CodeSnippet {\n" +
                new ListScopeVariables(insertionScope).visit(cu).stream()
                        .collect(joining(";\n  ", "  // variables visible in the insertion scope\n  ", ";\n")) + "\n" +
                "  // the contents of this block are the snippet\n" +
                "  {\n" +
                StringUtils.trimIndent(snippet) + "\n" +
                "  }\n" +
                "}";
        // @formatter:on

        if (logger.isDebugEnabled()) {
            logger.debug("Building code snippet using synthetic class:");
            logger.debug(source);
        }

        J.CompilationUnit cu = javaParser.parse(source).get(0);
        List<J> statements = cu.getClasses().get(0).getBody().getStatements();
        J.Block<T> block = (J.Block<T>) statements.get(statements.size() - 1);

        JavaFormatter formatter = new JavaFormatter(cu);

        return block.getStatements().stream()
                .map(stat -> {
                    ShiftFormatRightVisitor shiftRight = new ShiftFormatRightVisitor(stat, enclosingIndent(insertionScope.getTree()) +
                            formatter.findIndent(enclosingIndent(insertionScope.getTree()), stat).getEnclosingIndent(), formatter.isIndentedWithSpaces());
                    return (T) shiftRight.visit(stat);
                })
                .collect(toList());
    }

    private static class ListScopeVariables extends AbstractJavaSourceVisitor<List<String>> {
        @Nullable
        private final Cursor scope;

        private ListScopeVariables(@Nullable Cursor scope) {
            this.scope = scope;
            setCursoringOn();
        }

        @Override
        public List<String> defaultTo(@Nullable Tree t) {
            return emptyList();
        }

        @Override
        public List<String> visitCompilationUnit(J.CompilationUnit cu) {
            return scope == null ? emptyList() : super.visitCompilationUnit(cu);
        }

        @Override
        public List<String> visitVariable(J.VariableDecls.NamedVar variable) {
            if (isInSameNameScope(scope)) {
                return singletonList(variableDefinitionSource(getCursor().getParentOrThrow().getTree(), variable));
            }

            return super.visitVariable(variable);
        }
    }

    private static String variableDefinitionSource(J.VariableDecls variableDecls, J.VariableDecls.NamedVar variable) {
        JavaType type = variableDecls.getTypeExpr() == null ? variable.getType() : variableDecls.getTypeExpr().getType();
        String typeName = "";
        if (type instanceof JavaType.Class) {
            typeName = ((JavaType.Class) type).getFullyQualifiedName();
        } else if (type instanceof JavaType.ShallowClass) {
            typeName = ((JavaType.ShallowClass) type).getFullyQualifiedName();
        } else if (type instanceof JavaType.GenericTypeVariable) {
            typeName = ((JavaType.GenericTypeVariable) type).getFullyQualifiedName();
        } else if (type instanceof JavaType.Primitive) {
            typeName = ((JavaType.Primitive) type).getKeyword();
        }

        return "static " + typeName + " " + variable.getSimpleName();
    }
}
