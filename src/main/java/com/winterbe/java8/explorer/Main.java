package com.winterbe.java8.explorer;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * @author Benjamin Winterberg
 */
public class Main {

    public static void main(String[] args) throws IOException {
        String basePath = System.getProperty("basePath");

        System.out.println("parsing files from basePath: " + basePath);

        FileWalker fileWalker = new FileWalker();
        ExplorerResult result = fileWalker.walk(basePath);

        System.out.println(result.getStatistics());

        List<TypeInfo> typeInfos = result.getTypeInfos();
        typeInfos.sort(Comparator.comparing(t -> t.getPackageName()));

        System.out.println("creating site");

        SiteCreator siteCreator = new SiteCreator();
        siteCreator.createSite(result);

        System.out.println("done");
    }

}