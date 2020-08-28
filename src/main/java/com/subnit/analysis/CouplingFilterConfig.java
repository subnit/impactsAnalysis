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


public class CouplingFilterConfig {


    private couplingFilter include;
    private couplingFilter exclude;

    public couplingFilter getInclude() {
        return include;
    }

    public void setInclude(couplingFilter include) {
        this.include = include;
    }

    public couplingFilter getExclude() {
        return exclude;
    }

    public void setExclude(couplingFilter exclude) {
        this.exclude = exclude;
    }

    public void fillPattern() {
       fillPattern(include);
       fillPattern(exclude);
    }


    public void fillPattern(couplingFilter filter) {
        if (filter.getSourceClass() != null) {
            filter.setSourceClassPattern(Pattern.compile(filter.getSourceClass()));
        }
        if (filter.getSourceMethod() != null) {
            filter.setSourceMethodPattern(Pattern.compile(filter.getSourceMethod()));
        }
        if (filter.getSourcePackage() != null) {
            filter.setSourcePackagePattern(Pattern.compile(filter.getSourcePackage()));
        }
        if (filter.getTargetClass() != null) {
            filter.setTargetClassPattern(Pattern.compile(filter.getTargetClass()));
        }
        if (filter.getTargetMethod() != null) {
            filter.setTargetMethodPattern(Pattern.compile(filter.getTargetMethod()));
        }
        if (filter.getTargetPackage() != null) {
            filter.setTargetPackagePattern(Pattern.compile(filter.getTargetPackage()));
        }
    }
}
