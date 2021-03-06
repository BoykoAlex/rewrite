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
package org.openrewrite.yaml;

import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.List;

public class CoalesceProperties extends YamlRefactorVisitor {
    @Override
    public Yaml visitMapping(Yaml.Mapping mapping) {
        Yaml.Mapping m = refactor(mapping, super::visitMapping);

        boolean changed = false;
        List<Yaml.Mapping.Entry> entries = new ArrayList<>();

        for (Yaml.Mapping.Entry entry : m.getEntries()) {
            if (entry.getValue() instanceof Yaml.Mapping) {
                Yaml.Mapping valueMapping = (Yaml.Mapping) entry.getValue();
                if (valueMapping.getEntries().size() == 1) {
                    Yaml.Mapping.Entry subEntry = valueMapping.getEntries().iterator().next();
                    Yaml.Scalar coalescedKey = entry.getKey().withValue(entry.getKey().getValue() + "." + subEntry.getKey().getValue());

                    entries.add(entry.withKey(coalescedKey)
                            .withValue(subEntry.getValue()));

                    andThen(new ShiftFormatLeft(subEntry.getValue(),
                            formatter.wholeSourceIndent().getIndentToUse()));

                    changed = true;
                } else {
                    entries.add(entry);
                }
            } else {
                entries.add(entry);
            }
        }

        if (changed) {
            m = m.withEntries(entries);
        }

        return m;
    }
}
