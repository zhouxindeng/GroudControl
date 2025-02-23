import base64
import requests
import json

# Flask 服务器地址
BASE_URL = 'http://127.0.0.1:6666'  # 本地运行时使用这个地址

# 测试插入无人机数据的函数
def test_insert_drone_data():
    url = f"{BASE_URL}/insertDroneAttitude"
    # 构造要发送的测试数据
    payload = {
        "longitude": 118.789,
        "latitude": 32.042,
        "velocityX": 5.6,
        "velocityY": 4.5,
        "velocityZ": -0.2,
        "altitude": 100.3,
        "pitch": 10.2,
        "roll": 2.5,
        "yaw": 90.0
    }
    
    headers = {
        'Content-Type': 'application/json'
    }

    # 发送 POST 请求
    response = requests.post(url, data=json.dumps(payload), headers=headers)
    
    # 打印服务器返回的响应内容
    print("Insert Drone Data Response:")
    print(response.json())

# 测试获取无人机数据的函数
def test_get_drone_data():
    url = f"{BASE_URL}/getDroneAttitude"
    
    # 发送 GET 请求
    response = requests.get(url)
    
    # 打印服务器返回的响应内容
    print("Get Drone Data Response:")
    print(response.json())
    
def test_insert_photo():
    url = f"{BASE_URL}/insertPhotos"
    photo_name = "1.jpg"
    photo_path = "./1.jpg"  # 确保 1.jpg 与本文件在同一文件夹中

    # 打开图片文件
    with open(photo_path, 'rb') as photo_file:
        # 构造表单数据
        files = {
            'photoName': (None, photo_name),  # 图片名称字段
            'photo': (photo_name, photo_file, 'image/jpeg')  # 文件字段
        }
        
        # 发送 POST 请求
        response = requests.post(url, files=files)
        
    # 打印服务器返回的响应内容
    print("Insert Photo Response:")
    print(response.json())

# 测试获取图片数据的函数
def test_get_photos():
    url = f"{BASE_URL}/getPhotos"
    
    # 发送 GET 请求
    response = requests.get(url)
    
    # 打印服务器返回的响应内容
    print("Get Photos Response:")
    photos_data = response.json()
    print(photos_data)

    # 保存图片以验证内容
    if photos_data["status"] == "success":
        for idx, photo in enumerate(photos_data["data"]):
            image_data = photo["image"].split(",")[1]  # 去掉 `data:image/jpeg;base64,`
            with open(f"photo_{idx + 1}.jpg", "wb") as img_file:
                img_file.write(base64.b64decode(image_data))
        print("Decoded images saved as photo_1.jpg, photo_2.jpg, ...")

if __name__ == '__main__':
    # 执行插入数据测试
    test_insert_drone_data()

    # 执行获取数据测试
    test_get_drone_data()
    
      # 测试插入图片
    test_insert_photo()

    # 测试获取图片
    test_get_photos()
