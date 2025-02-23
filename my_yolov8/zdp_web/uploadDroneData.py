import base64
from flask import Flask, render_template, jsonify, request
import pymysql
import json

app = Flask(__name__)

# 数据库配置
DB_HOST = '47.93.170.238'  # 数据库服务器地址
DB_PORT = 13306  # 指定 MySQL 数据库的端口号
DB_USER = 'liuchang'  # 数据库用户名
DB_PASSWORD = '4RJHdessDSHyhMMf'  # 数据库密码
DB_NAME = 'liuchang'  # 数据库名称



# 创建数据库连接函数
def get_db_connection():
    connection = pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,  # 指定数据库端口
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME,
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor  # 返回字典格式的结果
    )
    return connection


@app.route('/')
def index():
    return render_template('index.html')


# 插入无人机姿态数据
@app.route('/insertDroneAttitude', methods=['POST'])
def insert_drone_data():
    try:
        # 获取请求的 JSON 数据
        data = request.get_json()

        # 获取各个字段的值
        longitude = data['longitude']
        latitude = data['latitude']
        velocityX = data['velocityX']
        velocityY = data['velocityY']
        velocityZ = data['velocityZ']
        altitude = data['altitude']
        pitch = data['pitch']
        roll = data['roll']
        yaw = data['yaw']

        # 获取数据库连接
        connection = get_db_connection()
        with connection.cursor() as cursor:
            # 插入数据的 SQL 语句
            sql = """
                INSERT INTO droneAttitude 
                (longitude, latitude, velocityX, velocityY, velocityZ, altitude, pitch, roll, yaw, time)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())
            """
            cursor.execute(sql, (longitude, latitude, velocityX, velocityY, velocityZ, altitude, pitch, roll, yaw))
            connection.commit()

        return jsonify({"status": "success", "message": "Drone data inserted successfully!"})

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)})
    finally:
        if 'connection' in locals():
            connection.close()

# 查询无人机姿态数据
@app.route('/getDroneAttitude', methods=['GET'])
def get_drone_data():
    try:
        # 获取数据库连接
        connection = get_db_connection()
        with connection.cursor() as cursor:
            # 查询数据的 SQL 语句
            sql = "SELECT * FROM droneAttitude ORDER BY time DESC LIMIT 10"
            cursor.execute(sql)
            results = cursor.fetchall()

        return jsonify({"status": "success", "data": results})

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)})
    finally:
        if 'connection' in locals():
            connection.close()
            
@app.route('/insertPhotos', methods=['POST'])
def insert_photo():
    try:
        # 获取上传的图片文件和图片名称
        photo_name = request.form['photoName']
        photo_file = request.files['photo']

        # 将图片文件内容转换为 Base64 编码字符串
        photo_data = base64.b64encode(photo_file.read()).decode('utf-8')

        # 获取数据库连接
        connection = get_db_connection()
        with connection.cursor() as cursor:
            # 插入图片数据
            sql = "INSERT INTO photos (photoName, image) VALUES (%s, %s)"
            cursor.execute(sql, (photo_name, photo_data))
            connection.commit()

        return jsonify({"status": "success", "message": "Photo inserted successfully!"})

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)})
    finally:
        if 'connection' in locals():
            connection.close()

# 查询并显示所有图片数据（Base64 解码）
@app.route('/getPhotos', methods=['GET'])
def get_photos():
    try:
        # 获取数据库连接
        connection = get_db_connection()
        with connection.cursor() as cursor:
            # 查询所有图片数据
            sql = "SELECT id, photoName, image FROM photos"
            cursor.execute(sql)
            results = cursor.fetchall()

        # 解码图片的 Base64 数据
        for result in results:
            result['image'] = f"data:image/jpeg;base64,{result['image']}"

        return jsonify({"status": "success", "data": results})

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)})
    finally:
        if 'connection' in locals():
            connection.close()


if __name__ == '__main__':
    #app.run(debug=True, host='0.0.0.0', port=8088)
    app.run(debug=True)  
