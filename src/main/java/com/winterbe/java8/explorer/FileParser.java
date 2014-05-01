package com.winterbe.java8.explorer;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

/**
 * @author Benjamin Winterberg
 */
public class FileParser {

    private static final String JAVA_VERSION = "1.8";

    public Optional<TypeInfo> parse(File file, String path, Statistics statistics) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("file does not exist: " + file.getAbsolutePath());
        }

        Document document = Jsoup.parse(file, "UTF-8", "http://download.java.net/jdk8/docs/api/");

        try {
            return getTypeInfo(document, path, statistics);
        }
        catch (Exception e) {
            statistics.failures++;
            System.err.println("failed to parse file " + file.getAbsolutePath() + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<TypeInfo> getTypeInfo(Document document, String path, Statistics statistics) {
        String title = document.title();
        String typeName = StringUtils.substringBefore(title, " ");

        Element body = document.body();

        if (!body.html().contains(JAVA_VERSION)) {
            return Optional.empty();
        }

        String fullType = body
                .select(".header h2")
                .first()
                .text();

        String packageName = body
                .select(".header > .subTitle")
                .last()
                .text();

        String declaration = body
                .select(".description > ul > li > pre")
                .first()
                .html();

        Elements elements1 = body.select(".contentContainer .description dd");
        boolean newType = elements1.stream().anyMatch(dd -> dd.text().equals(JAVA_VERSION));

        TypeInfo typeInfo = new TypeInfo();
        typeInfo.setName(typeName);
        typeInfo.setFullType(fullType);

        FileType fileType = FileType.ofFullType(fullType);
        typeInfo.setFileType(fileType);

        typeInfo.setPackageName(packageName);
        typeInfo.setPath(path);
        typeInfo.setNewType(newType);
        typeInfo.setDeclaration(declaration);

        Elements elements = body.select(".contentContainer .details > ul > li > ul > li");
        for (Element element : elements) {
            MemberType type = MemberType.UNKNOWN;
            Element a = element.child(0);
            String name = a.attr("name");
            switch (name) {
                case "constructor.detail":
                    type = MemberType.CONSTRUCTOR;
                    break;
                case "method.detail":
                    type = MemberType.METHOD;
                    break;
                case "field.detail":
                    type = MemberType.FIELD;
                    break;
            }

            for (Element ul : element.select("> ul")) {
                String methodName = ul.select("h4").text();
                Elements dds = ul.select("dl > dd");
                for (Element dd : dds) {
                    statistics.maxMembers++;

                    if (newType || dd.text().equals(JAVA_VERSION)) {
                        MemberInfo memberInfo = new MemberInfo();
                        memberInfo.setType(type);
                        memberInfo.setName(methodName);
                        memberInfo.setDeclaration(ul.select("pre").first().html());
                        typeInfo.getMembers().add(memberInfo);

                        statistics.newMembers++;

                        switch (memberInfo.getType()) {
                            case METHOD:
                                statistics.newMethods++;
                                if (fileType == FileType.INTERFACE && memberInfo.isDefault()) {
                                    statistics.newDefaulInterfacetMethods++;
                                }
                                if (fileType == FileType.INTERFACE && memberInfo.isStatic()) {
                                    statistics.newStaticInterfaceMethods++;
                                }
                                break;
                            case CONSTRUCTOR:
                                statistics.newConstructors++;
                                break;
                            case FIELD:
                                statistics.newFields++;
                                break;
                        }

                        break;
                    }
                }
            }
        }

        if (typeInfo.getMembers().isEmpty()) {
            return Optional.empty();
        }

        if (newType) {
            statistics.newFiles++;

            switch (fileType) {
                case CLASS:
                    statistics.newClasses++;
                    break;
                case INTERFACE:
                    statistics.newInterfaces++;
                    break;
                case ENUM:
                    statistics.newEnums++;
                    break;
            }
        }

        if (typeInfo.isFunctionalInterface()) {
            statistics.maxFunctionalInterfaces++;
        }

        return Optional.of(typeInfo);
    }
}