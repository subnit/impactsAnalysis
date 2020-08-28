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
 * Represents a fully qualified name of a method
 */

public class Method {

    private String className;
    private String methodName;

    public Method() {}

    public Method(String className, String methodName){
        this.className = className;
        this.methodName = methodName;
    }



   public static Comparator<Method> COMPARATOR = (a, b) -> {
        final int comparison = a.getClassName().compareTo(b.getClassName());
        if (comparison == 0) {
            return a.getMethodName().compareTo(b.getMethodName());
        }
        return comparison;
    };


    /**
     * Simple class name
     * e.g.: For "foo.bar.MyClass", returns "MyClass"
     *
     * @return The simple class name.
     */
    public String getSimpleClassName() {
        final String fullName = getClassName();
        final int index = fullName.lastIndexOf('.');
        return index > 0 ? fullName.substring(index + 1) : fullName;
    }

    /**
     * Package name of the class
     * e.g.: For "foo.bar.MyClass", returns "foo.bar"
     *
     * @return The package name.
     */
    public String getPackageName() {
        final String fullName = getClassName();
        final int index = fullName.lastIndexOf('.');
        return index > 0 ? fullName.substring(0, index) : "";
    }


    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
