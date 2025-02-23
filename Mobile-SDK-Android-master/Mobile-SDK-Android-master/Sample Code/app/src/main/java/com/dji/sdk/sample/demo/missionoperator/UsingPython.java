package com.dji.sdk.sample.demo.missionoperator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UsingPython {

    public void runPythonScript() {
        String path = "F:/GroudControl/my_yolov8/predict.py";  // Python 脚本路径
        String condaPath = "F:/Anaconda/envs/yolov8/python.exe";  // Anaconda 环境的 Python 可执行文件路径
        Process process;

        try {
            // 使用 Runtime 执行命令
            process = Runtime.getRuntime().exec(condaPath + " " + path);

            // 读取脚本的标准输出
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), "gbk"));
            BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream(), "gbk"));

            String line;
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();

            // 处理输出内容
            while ((line = in.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 处理错误输出
            while ((line = error.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }

            in.close();
            error.close();

            // 输出日志
            System.out.println("Output:\n" + output.toString());
            System.err.println("Error:\n" + errorOutput.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
