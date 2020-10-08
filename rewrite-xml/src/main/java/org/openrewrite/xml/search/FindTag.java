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
package org.openrewrite.xml.search;

import org.openrewrite.Tree;
import org.openrewrite.xml.AbstractXmlSourceVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlSourceVisitor;
import org.openrewrite.xml.tree.Xml;

public class FindTag extends AbstractXmlSourceVisitor<Xml.Tag> {
    private final XPathMatcher xPathMatcher;

    public FindTag(String xpath) {
        this.xPathMatcher = new XPathMatcher(xpath);
        setCursoringOn();
    }

    @Override
    public Xml.Tag defaultTo(Tree t) {
        return null;
    }

    @Override
    public Xml.Tag visitTag(Xml.Tag tag) {
        if(xPathMatcher.matches(getCursor())) {
            return tag;
        }
        return super.visitTag(tag);
    }
}
