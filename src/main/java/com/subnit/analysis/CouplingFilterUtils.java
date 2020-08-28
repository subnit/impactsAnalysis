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

import java.util.regex.Pattern;

public final class CouplingFilterUtils {

    private CouplingFilterUtils() {}

    public static boolean filterMethodCoupling(final CouplingFilterConfig couplingFilterConfig,
                                               final MethodCoupling coupling) {
        return  matchCoupling(couplingFilterConfig.getInclude(), coupling) && !matchCoupling(couplingFilterConfig.getExclude(), coupling);
    }


    static boolean matchCoupling(final couplingFilter filter, final MethodCoupling coupling) {
        return matchString(filter.getSourcePackagePattern(), coupling.getSource().getPackageName()) &&
                matchString(filter.getSourceClassPattern(), coupling.getSource().getSimpleClassName()) &&
                matchString(filter.getSourceMethodPattern(), coupling.getSource().getMethodName()) &&
                matchString(filter.getTargetPackagePattern(), coupling.getTarget().getPackageName()) &&
                matchString(filter.getTargetClassPattern(), coupling.getTarget().getSimpleClassName()) &&
                matchString(filter.getTargetMethodPattern(), coupling.getTarget().getMethodName());
    }

    static boolean matchString(Pattern pattern, final String string) {
        if (pattern == null) {
            return true;
        }
        return pattern.matcher(string).matches();
    }
}
