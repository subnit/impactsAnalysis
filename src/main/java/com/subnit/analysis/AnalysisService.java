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

    public static List<ShadowClass> getAllClass(String[] jarURL) throws MalformedURLException {
        List<ClassPath.ClassInfo> classInfoList = new ArrayList<>();
        for (String url : jarURL) {
            classInfoList.addAll(getClassInfoList(url));
        }
        ClassLoader classLoader = creatClassLoaderForJar(jarURL);


        List<ShadowClass> res = new ArrayList<>();
        for (ClassPath.ClassInfo classInfo : classInfoList) {
            try {
                res.add(mapClassInfoToShadowClass(classInfo, classLoader));

            } catch (IOException | ClassNotFoundException e) {
                throw new IllegalStateException(String.format("Unable to load class %s", classInfo.getName()), e);
            }
        }

        return  res;
    }


    private static List<ClassPath.ClassInfo> getClassInfoList(String jarURL) {
        File jarFile = new File(jarURL);

        System.out.println("加了一行...");
        final String jarFilePath = jarFile.getPath();
        final ClassPath classPath;
        final ClassLoader classLoader;
        try {
            classLoader = creatClassLoaderForJar(jarURL);
          /*  Class<?> aClass = classLoader.loadClass("com.subnit.anlysis.ClassB");
            Class<?> superclass = aClass.getSuperclass();
            Class<?>[] interfaces = aClass.getInterfaces();
            java.lang.reflect.Method[] methods = aClass.getMethods();
            java.lang.reflect.Method[] declaredMethods = aClass.getDeclaredMethods();*/
            classPath = ClassPath.from(classLoader);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Unable to create classloader for jar %s", jarURL), e);
        }

        final String prefix = fileNameToJarPrefix(jarFilePath);
        final Predicate<ClassPath.ClassInfo> predicateClassesOnlyFromJar =
                classInfo -> classInfo.url().toString().startsWith(prefix) && classInfo.url().toString().contains("com/subnit") && !classInfo.url().toString().contains("BOOT-INF/classes");
        return classPath.getAllClasses()
                        .stream()
                        .filter(predicateClassesOnlyFromJar)// select classes only from the Jar
                        //.filter(c -> classNameFilter.test(c.getName()))// apply the user defined filter
                        .sorted(Ordering.natural().onResultOf(ClassPath.ClassInfo::getName))// always return the ordered list (by name)
                        .collect(toList());
    }



    public   static List<MethodCoupling> getAllMethodCoupling(List<ShadowClass> classesFromJarClassLoader, String filter) {
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


    private static ClassLoader creatClassLoaderForJar(@Nonnull final String[] jarURL) throws MalformedURLException {
        URL[] urls = new URL[jarURL.length];
        for (int i = 0; i < jarURL.length; i++) {
           urls[i] =  new URL(fileNameToFileProtocol(jarURL[i]));
        }
        final ClassLoader bootstrapClassLoader = ClassLoader.getSystemClassLoader().getParent();
        return URLClassLoader.newInstance(urls, bootstrapClassLoader);
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

    static ShadowClass mapClassInfoToShadowClass(@Nonnull final ClassPath.ClassInfo classInfo, ClassLoader classLoader) throws IOException, ClassNotFoundException {
        if (classInfo.getName().startsWith("com.subnit.anlysis.Usage")) {
            System.out.println();
        }
        System.out.println(classInfo.getName());
        Class<?> aClass = classLoader.loadClass(classInfo.getName());
        Class<?> superclass = aClass.getSuperclass();
        Class<?>[] interfaces = aClass.getInterfaces();
        Set<String> effectClasses = new HashSet<>();
        if (superclass != null) {
            effectClasses.add(superclass.getName());
        }
        for (Class<?> anInterface : interfaces) {
            effectClasses.add(anInterface.getName());
        }
        return new ShadowClass(classInfo.getName(), classInfo.asByteSource().read(), effectClasses);
    }


    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException {
        List<String> list = new ArrayList<>();

        //List<ShadowClass> allClass = getAllClass("/Users/huihui/IdeaProjects/subnit-web/subnit-web-util/target/subnit-web-util-0.0.1-SNAPSHOT.jar");
        list.add("/Users/huihui/IdeaProjects/subnit-web/subnit-web-application/target/subnit-web-application-0.0.1-SNAPSHOT.jar");
        list.add("/Users/huihui/IdeaProjects/subnit-web/subnit-web-start/target/subnit-web-start-0.0.1-SNAPSHOT.jar");
        list.add("/Users/huihui/IdeaProjects/subnit-web/subnit-web-infrastructure/target/subnit-web-infrastructure-0.0.1-SNAPSHOT.jar");
        list.add("/Users/huihui/IdeaProjects/subnit-web/subnit-web-controller/target/subnit-web-controller-0.0.1-SNAPSHOT.jar");
        list.add("/Users/huihui/IdeaProjects/subnit-web/subnit-web-domain/target/subnit-web-domain-0.0.1-SNAPSHOT.jar");
        list.add("/Users/huihui/IdeaProjects/subnit-web/subnit-web-util/target/subnit-web-util-0.0.1-SNAPSHOT.jar");
        list.add("/Users/huihui/.m2/repository/org/ow2/asm/asm/7.1/asm-7.1.jar");
        list.add("/Users/huihui/.m2/repository/com/google/guava/guava/29.0-jre/guava-29.0-jre.jar");
        String[] urls = list.toArray(new String[0]);
        List<ShadowClass> allClass = getAllClass(urls);
        String filter = "{\n" +
                "  \"include\": {\n" +
                "    \"targetPackage\": \"^(com.subnit).*$\"\n" +
                "  },\n" +
                "  \"exclude\": {\n" +
                "    \"sourcePackage\": \"^(com\\\\.google).*$\"\n" +
                "  }\n" +
                "}\n";
        List<MethodCoupling> allMethodCoupling = getAllMethodCoupling(allClass, filter);
        System.out.println(JSONObject.toJSONString(allMethodCoupling));
        Map<String, Set<String>> methodRelationMap = getMethodRelationMap(allMethodCoupling);

        System.out.println(JSONObject.toJSONString(methodRelationMap));
        List<String> res = new ArrayList<>();
        res.add("com.subnit.anlysis.Method_getSimpleClassName()Ljava/lang/String;");
        getMethodInfluence("com.subnit.anlysis.Method_getSimpleClassName()Ljava/lang/String;", methodRelationMap, res);
        System.out.println(JSONObject.toJSONString(res));
    }
}
