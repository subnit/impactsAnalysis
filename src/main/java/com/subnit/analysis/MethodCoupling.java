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

import java.util.Comparator;

/**
 * This class represents a coupling between two methods.
 * We should be able to represent the majority of the
 * {@link java.lang.invoke.CallSite}s as couplings.
 */

public class MethodCoupling {

    private Method source;

    private Method target;


    public MethodCoupling() {}
    public MethodCoupling(Method source, Method target) {
        this.source = source;
        this.target = target;
    }

    public static Comparator<MethodCoupling> COMPARATOR = (a, b) -> {
        final int comparison = Method.COMPARATOR.compare(a.getSource(), b.getSource());
        if (comparison == 0) {
            return Method.COMPARATOR.compare(a.getTarget(), b.getTarget());
        }
        return comparison;
    };


    public Method getSource() {
        return source;
    }

    public void setSource(Method source) {
        this.source = source;
    }

    public Method getTarget() {
        return target;
    }

    public void setTarget(Method target) {
        this.target = target;
    }
}
