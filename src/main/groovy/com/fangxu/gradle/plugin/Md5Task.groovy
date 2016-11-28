package com.fangxu.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.security.MessageDigest
import groovy.json.*

class Md5Task extends DefaultTask {

    def soList = [] as ArrayList
    def MessageDigest messagedigest = MessageDigest.getInstance("MD5")
    def md5Map = [:]

    @TaskAction
    def generateMd5s() {
        storeMd5ToMap()
        writeMd5JsonToFile()
    }

    def writeMd5JsonToFile() {
        String json = generateJsonString()
        println json
        String path = project.extensions.getByType(SoCheckPluginExtension.class).md5Path
        File file = new File(path);
        file.write(json)
        println file.text
    }

    def String generateJsonString() {
        String json;
        JsonOutput jsonOutput = new JsonOutput()
        json = jsonOutput.toJson(md5Map)
        return json;
    }

    def storeMd5ToMap() {
        getAllSoFiles()
        soList.each { File f ->
            String md5 = getFileMD5String(f)
            md5Map.put(f.getName(), md5)
        }
    }

    def getAllSoFiles() {
        String path = project.extensions.getByType(SoCheckPluginExtension.class).soPath
        File file = new File(path)
        File[] files = file.listFiles()
        files.each {
            File f ->
                println f.absolutePath
                soList.add(f)
        }
    }

    def String getFileMD5String(File file) {
        InputStream fis
        fis = new FileInputStream(file)
        byte[] buffer = new byte[1024]
        int numRead
        while ((numRead = fis.read(buffer)) > 0) {
            messagedigest.update(buffer, 0, numRead);
        }
        fis.close()
        return bufferToHex(messagedigest.digest())
    }

    def String bufferToHex(byte[] bytes) {
        String returnVal = "";
        for (int i = 0; i < bytes.length; i++) {
            returnVal += Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1);
        }
        return returnVal.toLowerCase();
    }
}

