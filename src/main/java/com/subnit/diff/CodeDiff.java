package com.subnit.diff;

import com.alibaba.fastjson.JSONObject;
import com.subnit.base.data.FileUtil;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.subnit.base.data.FileUtil.deleteDir;

/**
 * description: git operation
 * date : create in 上午9:56 2020/8/29
 * modified by :
 *
 * @author subo
 */
public class CodeDiff {
    public final static String REF_HEADS = "refs/heads/";

    public static String cloneGit(String userName, String password, String repo, String branch, String directory)  {
        deleteDir(new File(directory));
        CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(userName, password);
        try {
            Git git = Git.cloneRepository().setURI(repo)
                    .setBranch(branch)
                    .setDirectory(new File(directory))
                    .setCredentialsProvider(credentialsProvider)
                    .call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return directory;
    }


    public static List<DiffEntry> getDiff(CredentialsProvider credentialsProvider, String repo, String gitBranchA, String gitBranchB, String directory) throws GitAPIException, IOException {
        deleteDir(new File(directory + "/" + gitBranchA));
        deleteDir(new File(directory + "/" + gitBranchB));
        Git gitA = Git.cloneRepository().setURI(repo)
                .setBranch(gitBranchA)
                .setDirectory(new File(directory + "/" + gitBranchA))
                .setCredentialsProvider(credentialsProvider)
                .call();
        Git gitB = Git.cloneRepository().setURI(repo)
                .setBranch(gitBranchB)
                .setDirectory(new File(directory + "/" + gitBranchB))
                .setCredentialsProvider(credentialsProvider)
                .call();
        AbstractTreeIterator oldTreeParser = prepareTreeParser(gitA.getRepository(), "refs/heads/" + gitBranchA);
        AbstractTreeIterator newTreeParser = prepareTreeParser(gitB.getRepository(), "refs/heads/" + gitBranchB);
        return gitA.diff().setOldTree(oldTreeParser).setNewTree(newTreeParser).call();
    }

    public static AbstractTreeIterator prepareTreeParser(Repository repository, String ref) throws IOException {
        Ref head = repository.exactRef(ref);
        if (head == null) {
            return null;
        }
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(head.getObjectId());
            RevTree tree = walk.parseTree(commit.getTree().getId());
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }
            walk.dispose();
            return treeParser;
        }

    }


    private static List<ClassInfo> diffMethods(String gitPath, String newBranchName, String oldBranchName, String username, String password) {
        try {
            CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
            String localDir = "D:\\gitTemp";
            deleteDir(new File(localDir));
            Git localGit = Git.cloneRepository()
                .setURI(gitPath)
                .setBranch(newBranchName)
                .setDirectory(new File(localDir))
                .setCredentialsProvider(credentialsProvider)
                .call();
            checkoutAndPull(localGit, oldBranchName);
            //  获取本地分支
            GitAdapter gitAdapter = new GitAdapter(localDir);
            gitAdapter.setCredentialsProvider(username, password);
            Git git = gitAdapter.getGit();
            Ref localBranchRef = gitAdapter.getRepository().exactRef(REF_HEADS + newBranchName);
            Ref localMasterRef = gitAdapter.getRepository().exactRef(REF_HEADS + oldBranchName);
            //  更新本地分支
            gitAdapter.checkOutAndPull(localMasterRef, oldBranchName);
            gitAdapter.checkOutAndPull(localBranchRef, newBranchName);
            //  获取分支信息
            AbstractTreeIterator newTreeParser = gitAdapter.prepareTreeParser(localBranchRef);
            AbstractTreeIterator oldTreeParser = gitAdapter.prepareTreeParser(localMasterRef);
            //  对比差异
            List<DiffEntry> diffs = git.diff().setOldTree(oldTreeParser).setNewTree(newTreeParser).setShowNameAndStatusOnly(true).call();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DiffFormatter df = new DiffFormatter(out);
            //设置比较器为忽略空白字符对比（Ignores all whitespace）
            df.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
            df.setRepository(git.getRepository());
            List<ClassInfo> allClassInfos = batchPrepareDiffMethod(gitAdapter, newBranchName, oldBranchName, df, diffs);
            return allClassInfos;
        }catch (Exception e) {
            e.printStackTrace();
        }
        return  new ArrayList<ClassInfo>();
    }

    /**
     * 多线程执行对比
     */
    private static List<ClassInfo> batchPrepareDiffMethod(final GitAdapter gitAdapter, final String branchName, final String oldBranchName, final DiffFormatter df, List<DiffEntry> diffs) {
        int threadSize = 100;
        int dataSize = diffs.size();
        int threadNum = dataSize / threadSize + 1;
        boolean special = dataSize % threadSize == 0;
        ExecutorService executorService = Executors.newFixedThreadPool(threadNum);

        List<Callable<List<ClassInfo>>> tasks = new ArrayList<Callable<List<ClassInfo>>>();
        Callable<List<ClassInfo>> task = null;
        List<DiffEntry> cutList = null;
        //  分解每条线程的数据
        for (int i = 0; i < threadNum; i++) {
            if (i == threadNum - 1) {
                if (special) {
                    break;
                }
                cutList = diffs.subList(threadSize * i, dataSize);
            } else {
                cutList = diffs.subList(threadSize * i, threadSize * (i + 1));
            }
            final List<DiffEntry> diffEntryList = cutList;
            task = new Callable<List<ClassInfo>>() {
                public List<ClassInfo> call() throws Exception {
                    List<ClassInfo> allList = new ArrayList<ClassInfo>();
                    for (DiffEntry diffEntry : diffEntryList) {
                        ClassInfo classInfo = prepareDiffMethod(gitAdapter, branchName, oldBranchName, df, diffEntry);
                        if (classInfo != null) {
                            allList.add(classInfo);
                        }
                    }
                    return allList;
                }
            };
            // 这里提交的任务容器列表和返回的Future列表存在顺序对应的关系
            tasks.add(task);
        }
        List<ClassInfo> allClassInfoList = new ArrayList<ClassInfo>();
        try {
            List<Future<List<ClassInfo>>> results = executorService.invokeAll(tasks);
            //结果汇总
            for (Future<List<ClassInfo>> future : results ) {
                allClassInfoList.addAll(future.get());
            }
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            // 关闭线程池
            executorService.shutdown();
        }
        return allClassInfoList;
    }


    /**
     * 单个差异文件对比
     */
    private synchronized static ClassInfo prepareDiffMethod(GitAdapter gitAdapter, String branchName, String oldBranchName, DiffFormatter df, DiffEntry diffEntry) {
        List<MethodInfo> methodInfoList = new ArrayList<MethodInfo>();
        try {
            String newJavaPath = diffEntry.getNewPath();
            //  排除测试类
            if (newJavaPath.contains("/src/test/java/")) {
                return null;
            }
            //  非java文件 和 删除类型不记录
            if (!newJavaPath.endsWith(".java") || diffEntry.getChangeType() == DiffEntry.ChangeType.DELETE){
                return null;
            }
            String newClassContent = gitAdapter.getBranchSpecificFileContent(branchName,newJavaPath);
            ASTGenerator newAstGenerator = new ASTGenerator(newClassContent);
            /*  新增类型   */
            if (diffEntry.getChangeType() == DiffEntry.ChangeType.ADD) {
                return newAstGenerator.getClassInfo();
            }
            /*  修改类型  */
            //  获取文件差异位置，从而统计差异的行数，如增加行数，减少行数
            FileHeader fileHeader = df.toFileHeader(diffEntry);
            List<int[]> addLines = new ArrayList<int[]>();
            List<int[]> delLines = new ArrayList<int[]>();
            EditList editList = fileHeader.toEditList();
            for(Edit edit : editList){
                if (edit.getLengthA() > 0) {
                    delLines.add(new int[]{edit.getBeginA(), edit.getEndA()});
                }
                if (edit.getLengthB() > 0 ) {
                    addLines.add(new int[]{edit.getBeginB(), edit.getEndB()});
                }
            }
            String oldJavaPath = diffEntry.getOldPath();
            String oldClassContent = gitAdapter.getBranchSpecificFileContent(oldBranchName,oldJavaPath);
            ASTGenerator oldAstGenerator = new ASTGenerator(oldClassContent);
            MethodDeclaration[] newMethods = newAstGenerator.getMethods();
            MethodDeclaration[] oldMethods = oldAstGenerator.getMethods();
            Map<String, MethodDeclaration> methodsMap = new HashMap<String, MethodDeclaration>();
            for (int i = 0; i < oldMethods.length; i++) {
                methodsMap.put(oldMethods[i].getName().toString()+ oldMethods[i].parameters().toString(), oldMethods[i]);
            }
            for (final MethodDeclaration method : newMethods) {
                // 如果方法名是新增的,则直接将方法加入List
                if (!ASTGenerator.isMethodExist(method, methodsMap)) {
                    MethodInfo methodInfo = newAstGenerator.getMethodInfo(method);
                    methodInfoList.add(methodInfo);
                    continue;
                }
                // 如果两个版本都有这个方法,则根据MD5判断方法是否一致
                if (!ASTGenerator.isMethodTheSame(method, methodsMap.get(method.getName().toString()+ method.parameters().toString()))) {
                    MethodInfo methodInfo =  newAstGenerator.getMethodInfo(method);
                    methodInfoList.add(methodInfo);
                }
            }
            return newAstGenerator.getClassInfo(methodInfoList, addLines, delLines);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }




    /**
     * 多线程执行对比
     */
    private static List<ClassInfo> batchPrepareDiffMethodForTag(final GitAdapter gitAdapter, final String newTag, final String oldTag, final DiffFormatter df, List<DiffEntry> diffs) {
        int threadSize = 100;
        int dataSize = diffs.size();
        int threadNum = dataSize / threadSize + 1;
        boolean special = dataSize % threadSize == 0;
        ExecutorService executorService = Executors.newFixedThreadPool(threadNum);

        List<Callable<List<ClassInfo>>> tasks = new ArrayList<Callable<List<ClassInfo>>>();
        Callable<List<ClassInfo>> task = null;
        List<DiffEntry> cutList = null;
        //  分解每条线程的数据
        for (int i = 0; i < threadNum; i++) {
            if (i == threadNum - 1) {
                if (special) {
                    break;
                }
                cutList = diffs.subList(threadSize * i, dataSize);
            } else {
                cutList = diffs.subList(threadSize * i, threadSize * (i + 1));
            }
            final List<DiffEntry> diffEntryList = cutList;
            task = new Callable<List<ClassInfo>>() {
                public List<ClassInfo> call() throws Exception {
                    List<ClassInfo> allList = new ArrayList<ClassInfo>();
                    for (DiffEntry diffEntry : diffEntryList) {
                        ClassInfo classInfo = prepareDiffMethodForTag(gitAdapter, newTag, oldTag, df, diffEntry);
                        if (classInfo != null) {
                            allList.add(classInfo);
                        }
                    }
                    return allList;
                }
            };
            // 这里提交的任务容器列表和返回的Future列表存在顺序对应的关系
            tasks.add(task);
        }
        List<ClassInfo> allClassInfoList = new ArrayList<ClassInfo>();
        try {
            List<Future<List<ClassInfo>>> results = executorService.invokeAll(tasks);
            //结果汇总
            for (Future<List<ClassInfo>> future : results ) {
                allClassInfoList.addAll(future.get());
            }
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            // 关闭线程池
            executorService.shutdown();
        }
        return allClassInfoList;
    }

    /**
     * 单个差异文件对比
     */
    private synchronized static ClassInfo prepareDiffMethodForTag(GitAdapter gitAdapter, String newTag, String oldTag, DiffFormatter df, DiffEntry diffEntry) {
        List<MethodInfo> methodInfoList = new ArrayList<MethodInfo>();
        try {
            String newJavaPath = diffEntry.getNewPath();
            //  排除测试类
            if (newJavaPath.contains("/src/test/java/")) {
                return null;
            }
            //  非java文件 和 删除类型不记录
            if (!newJavaPath.endsWith(".java") || diffEntry.getChangeType() == DiffEntry.ChangeType.DELETE){
                return null;
            }
            String newClassContent = gitAdapter.getTagRevisionSpecificFileContent(newTag,newJavaPath);
            ASTGenerator newAstGenerator = new ASTGenerator(newClassContent);
            /*  新增类型   */
            if (diffEntry.getChangeType() == DiffEntry.ChangeType.ADD) {
                return newAstGenerator.getClassInfo();
            }
            /*  修改类型  */
            //  获取文件差异位置，从而统计差异的行数，如增加行数，减少行数
            FileHeader fileHeader = df.toFileHeader(diffEntry);
            List<int[]> addLines = new ArrayList<int[]>();
            List<int[]> delLines = new ArrayList<int[]>();
            EditList editList = fileHeader.toEditList();
            for(Edit edit : editList){
                if (edit.getLengthA() > 0) {
                    delLines.add(new int[]{edit.getBeginA(), edit.getEndA()});
                }
                if (edit.getLengthB() > 0 ) {
                    addLines.add(new int[]{edit.getBeginB(), edit.getEndB()});
                }
            }
            String oldJavaPath = diffEntry.getOldPath();
            String oldClassContent = gitAdapter.getTagRevisionSpecificFileContent(oldTag,oldJavaPath);
            ASTGenerator oldAstGenerator = new ASTGenerator(oldClassContent);
            MethodDeclaration[] newMethods = newAstGenerator.getMethods();
            MethodDeclaration[] oldMethods = oldAstGenerator.getMethods();
            Map<String, MethodDeclaration> methodsMap = new HashMap<String, MethodDeclaration>();
            for (int i = 0; i < oldMethods.length; i++) {
                methodsMap.put(oldMethods[i].getName().toString()+ oldMethods[i].parameters().toString(), oldMethods[i]);
            }
            for (final MethodDeclaration method : newMethods) {
                // 如果方法名是新增的,则直接将方法加入List
                if (!ASTGenerator.isMethodExist(method, methodsMap)) {
                    MethodInfo methodInfo = newAstGenerator.getMethodInfo(method);
                    methodInfoList.add(methodInfo);
                    continue;
                }
                // 如果两个版本都有这个方法,则根据MD5判断方法是否一致
                if (!ASTGenerator.isMethodTheSame(method, methodsMap.get(method.getName().toString()+ method.parameters().toString()))) {
                    MethodInfo methodInfo =  newAstGenerator.getMethodInfo(method);
                    methodInfoList.add(methodInfo);
                }
            }
            return newAstGenerator.getClassInfo(methodInfoList, addLines, delLines);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static boolean branchNameExist(Git git, String branchName) throws GitAPIException {
        List<Ref> refs = git.branchList().call();
        for (Ref ref : refs) {
            if (ref.getName().contains(branchName)) {
                return true;
            }
        }
        return false;
    }

    public static void checkoutAndPull(Git git, String branchName) {
        try {
            try {
                if (branchNameExist(git, branchName)) {//如果分支在本地已存在，直接checkout即可。
                    git.checkout().setCreateBranch(false).setName(branchName).call();
                } else {//如果分支在本地不存在，需要创建这个分支，并追踪到远程分支上面。
                    git.checkout().setCreateBranch(true).setName(branchName).setStartPoint("origin/" + branchName).call();
                }
                git.pull().call();//拉取最新的提交
            } finally {
                git.close();
            }
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws GitAPIException, IOException {
        String path = "src/main/resources/token.properties";
        String userName = FileUtil.getSourcingValueBykey("username", path);
        String password = FileUtil.getSourcingValueBykey("password", path);;
        String repo = "/Users/huihui/IdeaProjects/ImpactsAnalysis";
        String branch = "master";
        String directory = "/Users/huihui/gitTemp";


        CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(userName, password);
        String gitBranchA = "master";
        String gitBranchB = "gitUtil";
        List<ClassInfo> classInfos = diffMethods(repo, gitBranchA, gitBranchB, userName, password);
        //List<DiffEntry> diff = getDiff(credentialsProvider, repo, gitBranchA, gitBranchB, directory);
        System.out.println(JSONObject.toJSONString(classInfos));
    }


}
