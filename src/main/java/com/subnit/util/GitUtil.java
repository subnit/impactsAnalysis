package com.subnit.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;

/**
 * description:
 * date : create in 上午9:56 2020/8/29
 * modified by :
 *
 * @author subo
 */
public class GitUtil {
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

    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }


    public static void main(String[] args) {
        String userName = "";
        String password = "";
        String repo = "https://github.com/subnit/impactsAnalysis.git";
        String branch = "master";
        String directory = "/Users/huihui/gitTemp/" + branch;

        cloneGit(userName, password, repo, branch, directory);
    }





}
