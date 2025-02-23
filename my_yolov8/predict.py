import os
from ultralytics import YOLO
from PIL import Image
import shutil

def predict_and_sort_images(model_path, source_folder, special_folder, normal_folder):
    # 加载已训练好的模型
    model = YOLO(model_path)
    
    # 检查目标文件夹是否存在，不存在则创建
    os.makedirs(special_folder, exist_ok=True)
    os.makedirs(normal_folder, exist_ok=True)
    
    # 遍历 photo 文件夹下的所有图片
    for image_name in os.listdir(source_folder):
        # 获取图片完整路径
        image_path = os.path.join(source_folder, image_name)
        
        # 打开图片并进行预测
        img = Image.open(image_path)
        results = model.predict(source=img, show=False, save=True)  # save=True 以保存标注后的图片
        
        # 预测后的图片会保存在当前目录的 `runs/predict` 文件夹中
        predicted_image_path = f"runs/detect/predict/{image_name}"  # 假设保存的文件名与原文件名相同

        # 判断是否有检测到标签
        if results[0].boxes:  # 如果存在检测框，则有检测到标签
            # 将保存的预测图片移动到 special 文件夹
            shutil.move(predicted_image_path, os.path.join(special_folder, image_name))
            print(f"{image_name} 的预测结果已保存到 special 文件夹")
             # 删除原始的未标注图片
            os.remove(image_path)
        else:
            # 将原始图片移动到 normal 文件夹
            shutil.move(image_path, os.path.join(normal_folder, image_name))
            print(f"{image_name} 移动到 normal 文件夹")


# 定义路径
model_path = "F:/GroudControl/my_yolov8/runs/detect/train_zdp/weights/best.pt"
source_folder = "F:/GroudControl/my_yolov8/zdp/photos/"
special_folder = "F:/GroudControl/my_yolov8/zdp/special/"
normal_folder = "F:/GroudControl/my_yolov8/zdp/normal/"

# 运行预测和分类函数
predict_and_sort_images(model_path, source_folder, special_folder, normal_folder)
