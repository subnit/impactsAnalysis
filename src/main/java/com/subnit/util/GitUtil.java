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
    public static String cloneGit() throws GitAPIException {
        CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider("subnit@163.com", "tiantian0971");
        Git git = Git.cloneRepository().setURI("https://github.com/subnit/impactsAnalysis.git")
                .setBranch("master")
                .setDirectory(new File("/Users/huihui/gitTemp"))
                .setCredentialsProvider(credentialsProvider)
                .call();
        return "";
    }

    public static void main(String[] args) {
        try {
            cloneGit();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }





}
