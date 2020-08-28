/*
* Copyright 2020 Expedia, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.subnit.analysis;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

public class UsageCollector implements Collector {

    private static CouplingFilterConfig DEFAULT_COUPLING_FILTER = new CouplingFilterConfig();

    private final Multimap<Method, Method> methodRefMap;

    private final CouplingFilterConfig couplingFilterConfig;

    public UsageCollector() {
        this(DEFAULT_COUPLING_FILTER);
    }

    public UsageCollector(final CouplingFilterConfig couplingFilterConfig) {
        Objects.requireNonNull(couplingFilterConfig, "couplingFilterConfig should not be null");
        this.couplingFilterConfig = couplingFilterConfig;
        this.methodRefMap = LinkedHashMultimap.create();
    }


    @Override
    public void collectMethodCoupling(final MethodCoupling coupling) {
        if (CouplingFilterUtils.filterMethodCoupling(couplingFilterConfig, coupling)) {
            methodRefMap.put(coupling.getSource(), coupling.getTarget());
        }
    }

    /**
     * Generates the efferent coupling graph for each method in the classes loaded by the class loader.
     *
     * @return The list of method couplings.
     */


    public List<MethodCoupling> getMethodCouplings() {
        return ImmutableList.copyOf(
                methodRefMap.entries().stream()
                        .map(UsageCollector::mapToMethodCoupling)
                        .sorted(MethodCoupling.COMPARATOR)
                        .collect(Collectors.toList())
        );
    }


    private static MethodCoupling mapToMethodCoupling(final Entry<Method, Method> entry) {
        return new MethodCoupling(entry.getKey(), entry.getValue());
    }
}
