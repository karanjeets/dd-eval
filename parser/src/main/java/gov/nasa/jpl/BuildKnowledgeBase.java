package gov.nasa.jpl;

import gov.nasa.jpl.util.Search;
import org.apache.commons.io.IOUtils;

import java.io.*;

/**
 * Created by karanjeetsingh on 9/24/16.
 */
public class BuildKnowledgeBase {

    public static void processUrlFile(File file, String nbModelDirPath, String cosineModelDirPath) throws Exception {
        File nbModel = new File(nbModelDirPath + File.separator + file.getName().replace("input", "nb"));
        File cosineModel = new File(cosineModelDirPath + File.separator + file.getName().replace("input", "cosine"));
        BufferedReader br = null;
        BufferedWriter nbBw = null;
        BufferedWriter cosineBw = null;
        try {
            br = new BufferedReader(new FileReader(file));
            nbBw = new BufferedWriter(new FileWriter(nbModel));
            cosineBw = new BufferedWriter(new FileWriter(cosineModel));
            for (String url; (url = br.readLine()) != null;) {
                String content = null;
                try {
                    content = Search.getPageText(url);
                    if (content != null || content.trim().length() != 0) {
                        content = content.replaceAll("\\n", " ");
                        nbBw.append("1\t" + content + "\n");
                        cosineBw.append(content + "\n");
                        System.out.println("Processed - File: " + file.getAbsolutePath() + " URL: " + url);
                    } else {
                        System.out.println("No Content Found - File: " + file.getAbsolutePath() + " URL: " + url);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Extraction error in File: " + file.getAbsolutePath() + " at URL: " + url);
                }
            }
        } finally {
            IOUtils.closeQuietly(br);
            IOUtils.closeQuietly(nbBw);
            IOUtils.closeQuietly(cosineBw);
        }
    }

    public static void processUrlFile(File[] files, String nbModelDirPath, String cosineModelDirPath) throws Exception {
        for (File file: files) {
            processUrlFile(file, nbModelDirPath, cosineModelDirPath);
        }
    }

    public static void makeSafeDir(String dirPath) throws Exception {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static void main(String[] args) throws Exception {
        String urlPath = args[0];
        String outDirPath = args[1];
        String nbModelDirPath = outDirPath + File.separator + "nb-models";
        String cosineModelDirPath = outDirPath + File.separator + "cosine-models";

        File urlFile = new File(urlPath);
        makeSafeDir(nbModelDirPath);
        makeSafeDir(cosineModelDirPath);

        if (urlFile.isDirectory()) {
            processUrlFile(urlFile.listFiles(), nbModelDirPath, cosineModelDirPath);
        } else {
            processUrlFile(urlFile, nbModelDirPath, cosineModelDirPath);
        }
    }
}
