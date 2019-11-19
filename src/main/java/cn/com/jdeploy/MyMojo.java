package cn.com.jdeploy;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Goal which touches a timestamp file.
 *
 * @goal deploy
 */
@Mojo(name = "deploy")
public class MyMojo extends AbstractMojo
{
    /**
     * @parameter expression="${webip}" default-value="91.10:220.21"
     */
    private String webip;

    /**
     * @parameter expression="${javaPath}" default-value="E:\\work-mkq2\\SHMBank"
     */
    private String javaPath;

    /**
     * @parameter expression="${timeInterval}" default-value="3"
     */
    // 单元：分钟
    private int timeInterval;

    private boolean envFlag = false;

    /**
     * @parameter expression="${serverName}" default-value="SHMBank"
     */
    private String serverName;

    /**
     * @parameter expression="${sleepTime}" default-value="10"
     */
    private long sleepTime;


    public void execute()
        throws MojoExecutionException
    {
        //javaPath = "/Users/jiangjun/Desktop/SHMBank_11";


        String result = readModeFile();
        if(result == null || "".equals(result)){
            System.out.println("mode.txt context is empty");

        }else{
            result = replaceIp(result);
            // 获取java文件在timeInterval间隔时间内是否更新过
            String flag = getJavaFileUpdate(javaPath);
            result = result.replace("$flag", flag).replace("$serverName", serverName);
            if(envFlag){
                result = result.replace("$buildweb", buildweb()).replace("$buildapp", buildapp());
            }else{
                result = result.replace("$buildweb","").replace("$buildapp","");
            }
            // 写入pom.xml文件
            writePomFile(result);
            try {
                System.out.println("sleep time: " + sleepTime + "s");
                int i = 0;
                while(i < sleepTime){
                    Thread.currentThread().sleep(1000);
                    i += 1;
                    System.out.println("leftover time: "+ (sleepTime - i) + "s");
                }
                System.out.println("jdeploy success");

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private String replaceIp(String result){
        // 获取发版的ip地址
        Map<String ,String > map = dealIp();
        result = result.replace("$web",map.get("web")).
                replace("$app", map.get("app"));
        if(envFlag){
            result = result.replace("$wweb", map.get("web2")).
                    replace("$aapp",map.get("app2"));
        }else{
            result = result.replaceAll("<ipwebaddr2.*?wweb.*?>\n","").
                    replaceAll("<ipappaddr2.*?aapp.*?>","");
        }

        return result;
    }

    private String getJavaFileUpdate(String path){
        File file = new File(path);
        File[] files = file.listFiles();
        String webflag = "0";
        if(files == null || files.length <= 0){
            return webflag;
        }
        for(File f : files){
            if(f.isDirectory()){
                webflag = getJavaFileUpdate(f.getPath());
                if("1".equals(webflag)){
                    break;
                }
            }else if(f.isFile() && f.getName().endsWith(".java")){
                long lastModified = f.lastModified();
                Date date = new Date(lastModified);
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MINUTE, -timeInterval);
                boolean flag = date.after(calendar.getTime());
                if(flag){
                    System.out.println("[DEBUG] --- update java file: "+f.getName());
                    webflag = "1";
                    break;
                }
            }
        }

        return webflag;
    }

    private Map<String, String> dealIp(){
        Map<String, String> map = new HashMap<String, String>();
        String[] strs = webip.split(":");
        if(strs.length > 1){
            String[] ips1 = getIps(strs[0]);
            String[] ips2 = getIps(strs[1]);
            map.put("web", ips1[0]);
            map.put("app", ips1[1]);
            map.put("web2", ips2[0]);
            map.put("app2", ips2[1]);
            envFlag = true;
        }else if(strs.length == 1){
            String[] ips1 = getIps(strs[0]);
            map.put("web", ips1[0]);
            map.put("app", ips1[1]);
            map.put("web2", "");
            map.put("app2", "");
            envFlag = false;
        }

        return map;
    }

    private String[] getIps(String webip){
        webip = resovlerIp(webip);
        String[] strs = new String[2];
        if("91.10".equals(webip)){//UAT
            strs[0]="10.240.91.10";
            strs[1]="10.240.91.11";
        }else if("44.95".equals(webip)){//UAT2
            strs[0]="10.240.44.95";
            strs[1]="10.240.44.94";
        }else if("91.14".equals(webip)){//MINI
            strs[0]="10.240.91.14";
            strs[1]="10.240.91.15";
        }else if("12.31".equals(webip)){//grep
            strs[0]="10.240.12.31";
            strs[1]="10.240.92.41";
        }else if("220.21".equals(webip)){//wai wang
            strs[0]="10.240.220.21";
            strs[1]="10.240.220.26";
        }else{
            strs[0]="10.240.91.10";
            strs[1]="10.240.91.11";
        }

        return strs;
    }

    private String resovlerIp(String webip){
        if("UAT".equalsIgnoreCase(webip)){
            return "91.10";
        }else if("UAT2".equalsIgnoreCase(webip)){
            return "44.95";
        }else if("MINI".equalsIgnoreCase(webip)){
            return "91.14";
        }else if("grep".equalsIgnoreCase(webip)){
            return "12.31";
        }else if("web".equalsIgnoreCase(webip)){
            return "220.21";
        }
        return webip;
    }

    /**
     * 读取mode.txt
     * @return
     */
    private String readModeFile(){
        InputStream inputStream = MyMojo.class.getResourceAsStream("/mode.txt");
        BufferedReader br = null;
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream,"UTF-8");
            br = new BufferedReader(inputStreamReader);
            String s = "";
            StringBuilder sb = new StringBuilder();
            while((s = br.readLine()) != null){
                sb.append(s+"\n");
            }
            return sb.toString();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(br != null){
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void writePomFile(String result){
        BufferedWriter bw = null;
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                    new FileOutputStream(javaPath+File.separator+"pom.xml"),"UTF-8");
            bw = new BufferedWriter(outputStreamWriter);
            bw.write(result);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private String buildweb(){

        return "<plugin>\n" +
                "\t        \t<groupId>org.codehaus.mojo</groupId>\n" +
                "\t\t\t    <artifactId>wagon-maven-plugin</artifactId>\n" +
                "\t\t\t    <version>1.0</version>\n" +
                "\t        \t<executions>\n" +
                "\t        \t\t<execution>\n" +
                "\t        \t\t\t<id>tar-deploy3</id>\n" +
                "\t        \t\t\t<phase>package</phase>\n" +
                "\t        \t\t\t<goals>\n" +
                "\t        \t\t\t\t<goal>upload-single</goal>\n" +
                "\t        \t\t\t\t<goal>sshexec</goal>\n" +
                "\t        \t\t\t</goals>\n" +
                "\t        \t\t\t<configuration>\n" +
                "\t\t\t        \t\t<fromFile>target/iweb_${serverName.version}.tar</fromFile>\n" +
                "\t\t\t        \t\t<url>scp://mobile:abcd1234@${ipwebaddr2.version}/mobile/www</url>\n" +
                "\t\t\t        \t\t<displayCommandOutputs>true</displayCommandOutputs>\n" +
                "\t\t\t        \t\t<commands>\n" +
                "\t\t\t        \t\t\t<command>cd /mobile/www/iweb_${serverName.version};tar -xf /mobile/www/iweb_${serverName.version}.tar;cd /mobile/jiangjun;sh md.sh</command>\n" +
                "\t\t\t        \t\t</commands>\n" +
                "\t\t\t        \t</configuration>\n" +
                "\t        \t\t</execution>\n" +
                "\t        \t</executions>\n" +
                "\t        </plugin>";
    }

    private String buildapp(){

        return "<plugin>\n" +
                "\t        \t<groupId>org.codehaus.mojo</groupId>\n" +
                "\t\t\t    <artifactId>wagon-maven-plugin</artifactId>\n" +
                "\t\t\t    <version>1.0</version>\n" +
                "\t        \t<executions>\n" +
                "\t        \t\t<execution>\n" +
                "\t        \t\t\t<id>upload-deploy3</id>\n" +
                "\t        \t\t\t<phase>package</phase>\n" +
                "\t        \t\t\t<goals>\n" +
                "\t        \t\t\t\t<goal>upload-single</goal>\n" +
                "\t        \t\t\t\t<goal>sshexec</goal>\n" +
                "\t        \t\t\t</goals>\n" +
                "\t        \t\t\t<configuration>\n" +
                "\t\t\t        \t\t<fromFile>target/${serverName.version}.war</fromFile>\n" +
                "\t\t\t        \t\t<url>scp://mobile:abcd1234@${ipappaddr2.version}/mobile/app/upload</url>\n" +
                "\t\t\t        \t\t<displayCommandOutputs>true</displayCommandOutputs>\n" +
                "\t\t\t        \t\t<commands>\n" +
                "\t\t\t        \t\t    <!-- <command>package -e</command> -->\n" +
                "\t\t\t        \t\t\t<!-- <command>sh /mobile/app/startAndStop/startAndStop.sh</command> -->\n" +
                "\t\t\t        \t\t\t<command>sh /mobile/jiangjun/start.sh ${webflag.version} 2>/dev/null</command>\n" +
                "\t\t\t        \t\t</commands>\n" +
                "\t\t\t        \t</configuration>\n" +
                "\t        \t\t</execution>\n" +
                "\t        \t</executions>\n" +
                "\t        </plugin>";
    }
}
