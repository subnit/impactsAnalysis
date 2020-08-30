package com.subnit.analysis;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Ordering;
import com.subnit.base.data.DataUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

/**
 * description:
 * date : create in 下午4:56 2020/8/23
 * modified by :
 *
 * @author subo
 */
public class AnalysisService {


    static Predicate<String> classNameFilter = s -> true;

    public static List<ShadowClass> getAllClass(String jarURL) {
        File jarFile = new File(jarURL);

        System.out.println("加了一行...");
        final String jarFilePath = jarFile.getPath();
        final ClassPath classPath;
        try {
            final ClassLoader classLoader = creatClassLoaderForJar(jarFilePath);
            classPath = ClassPath.from(classLoader);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to create classloader for jar %s", jarFilePath), e);
        }

        final String prefix = fileNameToJarPrefix(jarFilePath);
        final Predicate<ClassPath.ClassInfo> predicateClassesOnlyFromJar =
                classInfo -> classInfo.url().toString().startsWith(prefix);
        final List<ClassPath.ClassInfo> classInfoList =
                classPath.getAllClasses()
                        .stream()
                        .filter(predicateClassesOnlyFromJar)// select classes only from the Jar
                        //.filter(c -> classNameFilter.test(c.getName()))// apply the user defined filter
                        .sorted(Ordering.natural().onResultOf(ClassPath.ClassInfo::getName))// always return the ordered list (by name)
                        .collect(toList());

        List<ShadowClass> res = new ArrayList<>();
        for (ClassPath.ClassInfo classInfo : classInfoList) {
            try {
                res.add(mapClassInfoToShadowClass(classInfo));
            } catch (IOException e) {
                throw new IllegalStateException(String.format("Unable to load class %s", classInfo.getName()), e);
            }
        }

        return  res;
    }

    public static List<MethodCoupling> getAllMethodCoupling(List<ShadowClass> classesFromJarClassLoader, String filter) {
        CouplingFilterConfig filterConfig = JSONObject.parseObject(filter, CouplingFilterConfig.class);
        filterConfig.fillPattern();
        final UsageCollector usageCollector = new UsageCollector(filterConfig);
        for (ShadowClass c : classesFromJarClassLoader) {
            new ClassVisitor(c.getClassName(), usageCollector, c.getClassBytes()).visit();
        }
        return usageCollector.getMethodCouplings();
    }

    public static Map<String, Set<String>> getMethodRelationMap(List<MethodCoupling> methodCouplings) {
        Map<String, Set<String>> res = new HashMap<>();
        methodCouplings.forEach(method -> {
            Method source = method.getSource();
            Method target = method.getTarget();
            String key = target.getClassName() + "_" + target.getMethodName();
            Set<String> parents = res.get(key);
            if (parents == null) {
                parents = new HashSet<>();
                res.put(key, parents);
            }
            parents.add(source.getClassName() + "_" + source.getMethodName());

        });
        return res;
    }

    public static void getMethodInfluence(String method, Map<String, Set<String>> methodRelations, List<String> res) {

            Set<String> parent = methodRelations.get(method);
            if (DataUtil.isNotEmpty(parent)) {
                Set<String> parentCopy = new HashSet<>(parent);
                parentCopy.removeAll(res);
                res.addAll(parent);
                parentCopy.forEach(parentMethod -> {
                    getMethodInfluence(parentMethod, methodRelations, res);
                });
            }
        }


    private static ClassLoader creatClassLoaderForJar(@Nonnull final String jarFileName) throws MalformedURLException {
        final URL jarURL = new URL(fileNameToFileProtocol(jarFileName));
        final ClassLoader bootstrapClassLoader = ClassLoader.getSystemClassLoader().getParent();
        return URLClassLoader.newInstance(new URL[] {jarURL}, bootstrapClassLoader);
    }

    static String fileNameToFileProtocol(@Nonnull final String jarFileFullPath) {
        if (jarFileFullPath.startsWith("file:")) {
            return jarFileFullPath;
        }

        return "file:" + jarFileFullPath;
    }

    static String fileNameToJarPrefix(@Nonnull final String jarFileFullPath) {
        return "jar:" + fileNameToFileProtocol(jarFileFullPath) + "!";
    }

    static ShadowClass mapClassInfoToShadowClass(@Nonnull final ClassPath.ClassInfo classInfo) throws IOException {
        return new ShadowClass(classInfo.getName(), classInfo.asByteSource().read());
    }


    public static void main(String[] args) {
        //List<ShadowClass> allClass = getAllClass("/Users/huihui/IdeaProjects/subnit-web/subnit-web-util/target/subnit-web-util-0.0.1-SNAPSHOT.jar");
        List<ShadowClass> allClass = getAllClass("/Users/huihui/.m2/repository/net/oschina/jmind/jmind-base/2.1.0/jmind-base-2.1.0.jar");
        String filter = "{\n" +
                "  \"include\": {\n" +
                "    \"targetPackage\": \"^(jmind).*$\"\n" +
                "  },\n" +
                "  \"exclude\": {\n" +
                "    \"sourcePackage\": \"^(com\\\\.google).*$\"\n" +
                "  }\n" +
                "}\n";
        List<MethodCoupling> allMethodCoupling = getAllMethodCoupling(allClass, filter);
        Map<String, Set<String>> methodRelationMap = getMethodRelationMap(allMethodCoupling);
        System.out.println(JSONObject.toJSONString(methodRelationMap));
        List<String> res = new ArrayList<>();
        res.add("jmind.base.util.DataUtil_isEmpty(Ljava/lang/String;)Z");
        getMethodInfluence("jmind.base.util.DataUtil_isEmpty(Ljava/lang/String;)Z", methodRelationMap, res);
        System.out.println(JSONObject.toJSONString(res));
    }
}
