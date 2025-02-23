package com.dji.sdk.sample.internal.controller;


import com.dji.sdk.sample.internal.utils.ToastUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

public class MySQLDatabaseHelper
{
    // 数据库配置
    private static final String DB_URL = "jdbc:mysql://47.93.170.238:13306/liuchang";
    private static final String DB_USER = "liuchang";
    private static final String DB_PASSWORD = "4RJHdessDSHyhMMf";
    private static Connection conn;

    public static Connection getConn(){
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            System.err.println("SQL Exception: " + e.getMessage());
            e.printStackTrace();
        }

        return conn;
    }

    public static void testConnect(){
        String url = "jdbc:mysql://47.93.170.238:13306/liuchang"; // 替换为您的数据库URL
        String user = "liuchang"; // 替换为您的数据库用户名
        String password = "4RJHdessDSHyhMMf"; // 替换为您的数据库密码

        try {
            // 加载 MySQL JDBC 驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 建立连接
            conn = DriverManager.getConnection(url, user, password);

            // 创建语句对象
            Statement stmt = conn.createStatement();

            // 执行查询
            String sql = "SELECT a, b FROM test";
            ResultSet rs = stmt.executeQuery(sql);

            // 处理结果集
            while (rs.next()) {
                int a = rs.getInt("a");
                int b = rs.getInt("b");
                System.out.println("a = " + a + ", b = " + b);
            }

            // 关闭资源
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // 插入无人机飞行数据
    public static void insertDroneData(
            double longitude, double latitude, float velocityX, float velocityY, float velocityZ,
            float altitude, double pitch, double roll, double yaw
    ) {
        new Thread(() -> {
            Connection connection = null;
            PreparedStatement preparedStatement = null;

            try {
                // 加载驱动
                Class.forName("com.mysql.jdbc.Driver");

                // 建立连接
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

                // SQL 插入语句
                String sql = "INSERT INTO droneAttitude " +
                        "(longitude, latitude, velocityX, velocityY, velocityZ, altitude, pitch, roll, yaw, time) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

                preparedStatement = connection.prepareStatement(sql);

                // 设置参数
                preparedStatement.setDouble(1, longitude);
                preparedStatement.setDouble(2, latitude);
                preparedStatement.setFloat(3, velocityX);
                preparedStatement.setFloat(4, velocityY);
                preparedStatement.setFloat(5, velocityZ);
                preparedStatement.setFloat(6, altitude);
                preparedStatement.setDouble(7, pitch);
                preparedStatement.setDouble(8, roll);
                preparedStatement.setDouble(9, yaw);

                // 执行插入操作
                preparedStatement.executeUpdate();
                System.out.println("Drone data inserted successfully!");

            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            } finally {
                // 关闭连接
                try {
                    if (preparedStatement != null) preparedStatement.close();
                    if (connection != null) connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
