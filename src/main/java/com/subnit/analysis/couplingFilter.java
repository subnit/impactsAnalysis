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

/**
 * The coupling filter specifies the RegEx patterns to filter the couplings found at analysis,
 * before generating the output data.
 */
public class couplingFilter {

    private  String sourcePackage;
    private  String sourceClass;
    private  String sourceMethod;
    private  String targetPackage;
    private  String targetClass;
    private  String targetMethod;

    private  Pattern sourcePackagePattern;
    private  Pattern sourceClassPattern;
    private  Pattern sourceMethodPattern;
    private  Pattern targetPackagePattern;
    private  Pattern targetClassPattern;
    private  Pattern targetMethodPattern;

    public String getSourcePackage() {
        return sourcePackage;
    }

    public void setSourcePackage(String sourcePackage) {
        this.sourcePackage = sourcePackage;
    }

    public String getSourceClass() {
        return sourceClass;
    }

    public void setSourceClass(String sourceClass) {
        this.sourceClass = sourceClass;
    }

    public String getSourceMethod() {
        return sourceMethod;
    }

    public void setSourceMethod(String sourceMethod) {
        this.sourceMethod = sourceMethod;
    }

    public String getTargetPackage() {
        return targetPackage;
    }

    public void setTargetPackage(String targetPackage) {
        this.targetPackage = targetPackage;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }

    public String getTargetMethod() {
        return targetMethod;
    }

    public void setTargetMethod(String targetMethod) {
        this.targetMethod = targetMethod;
    }

    public Pattern getSourcePackagePattern() {
        return sourcePackagePattern;
    }

    public void setSourcePackagePattern(Pattern sourcePackagePattern) {
        this.sourcePackagePattern = sourcePackagePattern;
    }

    public Pattern getSourceClassPattern() {
        return sourceClassPattern;
    }

    public void setSourceClassPattern(Pattern sourceClassPattern) {
        this.sourceClassPattern = sourceClassPattern;
    }

    public Pattern getSourceMethodPattern() {
        return sourceMethodPattern;
    }

    public void setSourceMethodPattern(Pattern sourceMethodPattern) {
        this.sourceMethodPattern = sourceMethodPattern;
    }

    public Pattern getTargetPackagePattern() {
        return targetPackagePattern;
    }

    public void setTargetPackagePattern(Pattern targetPackagePattern) {
        this.targetPackagePattern = targetPackagePattern;
    }

    public Pattern getTargetClassPattern() {
        return targetClassPattern;
    }

    public void setTargetClassPattern(Pattern targetClassPattern) {
        this.targetClassPattern = targetClassPattern;
    }

    public Pattern getTargetMethodPattern() {
        return targetMethodPattern;
    }

    public void setTargetMethodPattern(Pattern targetMethodPattern) {
        this.targetMethodPattern = targetMethodPattern;
    }
}
