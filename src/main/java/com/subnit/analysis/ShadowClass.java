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

import java.util.Set;

public class ShadowClass {

    private String className;
    private byte[] classBytes;
    private Set<String> effectClasses;
    public ShadowClass(){}
    public ShadowClass(String className , byte[] classBytes, Set<String> effectClasses){
        this.className = className;
        this.classBytes = classBytes;
        this.effectClasses = effectClasses;
    }



    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public byte[] getClassBytes() {
        return classBytes;
    }

    public void setClassBytes(byte[] classBytes) {
        this.classBytes = classBytes;
    }

    public Set<String> getEffectClasses() {
        return effectClasses;
    }

    public void setEffectClasses(Set<String> effectClasses) {
        this.effectClasses = effectClasses;
    }
}
